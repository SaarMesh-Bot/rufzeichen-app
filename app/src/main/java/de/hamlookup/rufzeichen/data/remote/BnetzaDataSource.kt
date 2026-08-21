package de.hamlookup.rufzeichen.data.remote

import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.data.model.DataSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * Queries the official Bundesnetzagentur amateur radio call sign search
 * (an ASP.NET WebForms page) and scrapes the result table.
 *
 * The result table has the columns:
 *   Rufzeichen | K | p. Rufz. | Inhaber | Betriebsort
 * where "K" is the licence class. The "Inhaber" cell is "Name; Anschrift" in a
 * single field, so we split off the name and keep the full address separately.
 * Parsing keys off the header labels; all failures return an empty result.
 */
class BnetzaDataSource(
    private val client: OkHttpClient = defaultClient()
) {
    companion object {
        const val BASE_URL = "https://ans.bundesnetzagentur.de/Amateurfunk/Rufzeichen.aspx"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        private val CALL_REGEX = Regex("^[A-Z0-9]{1,3}[0-9][A-Z0-9]{0,4}$")
        private val CITY_REGEX = Regex("\\b\\d{4,5}\\s+([A-Za-zÄÖÜäöüß .\\-]+)$")
    }

    suspend fun search(query: String): List<Callsign> = withContext(Dispatchers.IO) {
        try {
            val page = get(BASE_URL) ?: return@withContext emptyList()
            val doc = Jsoup.parse(page, BASE_URL)
            val form = doc.selectFirst("form") ?: return@withContext emptyList()

            val formBuilder = FormBody.Builder()
            form.select("input[type=hidden]").forEach { input ->
                val name = input.attr("name")
                if (name.isNotEmpty()) formBuilder.add(name, input.attr("value"))
            }
            val textInput = form.select("input[type=text]").firstOrNull()
                ?: form.select("input").firstOrNull { it.attr("name").contains("text", true) }
            val fieldName = textInput?.attr("name") ?: return@withContext emptyList()
            formBuilder.add(fieldName, query)
            form.select("input[type=submit]").firstOrNull()?.let { submit ->
                val n = submit.attr("name")
                if (n.isNotEmpty()) formBuilder.add(n, submit.attr("value").ifEmpty { "Suche starten" })
            }

            val resultHtml = post(BASE_URL, formBuilder.build()) ?: return@withContext emptyList()
            parseResults(Jsoup.parse(resultHtml, BASE_URL))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseResults(doc: Document): List<Callsign> {
        for (table in doc.select("table")) {
            val rows = table.select("tr")
            if (rows.isEmpty()) continue

            val headerRow = rows.firstOrNull { row ->
                val texts = row.select("th, td").map { clean(it.text()) }
                texts.any { it.equals("Rufzeichen", true) } &&
                    texts.any {
                        it.equals("K", true) || it.contains("Inhaber", true) ||
                            it.contains("Betriebsort", true)
                    }
            } ?: continue

            val headers = headerRow.select("th, td").map { clean(it.text()) }
            val cCall = headers.indexOfFirst { it.equals("Rufzeichen", true) }
            val cClass = headers.indexOfFirst { it.equals("K", true) }
            val cPrev = headers.indexOfFirst { it.contains("p. Rufz", true) || it.contains("pers", true) }
            val cHolder = headers.indexOfFirst { it.contains("Inhaber", true) }
            val cQth = headers.indexOfFirst {
                it.contains("Betriebsort", true) || it.contains("Standort", true) || it == "Ort"
            }

            val headerIdx = rows.indexOf(headerRow)
            val results = mutableListOf<Callsign>()
            for (i in (headerIdx + 1) until rows.size) {
                val cells = rows[i].select("td").map { clean(it.text()) }
                if (cells.isEmpty()) continue
                val call = cells.getOrNull(cCall)?.uppercase()?.takeIf { it.isNotBlank() } ?: continue
                if (!CALL_REGEX.matches(call)) continue

                // "Inhaber" = "Name; Anschrift" -> split name from address.
                val holderRaw = cells.getOrNull(cHolder)?.takeIf { it.isNotBlank() }
                val name = holderRaw?.substringBefore(";")?.trim()?.takeIf { it.isNotBlank() }
                val betriebsort = cells.getOrNull(cQth)?.takeIf { it.isNotBlank() }
                val city = betriebsort?.let { cityFrom(it) }

                val extra = LinkedHashMap<String, String>()
                cells.getOrNull(cPrev)?.takeIf { it.isNotBlank() }?.let { extra["Pers. Rufzeichen"] = it }
                if (betriebsort != null) extra["Adresse"] = betriebsort

                val classCode = cells.getOrNull(cClass)?.takeIf { it.isNotBlank() }

                results += Callsign(
                    callsign = call,
                    holderName = name,
                    licenceClass = classCode?.let { classLabel(it) },
                    qth = city ?: betriebsort,
                    country = "Deutschland",
                    countryCode = "DE",
                    extra = extra,
                    sources = setOf(DataSourceType.BNETZA),
                    sourceName = "BNetzA",
                    official = true
                )
            }
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    private fun cityFrom(address: String): String? =
        CITY_REGEX.find(address.trim())?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun classLabel(code: String): String = when (code.trim().uppercase()) {
        "A" -> "Klasse A"
        "E" -> "Klasse E (Einsteiger)"
        "N" -> "Klasse N (Einsteiger)"
        else -> code.trim()
    }

    private fun clean(s: String): String =
        buildString { s.forEach { append(if (it.isWhitespace()) ' ' else it) } }
            .replace(Regex(" +"), " ").trim()

    private fun get(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) resp.body?.string() else null
        }
    }

    private fun post(url: String, body: okhttp3.RequestBody): String? {
        val req = Request.Builder().url(url)
            .header("User-Agent", USER_AGENT).header("Referer", url).post(body).build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) resp.body?.string() else null
        }
    }
}

private const val USER_AGENT =
    "Mozilla/5.0 (Android) RufzeichenApp/1.1 (Amateurfunk Rufzeichensuche)"
