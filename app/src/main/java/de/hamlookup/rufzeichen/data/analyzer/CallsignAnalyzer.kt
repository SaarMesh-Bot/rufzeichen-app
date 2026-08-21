package de.hamlookup.rufzeichen.data.analyzer

import de.hamlookup.rufzeichen.data.model.CallsignAnalysis

/**
 * Offline call sign analysis. Splits a call sign into prefix / number / suffix,
 * derives the ITU country from the prefix, and — for German call signs — the
 * licence class and purpose from the official "Rufzeichenplan" (BNetzA
 * Vfg. 15/2025, gültig ab 01.04.2025).
 *
 * Important: the German class depends on the *Rufzeichenreihe* = prefix + digit,
 * not on the two-letter prefix alone. E.g. within the DN block:
 *   DN1–DN6 = Ausbildungsrufzeichen (Klasse A), DN7–DN8 = Ausbildung (Klasse E),
 *   DN9 = Klasse N, DN0 = Klubstation (Klasse E).
 *
 * This works entirely without network access and is the always-available base
 * layer of every lookup. The authoritative class still comes from BNetzA online.
 */
object CallsignAnalyzer {

    private data class PrefixRange(val prefix: String, val country: String, val code: String)

    // Country resolution (longest-match). German blocks all map to DE.
    private val prefixTable: List<PrefixRange> = listOf(
        PrefixRange("DA", "Deutschland", "DE"), PrefixRange("DB", "Deutschland", "DE"),
        PrefixRange("DC", "Deutschland", "DE"), PrefixRange("DD", "Deutschland", "DE"),
        PrefixRange("DF", "Deutschland", "DE"), PrefixRange("DG", "Deutschland", "DE"),
        PrefixRange("DH", "Deutschland", "DE"), PrefixRange("DJ", "Deutschland", "DE"),
        PrefixRange("DK", "Deutschland", "DE"), PrefixRange("DL", "Deutschland", "DE"),
        PrefixRange("DM", "Deutschland", "DE"), PrefixRange("DN", "Deutschland", "DE"),
        PrefixRange("DO", "Deutschland", "DE"), PrefixRange("DP", "Deutschland", "DE"),
        PrefixRange("DQ", "Deutschland", "DE"), PrefixRange("DR", "Deutschland", "DE"),
        PrefixRange("OE", "Österreich", "AT"), PrefixRange("HB9", "Schweiz", "CH"),
        PrefixRange("HB0", "Liechtenstein", "LI"),
        PrefixRange("PA", "Niederlande", "NL"), PrefixRange("PB", "Niederlande", "NL"),
        PrefixRange("PD", "Niederlande", "NL"), PrefixRange("PE", "Niederlande", "NL"),
        PrefixRange("ON", "Belgien", "BE"), PrefixRange("LX", "Luxemburg", "LU"),
        PrefixRange("F", "Frankreich", "FR"), PrefixRange("G", "Großbritannien", "GB"),
        PrefixRange("M", "Großbritannien", "GB"), PrefixRange("2E", "Großbritannien", "GB"),
        PrefixRange("EI", "Irland", "IE"), PrefixRange("EA", "Spanien", "ES"),
        PrefixRange("CT", "Portugal", "PT"), PrefixRange("I", "Italien", "IT"),
        PrefixRange("SP", "Polen", "PL"), PrefixRange("OK", "Tschechien", "CZ"),
        PrefixRange("OM", "Slowakei", "SK"), PrefixRange("HA", "Ungarn", "HU"),
        PrefixRange("HG", "Ungarn", "HU"), PrefixRange("OZ", "Dänemark", "DK"),
        PrefixRange("SM", "Schweden", "SE"), PrefixRange("LA", "Norwegen", "NO"),
        PrefixRange("OH", "Finnland", "FI"), PrefixRange("SV", "Griechenland", "GR"),
        PrefixRange("TA", "Türkei", "TR"), PrefixRange("UA", "Russland", "RU"),
        PrefixRange("UR", "Ukraine", "UA"),
        PrefixRange("K", "USA", "US"), PrefixRange("N", "USA", "US"),
        PrefixRange("W", "USA", "US"), PrefixRange("AA", "USA", "US"),
        PrefixRange("VE", "Kanada", "CA"), PrefixRange("VA", "Kanada", "CA"),
        PrefixRange("JA", "Japan", "JP"), PrefixRange("VK", "Australien", "AU"),
        PrefixRange("ZL", "Neuseeland", "NZ"), PrefixRange("PY", "Brasilien", "BR"),
        PrefixRange("LU", "Argentinien", "AR"), PrefixRange("ZS", "Südafrika", "ZA")
    )

