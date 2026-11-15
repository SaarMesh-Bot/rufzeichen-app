package de.hamlookup.rufzeichen.data.tools

import kotlin.math.abs
import kotlin.math.floor

/**
 * Maidenhead locator and coordinate helpers for the locator tools.
 * Pure functions, no Android dependencies.
 */
object Locator {

    /** A decoded position with its precision (cell size in degrees, 0 for exact coordinates). */
    data class Position(
        val lat: Double,
        val lon: Double,
        /** Normalised input locator, or null when the input was coordinates. */
        val locator: String? = null,
        val cellLatDeg: Double = 0.0,
        val cellLonDeg: Double = 0.0
    )

    private val LOCATOR_RE = Regex("^[A-R]{2}([0-9]{2}([A-X]{2}([0-9]{2}([A-X]{2})?)?)?)?$")

    fun isLocator(s: String): Boolean = LOCATOR_RE.matches(s.trim().uppercase())

    /**
     * Centre of a Maidenhead cell, 2–10 characters (field … extended subsquare).
     * Returns null for malformed input.
     */
    fun decode(raw: String): Position? {
        val loc = raw.trim().uppercase()
        if (!LOCATOR_RE.matches(loc)) return null
        var lon = -180.0
        var lat = -90.0
        var lonSize = 20.0
        var latSize = 10.0
        lon += (loc[0] - 'A') * lonSize
        lat += (loc[1] - 'A') * latSize
        var i = 2
        var level = 1
        while (i + 1 < loc.length) {
            if (level % 2 == 1) { // digits: /10
                lonSize /= 10.0; latSize /= 10.0
                lon += (loc[i] - '0') * lonSize
                lat += (loc[i + 1] - '0') * latSize
            } else {              // letters: /24
                lonSize /= 24.0; latSize /= 24.0
                lon += (loc[i] - 'A') * lonSize
                lat += (loc[i + 1] - 'A') * latSize
            }
            i += 2
            level++
        }
        return Position(lat + latSize / 2, lon + lonSize / 2, loc, latSize, lonSize)
    }

    /** Coordinates -> locator with [pairs] character pairs (1..5 → 2..10 chars). */
    fun encode(lat: Double, lon: Double, pairs: Int = 3): String {
        var lonv = (lon + 180.0).coerceIn(0.0, 359.999999999)
        var latv = (lat + 90.0).coerceIn(0.0, 179.999999999)
        var lonSize = 20.0
        var latSize = 10.0
        val sb = StringBuilder()
        sb.append('A' + floor(lonv / lonSize).toInt())
        sb.append('A' + floor(latv / latSize).toInt())
        lonv %= lonSize; latv %= latSize
        for (level in 1 until pairs.coerceIn(1, 5)) {
            if (level % 2 == 1) {
                lonSize /= 10.0; latSize /= 10.0
                sb.append(floor(lonv / lonSize).toInt())
                sb.append(floor(latv / latSize).toInt())
            } else {
                lonSize /= 24.0; latSize /= 24.0
                sb.append('A' + floor(lonv / lonSize).toInt())
                sb.append('A' + floor(latv / latSize).toInt())
            }
            lonv %= lonSize; latv %= latSize
        }
        return sb.toString()
    }

    /**
     * Parses a locator or a coordinate pair. Accepted coordinate forms:
     * "49.24, 6.99" · "49.24 6.99" · "49,24 6,99" · "49.24N 6.99E" · "N49.24 E6.99" · "49.24;-6.99".
     */
    fun parse(input: String): Position? {
        val s = input.trim()
        if (s.isEmpty()) return null
        decode(s)?.let { return it }

        val tokens: List<String> = when {
            s.contains(';') -> s.split(';')
            Regex("""\s""").containsMatchIn(s.replace(Regex(""",\s+"""), " ")) ->
                s.replace(Regex(""",\s+"""), " ").trim().split(Regex("""\s+"""))
            s.count { it == ',' } == 1 -> s.split(',')
            else -> return null
        }.map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size != 2) return null

        val lat = parseAxis(tokens[0], 'N', 'S') ?: return null
        val lon = parseAxis(tokens[1], 'E', 'W', alt = 'O') ?: return null
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
        return Position(lat, lon)
    }

    /** "49.24N" / "S12,5" / "-6.99" -> signed degrees; 'O' accepts German "Ost". */
    private fun parseAxis(raw: String, pos: Char, neg: Char, alt: Char? = null): Double? {
        var t = raw.trim().uppercase().replace('°', ' ').trim()
        var sign = 1.0
        val first = t.firstOrNull() ?: return null
        val last = t.last()
        fun hemi(c: Char): Double? = when (c) {
            pos, alt -> 1.0
            neg -> -1.0
            else -> null
        }
        hemi(first)?.let { sign = it; t = t.drop(1).trim() }
            ?: hemi(last)?.let { sign = it; t = t.dropLast(1).trim() }
        val v = t.replace(',', '.').toDoubleOrNull() ?: return null
        return v * sign
    }

    /** 49.2417 -> "49° 14′ 30.1″ N" */
    fun toDms(value: Double, isLat: Boolean): String {
        val hemi = if (isLat) (if (value >= 0) "N" else "S") else (if (value >= 0) "E" else "W")
        val a = abs(value)
        var d = floor(a).toInt()
        val mFull = (a - d) * 60.0
        var m = floor(mFull).toInt()
        var sec = (mFull - m) * 60.0
        if (sec >= 59.95) { sec = 0.0; m += 1 }
        if (m >= 60) { m = 0; d += 1 }
        return "%d° %02d′ %04.1f″ %s".format(java.util.Locale.US, d, m, sec, hemi)
    }

    // ---- path between two points -------------------------------------------

    private const val EARTH_RADIUS_KM = 6371.0
    const val EARTH_CIRCUMFERENCE_KM = 2 * Math.PI * EARTH_RADIUS_KM

    data class Path(
        val distanceKm: Double,
        val bearingDeg: Double,
        val reverseBearingDeg: Double
    ) {
        val longPathKm get() = EARTH_CIRCUMFERENCE_KM - distanceKm
        val longPathBearingDeg get() = (bearingDeg + 180.0) % 360.0
    }

    fun path(a: Position, b: Position): Path = Path(
        distanceKm = distanceKm(a.lat, a.lon, b.lat, b.lon),
        bearingDeg = bearing(a.lat, a.lon, b.lat, b.lon),
        reverseBearingDeg = bearing(b.lat, b.lon, a.lat, a.lon)
    )

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
    }

    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = Math.sin(dl) * Math.cos(p2)
        val x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl)
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0
    }

    /** 16-point compass label for a bearing (N, NNE, NE …). */
    fun compassPoint(deg: Double, german: Boolean): String {
        val en = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val de = arrayOf("N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val i = (((deg % 360.0) + 360.0) % 360.0 / 22.5 + 0.5).toInt() % 16
        return if (german) de[i] else en[i]
    }
}
