package de.hamlookup.rufzeichen.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import de.hamlookup.rufzeichen.data.analyzer.CallsignAnalyzer
import de.hamlookup.rufzeichen.data.local.CachedCallsignEntity
import de.hamlookup.rufzeichen.data.local.CallsignDao
import de.hamlookup.rufzeichen.data.local.FavoriteEntity
import de.hamlookup.rufzeichen.data.local.HistoryEntity
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.data.model.DataSourceType
import de.hamlookup.rufzeichen.data.remote.BackendDataSource
import de.hamlookup.rufzeichen.data.remote.BnetzaDataSource
import de.hamlookup.rufzeichen.data.remote.HamQthDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Result of a combined search across all sources. */
data class SearchOutcome(
    val results: List<Callsign>,
    val usedSources: Set<DataSourceType>,
    val wasOffline: Boolean,
    val message: String? = null
)

class CallsignRepository(
    private val context: Context,
    private val dao: CallsignDao,
    private val settingsRepo: SettingsRepository,
    private val bnetza: BnetzaDataSource = BnetzaDataSource(),
    private val hamQth: HamQthDataSource = HamQthDataSource(),
    private val backend: BackendDataSource = BackendDataSource()
) {

    val favorites: Flow<List<Callsign>> =
        dao.observeFavorites().map { list -> list.map { it.toCallsign() } }

    val history: Flow<List<HistoryEntity>> = dao.observeHistory()

    fun isFavorite(callsign: String): Flow<Boolean> = dao.isFavorite(callsign.uppercase())

    suspend fun toggleFavorite(callsign: Callsign, makeFavorite: Boolean) {
        if (makeFavorite) {
            dao.addFavorite(
                FavoriteEntity(
                    callsign = callsign.callsign.uppercase(),
                    holderName = callsign.holderName,
                    licenceClass = callsign.licenceClass,
                    qth = callsign.qth,
                    country = callsign.country ?: callsign.analysis?.country
                )
            )
        } else {
            dao.removeFavorite(callsign.callsign.uppercase())
        }
    }

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun search(rawQuery: String): SearchOutcome {
        val query = rawQuery.trim().uppercase()
        if (query.isEmpty()) return SearchOutcome(emptyList(), emptySet(), false)

        val analysis = CallsignAnalyzer.analyze(query)
        val settings = settingsRepo.settings.first()
        val online = isOnline()
        val wildcard = query.contains('*')
        val used = mutableSetOf<DataSourceType>()
        val merged = LinkedHashMap<String, Callsign>()
        var backendMessage: String? = null
        var handledByBackend = false

        if (online) {
            // 1) International backend first (single call signs only; the
            //    wildcard search is a BNetzA-specific feature handled on-device).
            if (settings.useBackend && !wildcard) {
                val res = backend.lookup(settings.backendUrl, query)
                when (res.status) {
                    "ok" -> {
                        res.callsign?.let { merge(merged, it); used += DataSourceType.BACKEND }
                        handledByBackend = true
                    }
                    "not_found", "no_source", "invalid" -> {
                        // A definite answer from the backend — do not double-query
                        // on-device for the same call sign.
                        handledByBackend = true
                        backendMessage = res.message
                    }
                    else -> {
                        // unverifiable / error -> fall through to on-device sources.
                        handledByBackend = false
                    }
                }
            }

            // 2) On-device fallback (also the path for wildcard queries and when
            //    the backend is disabled/unreachable). Existing behaviour.
            if (!handledByBackend) {
                if (settings.useBnetza) {
                    val list = bnetza.search(query)
                    if (list.isNotEmpty()) used += DataSourceType.BNETZA
                    list.forEach { merge(merged, it) }
                }
                if (settings.useHamQth) {
                    val list = hamQth.search(query, settings.hamQthUser, settings.hamQthPass)
                    if (list.isNotEmpty()) used += DataSourceType.HAMQTH
                    list.forEach { merge(merged, it) }
                }
            }

            if (merged.isNotEmpty()) {
                cacheResults(merged.values.toList())
            }
        }

        // Offline / fallback: search local cache.
        if (merged.isEmpty()) {
            val pattern = query.replace('*', '_') + if (!query.endsWith("*")) "%" else ""
            dao.searchCache(pattern).forEach { merge(merged, it.toCallsign()) }
            if (merged.isNotEmpty()) used += DataSourceType.OFFLINE
        }

        // Always attach offline analysis to every result; when nothing was found
        // we still return the offline analysis so the user sees the country etc.
        val results = if (merged.isEmpty()) {
            used += DataSourceType.OFFLINE
            listOf(
                Callsign(
                    callsign = analysis.normalized.ifEmpty { query },
                    country = analysis.country,
                    licenceClass = analysis.germanClass,
                    sources = setOf(DataSourceType.OFFLINE),
                    analysis = analysis
                )
            )
        } else {
            merged.values.map { c ->
                c.copy(analysis = CallsignAnalyzer.analyze(c.callsign))
            }
        }

        dao.addHistory(HistoryEntity(query = query, resultCount = results.size))

        val message = when {
            !online -> "Offline – Ergebnisse aus lokalem Cache und Analyse."
            merged.isEmpty() && backendMessage != null -> backendMessage
            online && used.isEmpty() -> "Keine Online-Treffer – nur Offline-Analyse."
            else -> null
        }
        return SearchOutcome(results, used, !online, message)
    }

    suspend fun lookupDetail(callsign: String): Callsign {
        val analysis = CallsignAnalyzer.analyze(callsign)
        val cached = dao.getCached(callsign.uppercase())?.toCallsign()
        return (cached ?: Callsign(callsign = callsign.uppercase(), country = analysis.country))
            .copy(analysis = analysis)
    }

    // ---- helpers ----

    private fun merge(map: LinkedHashMap<String, Callsign>, c: Callsign) {
        val key = c.callsign.uppercase()
        val existing = map[key]
        map[key] = if (existing == null) c else existing.copy(
            holderName = existing.holderName ?: c.holderName,
            licenceClass = existing.licenceClass ?: c.licenceClass,
            qth = existing.qth ?: c.qth,
            country = existing.country ?: c.country,
            countryCode = existing.countryCode ?: c.countryCode,
            licenseStatus = existing.licenseStatus ?: c.licenseStatus,
            locator = existing.locator ?: c.locator,
            latitude = existing.latitude ?: c.latitude,
            longitude = existing.longitude ?: c.longitude,
            sourceName = existing.sourceName ?: c.sourceName,
            official = existing.official ?: c.official,
            extra = existing.extra + c.extra,
            sources = existing.sources + c.sources
        )
    }

    private suspend fun cacheResults(list: List<Callsign>) {
        val entities = list.map { c ->
            // Privacy: do not persist full street addresses in the local cache.
            val cacheExtra = c.extra.filterKeys { !it.contains("Adresse", true) && !it.contains("address", true) }
            CachedCallsignEntity(
                callsign = c.callsign.uppercase(),
                holderName = c.holderName,
                licenceClass = c.licenceClass,
                qth = c.qth,
                country = c.country,
                extraJson = JSONObject(cacheExtra as Map<*, *>).toString(),
                sourcesCsv = c.sources.joinToString(",") { it.name }
            )
        }
        dao.cache(entities)
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

// ---- mapping extensions ----

private fun FavoriteEntity.toCallsign(): Callsign = Callsign(
    callsign = callsign,
    holderName = holderName,
    licenceClass = licenceClass,
    qth = qth,
    country = country,
    sources = emptySet()
)

private fun CachedCallsignEntity.toCallsign(): Callsign {
    val extra = LinkedHashMap<String, String>()
    runCatching {
        val obj = JSONObject(extraJson)
        obj.keys().forEach { k -> extra[k] = obj.optString(k) }
    }
    val sources = sourcesCsv.split(",").mapNotNull { s ->
        runCatching { DataSourceType.valueOf(s) }.getOrNull()
    }.toSet()
    return Callsign(
        callsign = callsign,
        holderName = holderName,
        licenceClass = licenceClass,
        qth = qth,
        country = country,
        extra = extra,
        sources = sources
    )
}
