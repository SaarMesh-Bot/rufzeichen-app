package de.hamlookup.rufzeichen.data.tools

/**
 * Guideline coax attenuation figures (dB per 100 m) at a few reference
 * frequencies for common amateur feedlines. Interpolated by RadioMath.
 * These are typical manufacturer values — actual loss varies by make and age.
 */
object CoaxData {

    data class Cable(val name: String, val lossPer100m: List<Pair<Double, Double>>)

    val cables: List<Cable> = listOf(
        Cable("RG58", listOf(28.0 to 8.9, 144.0 to 20.5, 432.0 to 37.0, 1296.0 to 70.0)),
        Cable("RG213", listOf(28.0 to 3.6, 144.0 to 8.5, 432.0 to 15.8, 1296.0 to 29.0)),
        Cable("Aircell 7", listOf(28.0 to 2.9, 144.0 to 6.6, 432.0 to 11.9, 1296.0 to 22.0)),
        Cable("Aircom Plus", listOf(28.0 to 2.2, 144.0 to 5.1, 432.0 to 9.2, 1296.0 to 17.0)),
        Cable("Ecoflex 10", listOf(28.0 to 1.9, 144.0 to 4.5, 432.0 to 8.2, 1296.0 to 15.0)),
        Cable("H2000 Flex", listOf(28.0 to 2.0, 144.0 to 4.8, 432.0 to 8.5, 1296.0 to 15.5)),
        Cable("H155", listOf(28.0 to 4.0, 144.0 to 9.3, 432.0 to 16.8, 1296.0 to 31.0))
    )

    fun byName(name: String): Cable? = cables.firstOrNull { it.name == name }
}
