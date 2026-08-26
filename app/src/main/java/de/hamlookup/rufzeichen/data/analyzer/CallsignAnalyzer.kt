package de.hamlookup.rufzeichen.data.analyzer

import de.hamlookup.rufzeichen.data.model.CallsignAnalysis

/**
 * Offline call sign analysis. Splits a call sign into prefix / number / suffix,
 * derives the ITU country (DXCC entity) from the prefix, its continent and –
 * where an entity sits in a single zone – its nominal CQ/ITU zone, and for
 * German call signs the licence class and purpose from the official
 * "Rufzeichenplan" (BNetzA Vfg. 15/2025, gültig ab 01.04.2025).
 *
 * Important: the German class depends on the *Rufzeichenreihe* = prefix + digit,
 * not on the two-letter prefix alone. E.g. within the DN block:
 *   DN1–DN6 = Ausbildungsrufzeichen (Klasse A), DN7–DN8 = Ausbildung (Klasse E),
 *   DN9 = Klasse N, DN0 = Klubstation (Klasse E).
 *
 * Zones: large entities (USA, Kanada, Russland, Australien …) span several CQ/ITU
 * zones. For those the zone is left null (not shown) rather than guessed; the
 * continent is still given where it is unambiguous.
 *
 * This works entirely without network access and is the always-available base
 * layer of every lookup. The authoritative class still comes from BNetzA online.
 */
object CallsignAnalyzer {

    private data class PrefixRange(
        val prefix: String,
        val country: String,
        val code: String,
        val continent: String? = null,
        val cq: Int? = null,
        val itu: Int? = null
    )

