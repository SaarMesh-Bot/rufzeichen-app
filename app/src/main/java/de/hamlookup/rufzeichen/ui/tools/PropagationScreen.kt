package de.hamlookup.rufzeichen.ui.tools

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.remote.KpPoint
import de.hamlookup.rufzeichen.data.remote.Propagation
import de.hamlookup.rufzeichen.data.tools.SunTimes
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.OwnStation
import de.hamlookup.rufzeichen.ui.ToolsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Good = Color(0xFF2E7D32)
private val Fair = Color(0xFFF9A825)
private val Poor = Color(0xFFC62828)
private val Neutral = Color(0xFF757575)

private fun conditionColor(v: String?): Color = when (v?.trim()?.lowercase()) {
    "good" -> Good
    "fair" -> Fair
    "poor", "band closed" -> Poor
    else -> Neutral
}

private fun kpColor(kp: Double): Color = when {
    kp >= 5.0 -> Poor
    kp >= 4.0 -> Fair
    else -> Good
}

private fun num(v: Double?, dec: Int = 0): String =
    v?.let { if (dec == 0) "%.0f".format(Locale.US, it) else "%.${dec}f".format(Locale.US, it) } ?: "–"

private fun utcHm(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))

@Composable
fun PropagationScreen(viewModel: ToolsViewModel) {
    val state by viewModel.propagation.collectAsStateWithLifecycle()
    val own by viewModel.ownStation.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadPropagation() }

    val data = state.data
    when {
        data == null && state.loading -> Centered {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(Loc.wxLoading)
        }
        data == null -> Centered {
            Text(Loc.wxError, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = { viewModel.loadPropagation(force = true) }) { Text(Loc.wxRetry) }
        }
        else -> Content(data, own, state.loading, state.error) { viewModel.loadPropagation(force = true) }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun Content(
    p: Propagation,
    own: OwnStation?,
    loading: Boolean,
    refreshFailed: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Loc.wxUpdated(p.updated ?: p.fetchedAt ?: "–"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (loading) {
                CircularProgressIndicator(Modifier.padding(8.dp).width(20.dp).height(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = Loc.wxRefresh) }
            }
        }
        if (p.stale || refreshFailed) {
            Text(Loc.wxStale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        HfCard(p)
        SolarCard(p)
        if (p.kpHistory.isNotEmpty()) KpCard(p.kpHistory)
        GraylineCard(own)
        if (p.vhf.isNotEmpty()) VhfCard(p)
        p.scales?.let { ScalesCard(it.r, it.s, it.g) }
        SourcesFooter(p)
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
private fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HfCard(p: Propagation) {
    SectionCard(Loc.wxHf) {
        Row(Modifier.fillMaxWidth()) {
            Text(Loc.wxBand, Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Loc.wxDay, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Loc.wxNight, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        p.bands.forEach { b ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(b.name.replace("-", "–"), Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.weight(1f)) { StatusPill(Loc.wxCondition(b.day), conditionColor(b.day)) }
                Box(Modifier.weight(1f)) { StatusPill(Loc.wxCondition(b.night), conditionColor(b.night)) }
            }
        }
        Text(Loc.wxCalcNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SolarCard(p: Propagation) {
    val s = p.solar
    SectionCard(Loc.wxSolar) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric(Loc.wxSfi, num(s.sfi))
            Metric(Loc.wxSsn, num(s.sunspots))
            Metric(Loc.wxK, num(s.kIndex), s.kIndex?.let { kpColor(it) })
            Metric(Loc.wxA, num(s.aIndex), s.aIndex?.let { if (it >= 30) Poor else if (it >= 16) Fair else Good })
            Metric(Loc.wxXray, s.xray ?: "–", s.xray?.firstOrNull()?.let { if (it == 'X') Poor else if (it == 'M') Fair else null })
            Metric(Loc.wxWind, s.solarWind?.let { "${num(it)} km/s" } ?: "–")
            Metric(Loc.wxBz, s.bz?.let { "${num(it, 1)} nT" } ?: "–")
            Metric(Loc.wxGeomag, Loc.wxGeomagText(s.geomagField))
            Metric(Loc.wxNoise, s.signalNoise ?: "–")
        }
    }
}

@Composable
private fun Metric(label: String, value: String, accent: Color? = null) {
    Column(
        Modifier
            .widthIn(min = 92.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = accent ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun KpCard(points: List<KpPoint>) {
    SectionCard(Loc.wxKpTitle) {
        val grid = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val maxKp = 9f
            val n = points.size
            val gap = 2.dp.toPx()
            val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
            // reference lines at Kp 4 and 5
            listOf(4f, 5f).forEach { level ->
                val y = size.height * (1 - level / maxKp)
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            }
            points.forEachIndexed { i, p ->
                val h = (size.height * (p.kp.toFloat() / maxKp)).coerceAtLeast(2.dp.toPx())
                drawRect(
                    kpColor(p.kp),
                    topLeft = Offset(i * (barW + gap), size.height - h),
                    size = Size(barW, h)
                )
            }
        }
        val first = points.first().timeUtc
        val last = points.last().timeUtc
        Row(Modifier.fillMaxWidth()) {
            Text(shortUtc(first), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Kp ${num(points.last().kp, 1)} · ${shortUtc(last)}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Loc.wxKpHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "2026-11-15T18:00:00Z" -> "15.11. 18:00 UTC" */
private fun shortUtc(iso: String): String {
    val m = Regex("""\d{4}-(\d{2})-(\d{2})T(\d{2}):(\d{2})""").find(iso) ?: return iso
    val (mo, d, h, mi) = m.destructured
    return "$d.$mo. $h:$mi UTC"
}

@Composable
private fun GraylineCard(own: OwnStation?) {
    SectionCard(Loc.wxGray) {
        if (own == null) {
            Text(Loc.wxGrayNoQth, style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        val day = remember(own.lat, own.lon) { SunTimes.today(own.lat, own.lon) }
        when {
            day.polarDay -> Text(Loc.wxPolarDay)
            day.polarNight -> Text(Loc.wxPolarNight)
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(Loc.wxSunrise, day.sunriseMs?.let { utcHm(it) } ?: "–")
                Metric(Loc.wxSunset, day.sunsetMs?.let { utcHm(it) } ?: "–")
                own.locator?.let { Metric("QTH", it.uppercase()) }
            }
        }
        Text(Loc.wxGrayHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VhfCard(p: Propagation) {
    SectionCard(Loc.wxVhf) {
        p.vhf.forEach { v ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Loc.wxVhfName(v.name, v.location), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                val closed = v.status?.lowercase()?.contains("closed") == true
                StatusPill(Loc.wxCondition(v.status), if (closed) Neutral else Good)
            }
        }
    }
}

@Composable
private fun ScalesCard(r: Int?, s: Int?, g: Int?) {
    fun c(v: Int?) = when { v == null -> Neutral; v >= 3 -> Poor; v >= 1 -> Fair; else -> Good }
    SectionCard(Loc.wxScales) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("R${r ?: "–"}", c(r))
            StatusPill("S${s ?: "–"}", c(s))
            StatusPill("G${g ?: "–"}", c(g))
        }
        Text(Loc.wxScalesHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SourcesFooter(p: Propagation) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        Text(Loc.wxSources, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        p.sources.forEach { src ->
            Text(
                src.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(top = 2.dp).clickable {
                    runCatching {
                        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(src.url))
                    }
                }
            )
        }
    }
}
