package de.hamlookup.rufzeichen.data.tools

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Sunrise / sunset for a position (NOAA "sunrise equation", accurate to ~1–2 min,
 * plenty for grayline planning). Times are epoch milliseconds (UTC). Computed on
 * the device; the position never leaves the phone. No java.time (minSdk 24).
 */
object SunTimes {

    data class Day(
        /** Sunrise / sunset as epoch millis, null for polar day or polar night. */
        val sunriseMs: Long?,
        val sunsetMs: Long?,
        val polarDay: Boolean = false,
        val polarNight: Boolean = false
    )

    private const val DAY_MS = 86_400_000L

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)

    /** Sun times for the UTC calendar day containing [nowMs]. */
    fun today(lat: Double, lon: Double, nowMs: Long = System.currentTimeMillis()): Day =
        forEpochDay(lat, lon, Math.floorDiv(nowMs, DAY_MS))

    /** [altitude] -0.833° = geometric horizon incl. refraction and sun radius. */
    fun forEpochDay(lat: Double, lon: Double, epochDay: Long, altitude: Double = -0.833): Day {
        val jd = epochDay + 2440587.5 + 0.5 // Julian day at 12:00 UTC
        val n = floor(jd - 2451545.0 + 0.0008)
        val jStar = n - lon / 360.0
        val m = (357.5291 + 0.98560028 * jStar).mod(360.0)
        val c = 1.9148 * sin(rad(m)) + 0.02 * sin(rad(2 * m)) + 0.0003 * sin(rad(3 * m))
        val lambda = (m + c + 180.0 + 102.9372).mod(360.0)
        val jTransit = 2451545.0 + jStar + 0.0053 * sin(rad(m)) - 0.0069 * sin(rad(2 * lambda))
        val decl = asin(sin(rad(lambda)) * sin(rad(23.4397)))
        val cosH = (sin(rad(altitude)) - sin(rad(lat)) * sin(decl)) / (cos(rad(lat)) * cos(decl))
        if (cosH < -1) return Day(null, null, polarDay = true)
        if (cosH > 1) return Day(null, null, polarNight = true)
        val h = deg(acos(cosH))
        return Day(
            sunriseMs = julianToMs(jTransit - h / 360.0),
            sunsetMs = julianToMs(jTransit + h / 360.0)
        )
    }

    private fun julianToMs(jd: Double): Long = ((jd - 2440587.5) * DAY_MS).toLong()
}
