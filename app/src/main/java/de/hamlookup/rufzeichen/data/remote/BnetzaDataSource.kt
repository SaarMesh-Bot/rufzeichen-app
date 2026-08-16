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
 *   3. parsing the returned HTML table.
 *
 * Because it depends on the page's markup, it can break if the BNetzA changes
 * its website. The parsing is intentionally defensive (it discovers field and
 * column names dynamically) to survive small changes, and all failures are
 * reported as an empty result rather than crashing the app.
 */
class BnetzaDataSource(
    private val client: OkHttpClient = defaultClient()
) {
    companion object {
        const val BASE_URL = "https://ans.bundesnetzagentur.de/Amateurfunk/Rufzeichen.aspx"
        private val HIDDEN_FIELDS = listOf(
            "__VIEWSTATE", "__VIEWSTATEGENERATOR", "__EVENTVALIDATION",
            "__EVENTTARGET", "__EVENTARGUMENT", "__VIEWSTATEENCRYPTED"
        )

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
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

            // Find the search text field. Prefer a text input, else any input
            // whose name contains "ruf" (Rufzeichen).
            val textInput = form.select("input[type=text]").firstOrNull()
                ?: form.select("input")
                    .firstOrNull { it.attr("name").contains("ruf", ignoreCase = true) }
            val fieldName = textInput?.attr("name") ?: return@withContext emptyList()
            formBuilder.add(fieldName, query)

            // Include the submit button if present (WebForms often needs it).
            form.select("input[type=submit]").firstOrNull()?.let { submit ->
                val n = submit.attr("name")
                if (n.isNotEmpty()) formBuilder.add(n, submit.attr("value").ifEmpty { "Suchen" })
            }

            val resultHtml = post(BASE_URL, formBuilder.build().let { it }) ?: return@withContext emptyList()
            parseResults(Jsoup.parse(resultHtml, BASE_URL))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseResults(doc: Document): List<Callsign> {
        // Find the table that actually contains result rows. Heuristic: the
        // table with the most data rows that mentions a call-sign-like token.
        val tables = doc.select("table")
        val callRegex = Regex("^[A-Z0-9]{1,3}[0-9][A-Z0-9]{0,4}$")

        val best = tables.maxByOrNull { table ->
            table.select("tr").count { tr ->
                tr.select("td").any { callRegex.matches(it.text().trim().uppercase()) }
            }
        } ?: return emptyList()

        val rows = best.select("tr")
        if (rows.isEmpty()) return emptyList()

        // Determine header labels from the first row that uses <th>, else first row.
        val headerCells = rows.firstOrNull { it.select("th").isNotEmpty() }?.select("th")
            ?: rows.first().select("td")
        val headers = headerCells.map { it.text().trim() }

        val results = mutableListOf<Callsign>()
        rows.forEach { tr ->
            val cells = tr.select("td")
            if (cells.isEmpty()) return@forEach
            val values = cells.map { it.text().trim() }
            // Skip rows without a call-sign-like token.
            val callIdx = values.indexOfFirst { callRegex.matches(it.uppercase()) }
            if (callIdx < 0) return@forEach

            val map = LinkedHashMap<String, String>()
            values.forEachIndexed { i, v ->
                val key = headers.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: "Feld ${i + 1}"
                if (v.isNotEmpty()) map[key] = v
            }

            results += Callsign(
                callsign = values[callIdx].uppercase(),
                holderName = map.entries.firstOrNull { it.key.contains("Name", true) || it.key.contains("Inhaber", true) }?.value,
                licenceClass = map.entries.firstOrNull { it.key.contains("Klasse", true) }?.value,
                qth = map.entries.firstOrNull { it.key.contains("Ort", true) || it.key.contains("QTH", true) || it.key.contains("Standort", true) }?.value,
                extra = map,
                sources = setOf(DataSourceType.BNETZA)
            )
        }
        return results
    }

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
    "RufzeichenApp/1.0 (Android; Amateurfunk Rufzeichensuche)"
