package de.hamlookup.rufzeichen.data.remote

import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.data.model.DataSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Client for the international call-sign backend (hamapi). The backend performs
 * country detection, official-source lookup (BNetzA, Callook/FCC, …) and the
 * optional community fallback (HamQTH/QRZ/QRZCQ), and returns a normalised
 * envelope. This class only maps that envelope onto the app's [Callsign] model.
 *
 * The heavy provider logic lives server-side on purpose (secrets, caching,
 * shared updates). This keeps the client thin and offline-safe.
 */
class BackendDataSource(
    private val client: OkHttpClient = defaultClient()
) {
    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Result envelope mirroring the backend contract. */
    data class Result(
        val status: String,          // ok | not_found | no_source | unverifiable | invalid | error
        val callsign: Callsign?,     // present when status == ok
        val message: String? = null
    )

    /**
     * Looks up a single call sign (no wildcards) via the backend. Network or
     * parse problems are reported as status "unverifiable" so the caller can
     * fall back to on-device sources rather than treating it as "not found".
     */
    suspend fun lookup(baseUrl: String, query: String): Result = withContext(Dispatchers.IO) {
        try {
            val call = URLEncoder.encode(query.trim().uppercase(), "UTF-8")
            val url = "${baseUrl.trimEnd('/')}/callsign/$call?community=1"
            val req = Request.Builder().url(url)
                .header("User-Agent", "RufzeichenApp-Android")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (body.isNullOrBlank()) {
                    return@withContext Result("unverifiable", null,
                        "Server nicht erreichbar.")
                }
                val json = JSONObject(body)
                val status = json.optString("status", "error")
                if (status == "ok") {
                    Result("ok", mapResult(json), json.optStringOrNull("message"))
                } else {
                    Result(status, null, json.optStringOrNull("message"))
                }
            }
        } catch (e: Exception) {
            Result("unverifiable", null, "Server nicht erreichbar.")
        }
    }

    private fun mapResult(env: JSONObject): Callsign {
        val r = env.getJSONObject("result")
        val extraObj = r.optJSONObject("extra")
        val extra = LinkedHashMap<String, String>()
        if (extraObj != null) {
            for (k in extraObj.keys()) {
                val v = extraObj.optString(k)
                if (v.isNotBlank() && v != "null") {
                    val label = when (k) {
                        "address" -> "Adresse"
                        "grantDate" -> "Erteilt"
                        "expiryDate" -> "Gültig bis"
                        "type" -> "Typ"
                        else -> k
                    }
                    extra[label] = v
                }
            }
        }
        val city = r.optStringOrNull("city")
        val region = r.optStringOrNull("region")
        val qth = listOfNotNull(city, region).joinToString(", ").ifBlank { null }
        val official = env.optBoolean("official_checked", false) ||
            r.optStringOrNull("source_type") == "official"

        return Callsign(
            callsign = r.optString("callsign", env.optString("callsign")),
            holderName = r.optStringOrNull("name"),
            licenceClass = r.optStringOrNull("license_class"),
            qth = qth,
            country = r.optStringOrNull("country") ?: env.optStringOrNull("country"),
            countryCode = r.optStringOrNull("country_code") ?: env.optStringOrNull("country_code"),
            licenseStatus = r.optStringOrNull("license_status"),
            locator = r.optStringOrNull("locator"),
            latitude = r.optDoubleOrNull("latitude"),
            longitude = r.optDoubleOrNull("longitude"),
            sourceName = r.optStringOrNull("source") ?: env.optStringOrNull("source"),
            official = official,
            extra = extra,
            sources = setOf(DataSourceType.BACKEND)
        )
    }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val v = optString(key)
    return v.ifBlank { null }.takeIf { it != "null" }
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val d = optDouble(key, Double.NaN)
    return if (d.isNaN()) null else d
}
