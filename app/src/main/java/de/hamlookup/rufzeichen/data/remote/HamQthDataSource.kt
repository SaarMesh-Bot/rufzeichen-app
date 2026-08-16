package de.hamlookup.rufzeichen.data.remote

import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.data.model.DataSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Optional international fallback using the free HamQTH XML API
 * (https://www.hamqth.com/developers.php).
 *
 * Requires a (free) HamQTH account. Credentials are supplied by the user in
 * the app settings. When no credentials are configured, [search] returns an
 * empty list so the rest of the app keeps working.
 *
 * Flow:
 *   1. POST/GET login -> receive a session id (valid ~1h, cached in memory).
 *   2. GET lookup with the session id and call sign.
 */
class HamQthDataSource(
    private val client: OkHttpClient = defaultClient()
) {
    companion object {
        private const val ENDPOINT = "https://www.hamqth.com/xml.php"
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val sessionMutex = Mutex()
    @Volatile private var sessionId: String? = null

    suspend fun search(
        query: String,
        username: String?,
        password: String?
    ): List<Callsign> = withContext(Dispatchers.IO) {
        if (username.isNullOrBlank() || password.isNullOrBlank()) return@withContext emptyList()
        // HamQTH only supports exact call signs, not wildcards.
        if (query.contains('*')) return@withContext emptyList()

        try {
            var id = ensureSession(username, password) ?: return@withContext emptyList()
            var xml = lookup(id, query)
            // Session may have expired -> refresh once.
            if (xml == null || xml.contains("Session does not exist", true)) {
                sessionId = null
                id = ensureSession(username, password) ?: return@withContext emptyList()
                xml = lookup(id, query)
            }
            if (xml == null) return@withContext emptyList()
            parseLookup(xml, query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun ensureSession(user: String, pass: String): String? {
        sessionId?.let { return it }
        return sessionMutex.withLock {
            sessionId?.let { return it }
            val url = ENDPOINT.toHttpUrl().newBuilder()
                .addQueryParameter("u", user)
                .addQueryParameter("p", pass)
                .build()
            val xml = getString(url.toString()) ?: return null
            val id = extractTag(xml, "session_id")
            sessionId = id
            id
        }
    }

    private fun lookup(id: String, callsign: String): String? {
        val url = ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("id", id)
            .addQueryParameter("callsign", callsign)
            .addQueryParameter("prg", "RufzeichenApp")
            .build()
        return getString(url.toString())
    }

    private fun parseLookup(xml: String, query: String): List<Callsign> {
        if (extractTag(xml, "error") != null) return emptyList()
        val call = extractTag(xml, "callsign") ?: return emptyList()
        val extra = linkedMapOf<String, String>()
        listOf(
            "nick" to "Name",
            "qth" to "QTH",
            "country" to "Land",
            "adr_name" to "Inhaber",
            "adr_city" to "Stadt",
            "grid" to "Locator",
            "itu" to "ITU-Zone",
            "cq" to "CQ-Zone"
        ).forEach { (tag, label) ->
            extractTag(xml, tag)?.takeIf { it.isNotBlank() }?.let { extra[label] = it }
        }
        return listOf(
            Callsign(
                callsign = call.uppercase(),
                holderName = extractTag(xml, "adr_name") ?: extractTag(xml, "nick"),
                qth = extractTag(xml, "qth"),
                country = extractTag(xml, "country"),
                extra = extra,
                sources = setOf(DataSourceType.HAMQTH)
            )
        )
    }

    private fun extractTag(xml: String, tag: String): String? {
        val m = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL).find(xml)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun getString(url: String): String? {
        val req = Request.Builder().url(url)
            .header("User-Agent", "RufzeichenApp/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) resp.body?.string() else null
        }
    }
}
