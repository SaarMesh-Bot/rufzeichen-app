package de.hamlookup.rufzeichen.data.analyzer

import de.hamlookup.rufzeichen.data.model.CallsignAnalysis

/**
 * Offline call sign analysis. Splits a call sign into prefix / number / suffix,
 * derives the ITU country from the prefix, and — for German call signs — makes
 * a best-effort guess about the licence class from the call structure.
 *
 * This works entirely without network access and is used as the always-available
 * base layer of every lookup.
 */
object CallsignAnalyzer {

    /** A subset of ITU prefix ranges. Enough to cover the common cases. */
    private data class PrefixRange(val prefix: String, val country: String, val code: String)

    // Ordered longest-first at lookup time. Common allocations only.
    private val prefixTable: List<PrefixRange> = listOf(
        PrefixRange("DA", "Deutschland", "DE"),
        PrefixRange("DB", "Deutschland", "DE"),
        PrefixRange("DC", "Deutschland", "DE"),
        PrefixRange("DD", "Deutschland", "DE"),
        PrefixRange("DF", "Deutschland", "DE"),
        PrefixRange("DG", "Deutschland", "DE"),
        PrefixRange("DH", "Deutschland", "DE"),
        PrefixRange("DJ", "Deutschland", "DE"),
        PrefixRange("DK", "Deutschland", "DE"),
        PrefixRange("DL", "Deutschland", "DE"),
        PrefixRange("DM", "Deutschland", "DE"),
        PrefixRange("DN", "Deutschland", "DE"),
        PrefixRange("DO", "Deutschland", "DE"),
        PrefixRange("DP", "Deutschland", "DE"),
        PrefixRange("DQ", "Deutschland", "DE"),
        PrefixRange("DR", "Deutschland", "DE"),
        PrefixRange("OE", "Österreich", "AT"),
        PrefixRange("HB9", "Schweiz", "CH"),
        PrefixRange("HB0", "Liechtenstein", "LI"),
        PrefixRange("PA", "Niederlande", "NL"),
        PrefixRange("PB", "Niederlande", "NL"),
        PrefixRange("PD", "Niederlande", "NL"),
        PrefixRange("PE", "Niederlande", "NL"),
        PrefixRange("ON", "Belgien", "BE"),
        PrefixRange("LX", "Luxemburg", "LU"),
        PrefixRange("F", "Frankreich", "FR"),
        PrefixRange("G", "Großbritannien", "GB"),
        PrefixRange("M", "Großbritannien", "GB"),
        PrefixRange("2E", "Großbritannien", "GB"),
        PrefixRange("EI", "Irland", "IE"),
        PrefixRange("EA", "Spanien", "ES"),
        PrefixRange("CT", "Portugal", "PT"),
        PrefixRange("I", "Italien", "IT"),
        PrefixRange("SP", "Polen", "PL"),
        PrefixRange("OK", "Tschechien", "CZ"),
        PrefixRange("OM", "Slowakei", "SK"),
        PrefixRange("HA", "Ungarn", "HU"),
        PrefixRange("HG", "Ungarn", "HU"),
        PrefixRange("OZ", "Dänemark", "DK"),
        PrefixRange("SM", "Schweden", "SE"),
        PrefixRange("LA", "Norwegen", "NO"),
        PrefixRange("OH", "Finnland", "FI"),
        PrefixRange("SV", "Griechenland", "GR"),
        PrefixRange("TA", "Türkei", "TR"),
        PrefixRange("UA", "Russland", "RU"),
        PrefixRange("UR", "Ukraine", "UA"),
        PrefixRange("K", "USA", "US"),
        PrefixRange("N", "USA", "US"),
        PrefixRange("W", "USA", "US"),
        PrefixRange("AA", "USA", "US"),
        PrefixRange("VE", "Kanada", "CA"),
        PrefixRange("VA", "Kanada", "CA"),
        PrefixRange("JA", "Japan", "JP"),
        PrefixRange("VK", "Australien", "AU"),
        PrefixRange("ZL", "Neuseeland", "NZ"),
        PrefixRange("PY", "Brasilien", "BR"),
        PrefixRange("LU", "Argentinien", "AR"),
        PrefixRange("ZS", "Südafrika", "ZA")
    )

    /**
     * German licence class heuristic based on the call sign prefix block.
     * Note: this is an approximation. The authoritative class comes from the
     * BNetzA data source; this only fills the gap when offline.
     */
    private fun germanClassHint(prefix: String): String? = when (prefix.uppercase()) {
        "DO" -> "Klasse N (Einsteiger)"
        "DN" -> "Ausbildungsrufzeichen"
        "DL", "DK", "DJ", "DH", "DG", "DF", "DD", "DC", "DB", "DA" -> "Klasse A (Standard)"
        "DP" -> "Sonder-/Klubstation (Klasse A)"
        "DM", "DR", "DQ" -> "Klub- oder Sonderrufzeichen"
        else -> null
    }

    fun analyze(rawInput: String): CallsignAnalysis {
        val normalized = rawInput.trim().uppercase().replace(Regex("[^A-Z0-9/*]"), "")
        // Work on the base call (strip portable indicators like /P, /M, DL/…).
        val base = baseCall(normalized)

        val match = Regex("^([A-Z0-9]*?[A-Z])([0-9])([A-Z0-9]*)$").find(base)
        val prefix: String
        val number: String
        val suffix: String
        if (match != null) {
            prefix = match.groupValues[1]
            number = match.groupValues[2]
            suffix = match.groupValues[3]
        } else {
            prefix = base.takeWhile { it.isLetter() }
            number = base.dropWhile { it.isLetter() }.takeWhile { it.isDigit() }
            suffix = base.dropWhile { it.isLetter() }.dropWhile { it.isDigit() }
        }

        val range = lookupPrefix(base)
        val isGerman = range?.code == "DE"
        val germanClass = if (isGerman) germanClassHint(prefix) else null

        val notes = buildList {
            if (normalized.contains('*')) add("Enthält Platzhalter '*' – geeignet für die BNetzA-Suche.")
            if (normalized.contains('/')) add("Portabel-/Zusatzkennung erkannt: $normalized")
            if (isGerman && prefix.length == 2 && prefix[1] in 'A'..'R') {
                add("Deutscher Rufzeichenblock '$prefix'.")
            }
            if (range == null) add("Präfix nicht in der Offline-Tabelle – Land unbekannt.")
        }

        return CallsignAnalysis(
            normalized = normalized,
            prefix = prefix,
            number = number,
            suffix = suffix,
            country = range?.country ?: "Unbekannt",
            countryCode = range?.code ?: "??",
            isGerman = isGerman,
            germanClass = germanClass,
            notes = notes
        )
    }

    private fun baseCall(call: String): String {
        if (!call.contains('/')) return call
        val parts = call.split('/').filter { it.isNotEmpty() }
        // The base call is usually the longest alphanumeric segment.
        return parts.maxByOrNull { it.length } ?: call
    }

    private fun lookupPrefix(base: String): PrefixRange? {
        // Match the longest defined prefix first.
        return prefixTable
            .filter { base.startsWith(it.prefix) }
            .maxByOrNull { it.prefix.length }
    }
}
