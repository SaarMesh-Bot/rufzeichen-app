package de.hamlookup.rufzeichen.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.tools.Locator
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.OwnStation
import de.hamlookup.rufzeichen.ui.ToolsViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private fun d1(v: Double) = "%.1f".format(Locale.US, v)
private fun d5(v: Double) = "%.5f".format(Locale.US, v)
private fun deg(v: Double) = "${v.roundToInt() % 360}°"

private fun ownInput(own: OwnStation?): String =
    own?.let { it.locator?.uppercase() ?: "${d5(it.lat)}, ${d5(it.lon)}" } ?: ""

@Composable
fun LocatorToolsScreen(viewModel: ToolsViewModel) {
    val own by viewModel.ownStation.collectAsStateWithLifecycle()

    var convertIn by rememberSaveable { mutableStateOf("") }
    var aIn by rememberSaveable { mutableStateOf<String?>(null) }
    var bIn by rememberSaveable { mutableStateOf("") }
    val aText = aIn ?: ownInput(own)

    val a = remember(aText) { Locator.parse(aText) }
    val b = remember(bIn) { Locator.parse(bIn) }
    val path = if (a != null && b != null) Locator.path(a, b) else null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConvertCard(convertIn, { convertIn = it }, own)
        PathCard(
            aText = aText, onA = { aIn = it }, a = a,
            bText = bIn, onB = { bIn = it }, b = b,
            own = own,
            onSwap = { val t = aText; aIn = bIn; bIn = t },
            path = path
        )
        CompassCard(target = path?.bearingDeg, from = a)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun PointField(label: String, value: String, onChange: (String) -> Unit, valid: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(Loc.locInputHint) },
        singleLine = true,
        isError = value.isNotBlank() && !valid,
        supportingText = if (value.isNotBlank() && !valid) {{ Text(Loc.locInvalid) }} else null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ResultRow(label: String, value: String, copy: Boolean = false) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().let {
            if (copy) it.clickable {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, Loc.locCopied, Toast.LENGTH_SHORT).show()
            } else it
        }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConvertCard(input: String, onInput: (String) -> Unit, own: OwnStation?) {
    val pos = remember(input) { Locator.parse(input) }
    SectionCard(Loc.locConvertTitle) {
        PointField(Loc.locInputLabel, input, onInput, pos != null)
        if (own != null && input.isBlank()) {
            AssistChip(onClick = { onInput(ownInput(own)) }, label = { Text(Loc.locUseOwn) })
        }
        pos?.let { p ->
            ResultRow(Loc.locLocator6, Locator.encode(p.lat, p.lon, 3), copy = true)
            ResultRow(Loc.locLocator8, Locator.encode(p.lat, p.lon, 4), copy = true)
            ResultRow(Loc.locDecimal, "${d5(p.lat)}, ${d5(p.lon)}", copy = true)
            ResultRow(Loc.locLat, Locator.toDms(p.lat, true))
            ResultRow(Loc.locLon, Locator.toDms(p.lon, false))
            if (p.locator != null) {
                Text(
                    Loc.locCellNote(p.locator, cellKm(p)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(Loc.locCopyHint, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Approximate cell size "w × h km" at the cell's latitude. */
private fun cellKm(p: Locator.Position): String {
    val h = p.cellLatDeg * 111.2
    val w = p.cellLonDeg * 111.32 * Math.cos(Math.toRadians(p.lat))
    fun f(v: Double) = if (v >= 10) "%.0f".format(Locale.US, v) else if (v >= 1) "%.1f".format(Locale.US, v)
        else "%.0f m".format(Locale.US, v * 1000)
    val unit = if (h >= 1) " km" else ""
    return "${f(w)} × ${f(h)}$unit"
}

@Composable
private fun PathCard(
    aText: String, onA: (String) -> Unit, a: Locator.Position?,
    bText: String, onB: (String) -> Unit, b: Locator.Position?,
    own: OwnStation?,
    onSwap: () -> Unit,
    path: Locator.Path?
) {
    val german = Loc.pick("de", "en") == "de"
    SectionCard(Loc.locPathTitle) {
        PointField(Loc.locFrom, aText, onA, a != null)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (own != null) {
                AssistChip(onClick = { onA(ownInput(own)) }, label = { Text(Loc.locUseOwn) })
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSwap) { Text("⇅ " + Loc.locSwap) }
        }
        PointField(Loc.locTo, bText, onB, b != null)
        if (path != null) {
            ResultRow(Loc.locDistance, "${d1(path.distanceKm)} km")
            ResultRow(Loc.locDistanceMi, "${d1(path.distanceKm * 0.621371)} mi")
            ResultRow(Loc.locBearing, "${deg(path.bearingDeg)} ${Locator.compassPoint(path.bearingDeg, german)}")
            ResultRow(Loc.locReverse, "${deg(path.reverseBearingDeg)} ${Locator.compassPoint(path.reverseBearingDeg, german)}")
            ResultRow(Loc.locLongPath, "${deg(path.longPathBearingDeg)} · ${d1(path.longPathKm)} km")
            if (a?.locator != null || b?.locator != null) {
                Text(Loc.locCenterNote, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (aText.isBlank() && own == null) {
            Text(Loc.locNoOwn, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- compass -------------------------------------------------------------------

/** True-north heading in degrees from the rotation vector sensor, null if unavailable. */
@Composable
private fun rememberHeading(lat: Double?, lon: Double?): Pair<Boolean, Double?> {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember {
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    }
    var heading by remember { mutableStateOf<Double?>(null) }
    val declination = remember(lat, lon) {
        if (lat == null || lon == null) 0f
        else GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination
    }

    DisposableEffect(sensor, declination) {
        if (sensor == null) return@DisposableEffect onDispose { }
        val rot = FloatArray(9)
        val remapped = FloatArray(9)
        val orient = FloatArray(3)
        var smooth: Double? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                @Suppress("DEPRECATION")
                val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay.rotation
                val (x, y) = when (rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rot, x, y, remapped)
                SensorManager.getOrientation(remapped, orient)
                val mag = Math.toDegrees(orient[0].toDouble())
                val trueHeading = ((mag + declination) % 360.0 + 360.0) % 360.0
                // Low-pass on the circle to avoid jitter and the 359°->0° jump.
                val prev = smooth
                smooth = if (prev == null) trueHeading else {
                    var diff = trueHeading - prev
                    if (diff > 180) diff -= 360.0
                    if (diff < -180) diff += 360.0
                    ((prev + diff * 0.15) % 360.0 + 360.0) % 360.0
                }
                heading = smooth
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return (sensor != null) to heading
}

@Composable
private fun CompassCard(target: Double?, from: Locator.Position?) {
    val (available, heading) = rememberHeading(from?.lat, from?.lon)
    SectionCard(Loc.locCompassTitle) {
        when {
            !available -> Text(Loc.locNoSensor, style = MaterialTheme.typography.bodyMedium)
            target == null -> Text(Loc.locCompassNeedTarget, style = MaterialTheme.typography.bodyMedium)
            else -> {
                val primary = MaterialTheme.colorScheme.primary
                val outline = MaterialTheme.colorScheme.outline
                val onSurface = MaterialTheme.colorScheme.onSurface
                val error = MaterialTheme.colorScheme.error
                val h = heading ?: 0.0
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.widthIn(max = 260.dp).fillMaxWidth().aspectRatio(1f)) {
                        val r = size.minDimension / 2 * 0.92f
                        val c = Offset(size.width / 2, size.height / 2)
                        drawCircle(outline, r, c, style = Stroke(2.dp.toPx()))
                        // Dial rotates against the heading so N stays north.
                        rotate(-h.toFloat(), c) {
                            for (i in 0 until 36) {
                                rotate(i * 10f, c) {
                                    val len = if (i % 9 == 0) 14.dp.toPx() else 6.dp.toPx()
                                    drawLine(outline, Offset(c.x, c.y - r), Offset(c.x, c.y - r + len),
                                        strokeWidth = if (i % 9 == 0) 3.dp.toPx() else 1.dp.toPx())
                                }
                            }
                            // North marker
                            val n = Path().apply {
                                moveTo(c.x, c.y - r + 18.dp.toPx())
                                lineTo(c.x - 7.dp.toPx(), c.y - r + 34.dp.toPx())
                                lineTo(c.x + 7.dp.toPx(), c.y - r + 34.dp.toPx())
                                close()
                            }
                            drawPath(n, error)
                            // Target arrow (bearing relative to north)
                            rotate(target.toFloat(), c) {
                                drawLine(primary, c, Offset(c.x, c.y - r * 0.78f), strokeWidth = 6.dp.toPx())
                                val head = Path().apply {
                                    moveTo(c.x, c.y - r * 0.9f)
                                    lineTo(c.x - 12.dp.toPx(), c.y - r * 0.7f)
                                    lineTo(c.x + 12.dp.toPx(), c.y - r * 0.7f)
                                    close()
                                }
                                drawPath(head, primary)
                            }
                        }
                        // Fixed "phone points here" mark at the top.
                        drawLine(onSurface, Offset(c.x, c.y - r - 4.dp.toPx()), Offset(c.x, c.y - r + 10.dp.toPx()),
                            strokeWidth = 4.dp.toPx())
                        drawCircle(onSurface, 5.dp.toPx(), c)
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (heading != null) {
                    var diff = target - heading
                    if (diff > 180) diff -= 360.0
                    if (diff < -180) diff += 360.0
                    val text = when {
                        abs(diff) <= 3 -> Loc.locOnTarget
                        diff > 0 -> Loc.locTurnRight(abs(diff).roundToInt())
                        else -> Loc.locTurnLeft(abs(diff).roundToInt())
                    }
                    Box(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(Loc.locHeadingNow(deg(heading), deg(target)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(Loc.locCompassHint, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
