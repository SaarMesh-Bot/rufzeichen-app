package de.hamlookup.rufzeichen.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * A small interactive OpenStreetMap view (osmdroid) centred on [lat]/[lon] with
 * a marker at the position. If [fromLat]/[fromLon] are given (the user's own
 * QTH), a great-circle line is drawn from there to the station, a second marker
 * marks the own location, and the view is zoomed to fit both points.
 * Pan/zoom enabled; the OSM copyright overlay is drawn as required by the tile
 * usage policy.
 */
@Composable
fun LocationMap(
    lat: Double,
    lon: Double,
    label: String?,
    modifier: Modifier = Modifier,
    zoom: Double = 12.0,
    fromLat: Double? = null,
    fromLon: Double? = null
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
            val station = GeoPoint(lat, lon)
            // reset our own overlays (keep the copyright overlay)
            map.overlays.removeAll { it is Marker || it is Polyline }

            val stationMarker = Marker(map).apply {
                position = station
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = label
            }
            map.overlays.add(stationMarker)

            val own = if (fromLat != null && fromLon != null) GeoPoint(fromLat, fromLon) else null
            if (own != null) {
                val line = Polyline().apply {
                    setPoints(greatCircleGeoPoints(own.latitude, own.longitude, lat, lon))
                    outlinePaint.color = android.graphics.Color.parseColor("#1E88E5")
                    outlinePaint.strokeWidth = 6f
                }
                map.overlays.add(line)

                val ownMarker = Marker(map).apply {
                    position = own
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Dein Standort"
                }
                map.overlays.add(ownMarker)

                // Fit both endpoints with a little padding once the view is laid out.
                val box = BoundingBox.fromGeoPointsSafe(listOf(own, station))
                map.post {
                    runCatching { map.zoomToBoundingBox(box.increaseByScale(1.6f), false, 48) }
                }
            } else {
                map.controller.setZoom(zoom)
                map.controller.setCenter(station)
            }
            map.invalidate()
        }
    )
}

/**
 * Convert a Maidenhead locator (4 or 6 chars) to the centre coordinate of its
 * square/subsquare. Returns null for malformed input.
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
            lon += (2.0 / 24.0) / 2.0
            lat += (1.0 / 24.0) / 2.0
        } else {
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
// Great-circle helpers for distance, bearing and a curved connecting line.
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

/** Points along the great circle between two coordinates (for a curved line). */
fun greatCircleGeoPoints(
    lat1: Double, lon1: Double, lat2: Double, lon2: Double, segments: Int = 64
): List<GeoPoint> {
    val φ1 = Math.toRadians(lat1); val λ1 = Math.toRadians(lon1)
    val φ2 = Math.toRadians(lat2); val λ2 = Math.toRadians(lon2)
    val d = 2 * Math.asin(
        Math.sqrt(
            Math.sin((φ2 - φ1) / 2).let { it * it } +
                Math.cos(φ1) * Math.cos(φ2) * Math.sin((λ2 - λ1) / 2).let { it * it }
        )
    )
    if (d == 0.0 || d.isNaN()) return listOf(GeoPoint(lat1, lon1), GeoPoint(lat2, lon2))
    val out = ArrayList<GeoPoint>(segments + 1)
    for (i in 0..segments) {
        val f = i.toDouble() / segments
        val A = Math.sin((1 - f) * d) / Math.sin(d)
        val B = Math.sin(f * d) / Math.sin(d)
        val x = A * Math.cos(φ1) * Math.cos(λ1) + B * Math.cos(φ2) * Math.cos(λ2)
        val y = A * Math.cos(φ1) * Math.sin(λ1) + B * Math.cos(φ2) * Math.sin(λ2)
        val z = A * Math.sin(φ1) + B * Math.sin(φ2)
        val φ = Math.atan2(z, Math.sqrt(x * x + y * y))
        val λ = Math.atan2(y, x)
        out.add(GeoPoint(Math.toDegrees(φ), Math.toDegrees(λ)))
    }
    return out
}

// --------------------------------------------------------------------------
// Coordinates -> Maidenhead (6 chars) and "open in external map" helper.
// --------------------------------------------------------------------------

/** Convert coordinates to a 6-character Maidenhead locator. */
fun latLonToMaidenhead(lat: Double, lon: Double): String {
    val lonS = (lon + 180.0).coerceIn(0.0, 359.9999)
    val latS = (lat + 90.0).coerceIn(0.0, 179.9999)
    val sb = StringBuilder()
    sb.append('A' + (lonS / 20).toInt())
    sb.append('A' + (latS / 10).toInt())
    sb.append(((lonS % 20) / 2).toInt())
    sb.append((latS % 10).toInt())
    sb.append('A' + ((lonS % 2) / (2.0 / 24.0)).toInt())
    sb.append('A' + ((latS % 1.0) / (1.0 / 24.0)).toInt())
    return sb.toString()
}

/** Open a position in the device's map/navigation app, falling back to OSM web. */
fun openInMaps(context: android.content.Context, lat: Double, lon: Double, label: String?) {
    val q = if (label.isNullOrBlank()) "$lat,$lon" else "$lat,$lon(" +
        android.net.Uri.encode(label) + ")"
    val geo = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("geo:$lat,$lon?q=$q")
    )
    val ok = runCatching { context.startActivity(geo) }.isSuccess
    if (!ok) {
        val web = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=14/$lat/$lon")
        )
        runCatching { context.startActivity(web) }
    }
}

/**
 * Interactive OSM map for picking a QTH: tapping the map computes the 6-char
 * Maidenhead locator of the tapped point and reports it via [onPicked].
 */
@Composable
fun LocatorPickerMap(
    initialLocator: String?,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val start = maidenheadToCenter(initialLocator)
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            overlays.add(CopyrightOverlay(context))
            controller.setZoom(if (start != null) 9.0 else 5.0)
            controller.setCenter(GeoPoint(start?.first ?: 51.0, start?.second ?: 10.0))
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : org.osmdroid.events.MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                mapView.overlays.removeAll { it is Marker }
                val m = Marker(mapView).apply {
                    position = p
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(m)
                mapView.invalidate()
                onPicked(latLonToMaidenhead(p.latitude, p.longitude))
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        val events = org.osmdroid.views.overlay.MapEventsOverlay(receiver)
        mapView.overlays.add(0, events)
        // initial marker at the current locator, if any
        if (start != null) {
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(start.first, start.second)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
        }
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