    /** German licence class (A/E/N) + purpose code per Rufzeichenreihe (prefix+digit). */
    private val germanTable: Map<String, Pair<Char, String>> = buildMap {
        fun put(prefix: String, digits: IntRange, cls: Char, purpose: String) {
            for (d in digits) put("$prefix$d", cls to purpose)
        }
        put("DA", 0..0, 'A', "KS"); put("DA", 1..2, 'A', "PZ"); put("DA", 4..4, 'E', "SZ")
        put("DA", 5..5, 'A', "SZ"); put("DA", 6..6, 'E', "PZ"); put("DA", 7..7, 'E', "KS")
        put("DA", 8..8, 'N', "KS")
        put("DB", 0..0, 'A', "RLFB"); put("DB", 1..9, 'A', "PZ")
        put("DC", 0..9, 'A', "PZ")
        put("DD", 0..9, 'A', "PZ")
        put("DF", 0..0, 'A', "KS"); put("DF", 1..9, 'A', "PZ")
        put("DG", 0..9, 'A', "PZ")
        put("DH", 0..9, 'A', "PZ")
        put("DJ", 0..9, 'A', "PZ")
        put("DK", 0..0, 'A', "KS"); put("DK", 1..9, 'A', "PZ")
        put("DL", 0..0, 'A', "KS"); put("DL", 1..9, 'A', "PZ")
        put("DM", 0..0, 'A', "RLFB"); put("DM", 1..9, 'A', "PZ")
        put("DN", 0..0, 'E', "KS"); put("DN", 1..6, 'A', "AB"); put("DN", 7..8, 'E', "AB")
        put("DN", 9..9, 'N', "PZ")
        put("DO", 0..0, 'E', "RLFB"); put("DO", 1..9, 'E', "PZ")
        put("DP", 0..1, 'A', "KS"); put("DP", 2..2, 'E', "KS"); put("DP", 3..7, 'A', "KS")
        put("DP", 8..8, 'N', "KS"); put("DP", 9..9, 'A', "KS")
        put("DQ", 0..9, 'A', "KS")
        put("DR", 0..0, 'A', "KS"); put("DR", 1..1, 'A', "KSB"); put("DR", 2..2, 'E', "KSB")
        put("DR", 3..3, 'N', "KSB"); put("DR", 4..4, 'A', "KSO"); put("DR", 5..5, 'E', "KSO")
        put("DR", 6..6, 'N', "KSO"); put("DR", 7..9, 'A', "KS")
    }

    private val purposeLabel = mapOf(
        "PZ" to "personengebundenes Rufzeichen",
        "KS" to "Klubstation",
        "AB" to "Ausbildungsrufzeichen",
        "RLFB" to "Relaisfunkstelle / Bake",
        "SZ" to "experimentelle Studien",
        "KSB" to "Klubstation (BOS)",
        "KSO" to "Klubstation (Notfunk)"
    )

    private fun classLabel(c: Char): String? = when (c) {
        'A' -> "Klasse A"
        'E' -> "Klasse E (Einsteiger)"
        'N' -> "Klasse N (Einsteiger)"
        else -> null
    }

    /** Returns (classLabel, purposeCode) for a German Rufzeichenreihe, or null. */
    private fun germanInfo(prefix: String, number: String): Pair<String?, String>? {
        if (number.isEmpty()) return null
        val entry = germanTable[prefix.uppercase() + number.first()] ?: return null
        return classLabel(entry.first) to entry.second
    }

    fun analyze(rawInput: String): CallsignAnalysis {
        val normalized = rawInput.trim().uppercase().replace(Regex("[^A-Z0-9/*]"), "")
        val base = baseCall(normalized)

        val match = Regex("^([A-Z0-9]*?[A-Z])([0-9])([A-Z0-9]*)$").find(base)
        val prefix: String
        val number: String
        val suffix: String
        if (match != null) {
            prefix = match.groupValues[1]; number = match.groupValues[2]; suffix = match.groupValues[3]
        } else {
            prefix = base.takeWhile { it.isLetter() }
            number = base.dropWhile { it.isLetter() }.takeWhile { it.isDigit() }
            suffix = base.dropWhile { it.isLetter() }.dropWhile { it.isDigit() }
        }

        val range = lookupPrefix(base)
        val isGerman = range?.code == "DE"
        val german = if (isGerman) germanInfo(prefix, number) else null
        val germanClass = german?.first

        val notes = buildList {
            if (normalized.contains('*')) add("Enthält Platzhalter '*' – geeignet für die BNetzA-Suche.")
            if (normalized.contains('/')) add("Portabel-/Zusatzkennung erkannt: $normalized")
            german?.let { (_, purposeCode) ->
                purposeLabel[purposeCode]?.let { add("Verwendungszweck: $it") }
            }
            if (isGerman && german == null && prefix.length == 2) {
                add("Deutscher Rufzeichenblock '$prefix' – Reihe nicht eindeutig zuordenbar.")
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
        if (parts.isEmpty()) return call
        val addons = setOf("P", "M", "MM", "AM", "QRP", "A", "R")
        val candidates = parts.filter { it !in addons }.ifEmpty { parts }
        for (p in candidates) if (lookupPrefix(p) != null) return p
        return candidates.maxByOrNull { it.length } ?: call
    }

    private fun lookupPrefix(base: String): PrefixRange? =
        prefixTable.filter { base.startsWith(it.prefix) }.maxByOrNull { it.prefix.length }
}
