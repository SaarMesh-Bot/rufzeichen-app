package de.hamlookup.rufzeichen.data.tools

import android.net.Uri
import java.util.Locale

/**
 * Deep links into the radius search of repeatermap.de (Martin Leuschner, DK3ML).
 *
 * The app does NOT fetch or store any repeater data – it only opens the search
 * result page in the browser. Linking was explicitly permitted by DK3ML (11/2026).
 *
 * Parameter format (tested 02.11.2026):
 *   aroundme.php?step=2&lat=..&lon=..&radius_km=50&modes[]=DMR&modes[]=FM&bands[]=70cm&qrg=
 * - step=2 together with lat/lon shows the result list directly.
 * - modes[] / bands[] values are case-sensitive and must match the site's form;
 *   unknown values are silently ignored by the site (-> unfiltered result).
 * - radius_km only accepts 25 / 50 / 100 / 150; other values fall back to 50.
 * - Without lat/lon the page opens at the position step (map / GPS button).
 */
object RepeaterMap {
    const val BASE_URL = "https://repeatermap.de/aroundme.php"

    /** Operating modes exactly as the site expects them. */
    val MODES = listOf("FM", "C4FM", "DMR", "DStar", "M17", "NXDN", "P25", "TETRA", "ATV")

    /** Bands exactly as the site expects them (sorted by wavelength). */
    val BANDS = listOf("10m", "6m", "2m", "1.25m", "70cm", "33cm", "23cm", "13cm", "9cm", "6cm", "3cm", "other")

    /** Radius values offered by the site's form. */
    val RADII_KM = listOf(25, 50, 100, 150)
    const val DEFAULT_RADIUS_KM = 50

    /** Human-readable label for a mode value. */
    fun modeLabel(mode: String): String = if (mode == "DStar") "D-Star" else mode

    /** Snap any radius to the nearest value the site supports. */
    fun snapRadius(km: Int): Int = RADII_KM.minBy { kotlin.math.abs(it - km) }

    /**
     * Rounds a coordinate to 2 decimals (~1 km). That is plenty for a radius search
     * of 25 km or more and avoids passing the exact position to a third-party site.
     */
    fun roundCoord(v: Double): String = "%.2f".format(Locale.US, v)

    /**
     * Builds the search link. Pass [lat]/[lon] = null to open the page at the
     * position step, where the user picks the location on the site's map.
     */
    fun searchUri(
        lat: Double?,
        lon: Double?,
        radiusKm: Int = DEFAULT_RADIUS_KM,
        modes: Collection<String> = emptyList(),
        bands: Collection<String> = emptyList(),
        language: String = Locale.getDefault().language
    ): Uri {
        val b = Uri.parse(BASE_URL).buildUpon()
        if (lat != null && lon != null) {
            b.appendQueryParameter("step", "2")
            b.appendQueryParameter("lat", roundCoord(lat))
            b.appendQueryParameter("lon", roundCoord(lon))
        }
        // Keep a stable order and drop anything the site would not understand.
        MODES.filter { it in modes }.forEach { b.appendQueryParameter("modes[]", it) }
        BANDS.filter { it in bands }.forEach { b.appendQueryParameter("bands[]", it) }
        b.appendQueryParameter("qrg", "")
        b.appendQueryParameter("radius_km", snapRadius(radiusKm).toString())
        // Same rule as Loc: English only for English devices, German otherwise.
        b.appendQueryParameter("lang", if (language == "en") "en" else "de")
        return b.build()
    }
}