    // Country resolution (longest-match). German blocks all map to DE.
    private val prefixTable: List<PrefixRange> = listOf(
        PrefixRange("DA", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DB", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("DC", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DD", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("DF", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DG", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("DH", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DJ", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("DK", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DL", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("DM", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DN", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("DO", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DP", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("DQ", "Deutschland", "DE", "EU", 14, 28), PrefixRange("DR", "Deutschland", "DE", "EU", 14, 28),
        PrefixRange("OE", "Österreich", "AT", "EU", 15, 28),
        PrefixRange("HB9", "Schweiz", "CH", "EU", 14, 28),
        PrefixRange("HB0", "Liechtenstein", "LI", "EU", 14, 28),
        PrefixRange("PA", "Niederlande", "NL", "EU", 14, 27), PrefixRange("PB", "Niederlande", "NL", "EU", 14, 27),
        PrefixRange("PD", "Niederlande", "NL", "EU", 14, 27), PrefixRange("PE", "Niederlande", "NL", "EU", 14, 27),
        PrefixRange("ON", "Belgien", "BE", "EU", 14, 27), PrefixRange("LX", "Luxemburg", "LU", "EU", 14, 27),
        PrefixRange("F", "Frankreich", "FR", "EU", 14, 27), PrefixRange("G", "Großbritannien", "GB", "EU", 14, 27),
        PrefixRange("M", "Großbritannien", "GB", "EU", 14, 27), PrefixRange("2E", "Großbritannien", "GB", "EU", 14, 27),
        PrefixRange("EI", "Irland", "IE", "EU", 14, 27), PrefixRange("EJ", "Irland (Inseln)", "IE", "EU", 14, 27),
        PrefixRange("LZ", "Bulgarien", "BG", "EU", 20, 28), PrefixRange("EA", "Spanien", "ES", "EU", 14, 37),
        PrefixRange("CT", "Portugal", "PT", "EU", 14, 37), PrefixRange("I", "Italien", "IT", "EU", 15, 28),
        PrefixRange("SP", "Polen", "PL", "EU", 15, 28), PrefixRange("OK", "Tschechien", "CZ", "EU", 15, 28),
        PrefixRange("OM", "Slowakei", "SK", "EU", 15, 28), PrefixRange("HA", "Ungarn", "HU", "EU", 15, 28),
        PrefixRange("HG", "Ungarn", "HU", "EU", 15, 28), PrefixRange("OZ", "Dänemark", "DK", "EU", 14, 18),
        PrefixRange("SM", "Schweden", "SE", "EU", 14, 18), PrefixRange("LA", "Norwegen", "NO", "EU", 14, 18),
        PrefixRange("OH", "Finnland", "FI", "EU", 15, 18), PrefixRange("SV", "Griechenland", "GR", "EU", 20, 28),
        PrefixRange("TA", "Türkei", "TR", "AS", 20, 39), PrefixRange("UA", "Russland", "RU", null, null, null),
        PrefixRange("UR", "Ukraine", "UA", "EU", 16, 29),
        PrefixRange("K", "USA", "US", "NA", null, null), PrefixRange("N", "USA", "US", "NA", null, null),
        PrefixRange("W", "USA", "US", "NA", null, null), PrefixRange("AA", "USA", "US", "NA", null, null),
        PrefixRange("VE", "Kanada", "CA", "NA", null, null), PrefixRange("VA", "Kanada", "CA", "NA", null, null),
        PrefixRange("JA", "Japan", "JP", "AS", 25, 45), PrefixRange("VK", "Australien", "AU", "OC", null, null),
        PrefixRange("ZL", "Neuseeland", "NZ", "OC", 32, 60), PrefixRange("PY", "Brasilien", "BR", "SA", 11, null),
        PrefixRange("LU", "Argentinien", "AR", "SA", 13, null), PrefixRange("ZS", "Südafrika", "ZA", "AF", 38, 57)
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
        val parts = parseCompound(normalized)
        val base = parts.home

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

        // Roaming / portable enrichment.
        val guestRange = parts.guest?.takeIf { !parts.guestIsRegionDigit }?.let { lookupPrefix(it) }
        val currentLocation = when {
            parts.guest == null || parts.guestIsRegionDigit -> null
            guestRange != null -> guestRange.country
            else -> "Ausland"
        }
        val currentLocationCode = if (parts.guestIsRegionDigit) null else parts.guest
        val operatingMode = modeLabel(parts.addons)

        val notes = buildList {
            if (normalized.contains('*')) add("Enthält Platzhalter '*' – geeignet für die BNetzA-Suche.")
            if (currentLocation != null) {
                val loc = currentLocationCode?.let { "$currentLocation ($it)" } ?: currentLocation
                add("Roaming: $base funkt zurzeit aus $loc.")
            }
            if (parts.guestIsRegionDigit) add("Abweichendes Rufzeichengebiet: ${parts.guest}")
            operatingMode?.let { add("Betriebsart: $it") }
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
            continent = range?.continent,
            cqZone = range?.cq,
            ituZone = range?.itu,
            currentLocation = currentLocation,
            currentLocationCode = currentLocationCode,
            operatingMode = operatingMode,
            notes = notes
        )
    }

    // ---- compound (roaming / portable) parsing -------------------------------

    private data class Compound(
        val home: String,
        val guest: String?,
        val guestIsRegionDigit: Boolean,
        val addons: List<String>
    )

    private val ADDONS = setOf("P", "M", "MM", "AM", "A", "QRP", "R", "LH")

    private val modeLabels = mapOf(
        "P" to "Portabel",
        "M" to "Mobil",
        "MM" to "Maritim mobil (See)",
        "AM" to "Aeronautisch mobil (Luft)",
        "QRP" to "Kleinleistung (QRP)",
        "A" to "alternativer Standort",
        "R" to "Rover",
        "LH" to "Leuchtturm"
    )

    private fun modeLabel(addons: List<String>): String? =
        addons.mapNotNull { modeLabels[it] }.joinToString(", ").ifEmpty { null }

    /** A full call sign: letter, then digit, then at least one more letter, len >= 4.
     *  Separates a real home call (DC4AC) from a bare prefix (EI, EJ, LZ, 3A, W4). */
    private fun isFullCall(s: String): Boolean =
        s.length >= 4 && Regex("[A-Z][0-9].*[A-Z]").containsMatchIn(s)

    /** Split a compound call into home call / guest (location) prefix / add-ons. */
    private fun parseCompound(call: String): Compound {
        if (!call.contains('/')) return Compound(call, null, false, emptyList())
        val segs = call.split('/').filter { it.isNotEmpty() }
        if (segs.size < 2) return Compound(segs.firstOrNull() ?: call, null, false, emptyList())
        val nonAddon = segs.filter { it !in ADDONS }
        val pool = if (nonAddon.isEmpty()) segs else nonAddon
        val home = pool.filter { isFullCall(it) }.maxByOrNull { it.length }
            ?: pool.maxByOrNull { it.length } ?: call
        val homeIdx = segs.indexOf(home)
        val before = if (homeIdx > 0) segs.subList(0, homeIdx) else emptyList()
        val after = if (homeIdx in 0 until segs.size - 1) segs.subList(homeIdx + 1, segs.size) else emptyList()
        val addons = after.filter { it in ADDONS }
        // Guest/location prefix: a leading segment, or a trailing non-add-on (US style).
        val guest = before.firstOrNull() ?: after.firstOrNull { it !in ADDONS }
        val regionDigit = guest != null && guest.all { it.isDigit() }
        return Compound(home, guest, regionDigit, addons)
    }

    /** The home call sign used for the actual lookup (EJ/DC4AC/P -> DC4AC). */
    fun homeCall(rawInput: String): String {
        val norm = rawInput.trim().uppercase().replace(Regex("[^A-Z0-9/*]"), "")
        return parseCompound(norm).home
    }

    private fun lookupPrefix(base: String): PrefixRange? =
        prefixTable.filter { base.startsWith(it.prefix) }.maxByOrNull { it.prefix.length }
}
