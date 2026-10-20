package de.hamlookup.rufzeichen.data.tools

import java.math.BigDecimal

/**
 * Amateur bands with their ADIF band names and edges (MHz), following the
 * ADIF 3.1 "Band" enumeration. Used by the logbook to offer a band dropdown
 * and to derive the band from an entered frequency.
 */
object AmateurBands {

    data class Band(val name: String, val lowerMhz: Double, val upperMhz: Double)

    val all: List<Band> = listOf(
        Band("2190m", 0.1357, 0.1378),
        Band("630m", 0.472, 0.479),
        Band("160m", 1.8, 2.0),
        Band("80m", 3.5, 4.0),
        Band("60m", 5.06, 5.45),
        Band("40m", 7.0, 7.3),
        Band("30m", 10.1, 10.15),
        Band("20m", 14.0, 14.35),
        Band("17m", 18.068, 18.168),
        Band("15m", 21.0, 21.45),
        Band("12m", 24.89, 24.99),
        Band("10m", 28.0, 29.7),
        Band("6m", 50.0, 54.0),
        Band("4m", 70.0, 71.0),
        Band("2m", 144.0, 148.0),
        Band("70cm", 420.0, 450.0),
        Band("23cm", 1240.0, 1300.0),
        Band("13cm", 2300.0, 2450.0),
        Band("9cm", 3300.0, 3500.0),
        Band("6cm", 5650.0, 5925.0),
        Band("3cm", 10000.0, 10500.0)
    )

    val names: List<String> get() = all.map { it.name }

    /** Parses a frequency in MHz as typed by the user ("7.074" or "7,074"). */
    fun parseMhz(input: String?): Double? {
        val t = input?.trim()?.replace(" ", "")?.replace(',', '.') ?: return null
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    /** ADIF band name for a frequency in MHz, or null if outside all amateur bands. */
    fun bandFor(mhz: Double?): String? {
        if (mhz == null) return null
        return all.firstOrNull { mhz >= it.lowerMhz && mhz <= it.upperMhz }?.name
    }

    /** Normalises a free-text band ("40 m", "70CM") to the ADIF name, if known. */
    fun normalizeName(input: String?): String? {
        val t = input?.trim()?.lowercase()?.replace(" ", "") ?: return null
        return all.firstOrNull { it.name == t }?.name
    }

    /** Frequency formatted for ADIF FREQ (MHz, dot decimal, no trailing zeros). */
    fun formatMhz(mhz: Double): String =
        BigDecimal.valueOf(mhz).stripTrailingZeros().toPlainString()
}
