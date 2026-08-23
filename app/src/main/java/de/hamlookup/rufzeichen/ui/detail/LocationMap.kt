package de.hamlookup.rufzeichen.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

/**
 * A small interactive OpenStreetMap view (osmdroid) centred on [lat]/[lon] with
 * a marker at the position. Pan/zoom enabled; the OSM copyright overlay is drawn
 * as required by the tile usage policy.
 */
@Composable
fun LocationMap(
    lat: Double,
    lon: Double,
    label: String?,
    modifier: Modifier = Modifier,
    zoom: Double = 12.0
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            overlays.add(CopyrightOverlay(context))
        }
    }

    // Tie the MapView to the composition lifecycle to avoid leaks / stuck tiles.
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            val point = GeoPoint(lat, lon)
            map.controller.setZoom(zoom)
            map.controller.setCenter(point)
            // keep exactly one marker
            map.overlays.removeAll { it is Marker }
            val marker = Marker(map).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = label
            }
            map.overlays.add(marker)
            map.invalidate()
        }
    )
}

/**
 * Convert a Maidenhead locator (4 or 6 chars) to the centre coordinate of its
 * square/subsquare. Returns null for malformed input. Used as a fallback when
 * the backend supplied a locator but no explicit lat/lon (e.g. older cached
 * records).
 */
fun maidenheadToCenter(locator: String?): Pair<Double, Double>? {
    val loc = locator?.trim()?.uppercase() ?: return null
    if (loc.length < 4) return null
    return try {
        val a = 'A'.code
        var lon = (loc[0].code - a) * 20.0 - 180.0
        var lat = (loc[1].code - a) * 10.0 - 90.0
        lon += Character.digit(loc[2], 10) * 2.0
        lat += Character.digit(loc[3], 10) * 1.0
        if (loc.length >= 6 && loc[4].isLetter() && loc[5].isLetter()) {
            lon += (loc[4].code - a) * (2.0 / 24.0)
            lat += (loc[5].code - a) * (1.0 / 24.0)
            // centre of the subsquare
            lon += (2.0 / 24.0) / 2.0
            lat += (1.0 / 24.0) / 2.0
        } else {
            // centre of the 2° x 1° square
            lon += 1.0
            lat += 0.5
        }
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) null
        else lat to lon
    } catch (e: Exception) {
        null
    }
}

// --------------------------------------------------------------------------
// Great-circle helpers for distance & bearing between two points.
// --------------------------------------------------------------------------

private const val EARTH_RADIUS_KM = 6371.0

/** Great-circle (short-path) distance in kilometres. */
fun greatCircleKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return EARTH_RADIUS_KM * c
}

/** Initial bearing (degrees, 0..360) from point 1 to point 2 along the short path. */
fun initialBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val φ1 = Math.toRadians(lat1)
    val φ2 = Math.toRadians(lat2)
    val Δλ = Math.toRadians(lon2 - lon1)
    val y = Math.sin(Δλ) * Math.cos(φ2)
    val x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ)
    val θ = Math.atan2(y, x)
    return (Math.toDegrees(θ) + 360.0) % 360.0
}
