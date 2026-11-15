package de.hamlookup.rufzeichen.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One HF band group with N0NBH's calculated day/night condition ("Good"/"Fair"/"Poor"). */
data class BandCondition(val name: String, val day: String?, val night: String?)

/** A VHF phenomenon (aurora, E-skip) with its status ("Band Closed", "High MUF" …). */
data class VhfCondition(val name: String, val location: String, val status: String?)

data class KpPoint(val timeUtc: String, val kp: Double)

data class SolarIndices(
    val sfi: Double?,
    val sunspots: Double?,
    val aIndex: Double?,
    val kIndex: Double?,
    val xray: String?,
    val solarWind: Double?,
    val bz: Double?,
    val geomagField: String?,
    val signalNoise: String?
)

data class NoaaScales(val r: Int?, val s: Int?, val g: Int?)

data class PropagationSource(val name: String, val url: String)

/** Space weather summary as delivered by the backend (`/propagation`). */
data class Propagation(
    val updated: String?,
    val fetchedAt: String?,
    val stale: Boolean,
    val solar: SolarIndices,
    val bands: List<BandCondition>,
    val vhf: List<VhfCondition>,
    val kpHistory: List<KpPoint>,
    val scales: NoaaScales?,
    val sources: List<PropagationSource>
)

/**
 * Fetches the "Funkwetter" summary from our own backend. The backend caches the
 * upstream data (N0NBH / NOAA SWPC) for 30 minutes, so the app never talks to
 * those servers directly and no user data is sent anywhere.
 */
class PropagationDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun load(baseUrl: String): kotlin.Result<Propagation> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/propagation")
                .header("User-Agent", "RufzeichenApp-Android")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                require(!body.isNullOrBlank()) { "empty response" }
                val json = JSONObject(body)
                require(json.optString("status") == "ok") { json.optString("message", "unavailable") }
                parse(json)
            }
        }
    }

    companion object {
        private fun JSONObject.dbl(key: String): Double? =
            if (isNull(key) || !has(key)) null else optDouble(key).takeIf { !it.isNaN() }

        private fun JSONObject.str(key: String): String? =
            if (isNull(key) || !has(key)) null else optString(key).ifBlank { null }

        private fun JSONObject.int(key: String): Int? =
            if (isNull(key) || !has(key)) null else optInt(key)

        private inline fun <T> JSONArray?.mapObjects(f: (JSONObject) -> T?): List<T> {
            if (this == null) return emptyList()
            val out = ArrayList<T>(length())
            for (i in 0 until length()) optJSONObject(i)?.let { o -> f(o)?.let(out::add) }
            return out
        }

        fun parse(json: JSONObject): Propagation {
            val s = json.optJSONObject("solar") ?: JSONObject()
            return Propagation(
                updated = json.str("updated"),
                fetchedAt = json.str("fetched_at"),
                stale = json.optBoolean("stale", false),
                solar = SolarIndices(
                    sfi = s.dbl("sfi"),
                    sunspots = s.dbl("sunspots"),
                    aIndex = s.dbl("a_index"),
                    kIndex = s.dbl("k_index"),
                    xray = s.str("xray"),
                    solarWind = s.dbl("solar_wind"),
                    bz = s.dbl("bz"),
                    geomagField = s.str("geomag_field"),
                    signalNoise = s.str("signal_noise")
                ),
                bands = json.optJSONArray("bands").mapObjects { o ->
                    o.str("name")?.let { BandCondition(it, o.str("day"), o.str("night")) }
                },
                vhf = json.optJSONArray("vhf").mapObjects { o ->
                    o.str("name")?.let { VhfCondition(it, o.str("location") ?: "", o.str("status")) }
                },
                kpHistory = json.optJSONArray("kp_history").mapObjects { o ->
                    val t = o.str("time")
                    val kp = o.dbl("kp")
                    if (t != null && kp != null) KpPoint(t, kp) else null
                },
                scales = json.optJSONObject("noaa_scales")?.let {
                    NoaaScales(it.int("r"), it.int("s"), it.int("g"))
                },
                sources = json.optJSONArray("sources").mapObjects { o ->
                    val n = o.str("name"); val u = o.str("url")
                    if (n != null && u != null) PropagationSource(n, u) else null
                }
            )
        }
    }
}
