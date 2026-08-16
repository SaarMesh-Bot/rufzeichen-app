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
 * IMPORTANT: The BNetzA does not offer an official API. This class talks to
 * the public HTML search form at
 * https://ans.bundesnetzagentur.de/Amateurfunk/Rufzeichen.aspx by
 *   1. fetching the page to capture the hidden ViewState fields,
 *   2. posting the search term back with those fields, and
 *   3. parsing the returned HTML result table.
 *
 * The result table has the columns:
 *   Rufzeichen | K | p. Rufz. | Inhaber | Betriebsort
 * where "K" is the licence class (A / E / N). Parsing keys off these header
 * labels, so small layout changes are tolerated. All failures are reported as
 * an empty result rather than crashing the app.
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
    }

    /**
     * Searches for [query] (a call sign, '*' allowed as a single-char wildcard).
     * Returns the parsed results, or an empty list if the source is unreachable
     * or returns nothing.
     */
    suspend fun search(query: String): List<Callsign> = withContext(Dispatchers.IO) {
        try {
            val page = get(BASE_URL) ?: return@withContext emptyList()
            val doc = Jsoup.parse(page, BASE_URL)
            val form = doc.selectFirst("form") ?: return@withContext emptyList()

            // Collect all hidden inputs (ViewState etc.).
            val formBuilder = FormBody.Builder()
            form.select("input[type=hidden]").forEach { input ->
                val name = input.attr("name")
                if (name.isNotEmpty()) formBuilder.add(name, input.attr("value"))
            }

            // Find the search text field (name is "Text1", but discover it dynamically).
            val textInput = form.select("input[type=text]").firstOrNull()
                ?: form.select("input").firstOrNull { it.attr("name").contains("text", true) }
            val fieldName = textInput?.attr("name") ?: return@withContext emptyList()
            formBuilder.add(fieldName, query)

            // Include the submit button (name "Bt_Suche") if present.
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

            // Locate the header row: contains "Rufzeichen" plus one of the other columns.
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

                val extra = LinkedHashMap<String, String>()
                cells.getOrNull(cPrev)?.takeIf { it.isNotBlank() }?.let { extra["Pers. Rufzeichen"] = it }
                val classCode = cells.getOrNull(cClass)?.takeIf { it.isNotBlank() }

                results += Callsign(
                    callsign = call,
                    holderName = cells.getOrNull(cHolder)?.takeIf { it.isNotBlank() },
                    licenceClass = classCode?.let { classLabel(it) },
                    qth = cells.getOrNull(cQth)?.takeIf { it.isNotBlank() },
                    extra = extra,
                    sources = setOf(DataSourceType.BNETZA)
                )
            }
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    /** Maps the single-letter BNetzA class code to a readable label. */
    private fun classLabel(code: String): String = when (code.trim().uppercase()) {
        "A" -> "Klasse A"
        "E" -> "Klasse E (Einsteiger)"
        "N" -> "Klasse N (Einsteiger)"
        else -> code.trim()
    }

    /** Normalises all whitespace, including non-breaking spaces, from scraped cells. */
    private fun clean(s: String): String =
        buildString { s.forEach { append(if (it.isWhitespace()) ' ' else it) } }
            .replace(Regex(" +"), " ").trim()

    private fun get(url: String): String? {
        val req = Request.Builder().url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) resp.body?.string() else null
        }
    }

    private fun post(url: String, body: okhttp3.RequestBody): String? {
        val req = Request.Builder().url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", url)
            .post(body)
            .build()
        client.newCall(req).execute().use { resp ->
            return if (resp.isSuccessful) resp.body?.string() else null
        }
    }
}

private const val USER_AGENT =
    "Mozilla/5.0 (Android) RufzeichenApp/1.1 (Amateurfunk Rufzeichensuche)"
