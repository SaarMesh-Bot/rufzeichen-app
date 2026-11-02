package de.hamlookup.rufzeichen.ui.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.repository.RepeaterFilter
import de.hamlookup.rufzeichen.data.repository.RepeaterPositionSource
import de.hamlookup.rufzeichen.data.tools.RepeaterMap
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.ToolsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * "Repeaters nearby": the user picks modes, bands, radius and a position source;
 * the app then opens the matching radius search on repeatermap.de (DK3ML).
 * No repeater data is fetched or stored by the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RepeaterSearchScreen(viewModel: ToolsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stored by viewModel.repeaterFilter.collectAsStateWithLifecycle()
    val own by viewModel.ownStation.collectAsStateWithLifecycle()
    val filter = stored ?: RepeaterFilter()

    var locating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun save(f: RepeaterFilter) {
        message = null
        viewModel.saveRepeaterFilter(f)
    }

    fun open(lat: Double?, lon: Double?) {
        val uri = RepeaterMap.searchUri(lat, lon, filter.radiusKm, filter.modes, filter.bands)
        if (!openInBrowser(context, uri)) message = Loc.rptNoBrowser
    }

    fun locateAndOpen() {
        locating = true
        message = null
        scope.launch {
            val loc = currentApproxLocation(context)
            locating = false
            if (loc != null) open(loc.latitude, loc.longitude) else message = Loc.rptLocationOff
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) locateAndOpen() else message = Loc.rptNoPermission
    }

    fun search() {
        when (filter.source) {
            RepeaterPositionSource.GPS ->
                if (hasLocationPermission(context)) locateAndOpen()
                else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            RepeaterPositionSource.OWN_STATION -> {
                val o = own
                if (o != null) open(o.lat, o.lon) else message = Loc.rptSrcOwnMissing
            }
            RepeaterPositionSource.PICK_ON_MAP -> open(null, null)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(Loc.rptIntro, style = MaterialTheme.typography.bodyMedium)

        // ---- position source
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                SectionTitle(Loc.rptPosition, Modifier.padding(horizontal = 16.dp))
                SourceRow(
                    title = Loc.rptSrcGps,
                    subtitle = Loc.rptSrcGpsSub,
                    selected = filter.source == RepeaterPositionSource.GPS
                ) { save(filter.copy(source = RepeaterPositionSource.GPS)) }
                SourceRow(
                    title = Loc.rptSrcOwn,
                    subtitle = own?.let { Loc.rptSrcOwnSub(it.locator) } ?: Loc.rptSrcOwnMissing,
                    selected = filter.source == RepeaterPositionSource.OWN_STATION,
                    enabled = own != null
                ) { save(filter.copy(source = RepeaterPositionSource.OWN_STATION)) }
                SourceRow(
                    title = Loc.rptSrcMap,
                    subtitle = Loc.rptSrcMapSub,
                    selected = filter.source == RepeaterPositionSource.PICK_ON_MAP
                ) { save(filter.copy(source = RepeaterPositionSource.PICK_ON_MAP)) }
            }
        }

        // ---- modes
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(Loc.rptModes)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeaterMap.MODES.forEach { m ->
                        FilterChip(
                            selected = m in filter.modes,
                            onClick = { save(filter.copy(modes = filter.modes.toggle(m))) },
                            label = { Text(RepeaterMap.modeLabel(m)) }
                        )
                    }
                }
            }
        }

        // ---- bands
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(Loc.rptBands)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeaterMap.BANDS.forEach { b ->
                        FilterChip(
                            selected = b in filter.bands,
                            onClick = { save(filter.copy(bands = filter.bands.toggle(b))) },
                            label = { Text(if (b == "other") Loc.rptBandOther else b) }
                        )
                    }
                }
            }
        }

        // ---- radius
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(Loc.rptRadius)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeaterMap.RADII_KM.forEach { r ->
                        FilterChip(
                            selected = RepeaterMap.snapRadius(filter.radiusKm) == r,
                            onClick = { save(filter.copy(radiusKm = r)) },
                            label = { Text("$r km") }
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Loc.rptAllHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (filter.modes.isNotEmpty() || filter.bands.isNotEmpty()) {
                TextButton(onClick = { save(filter.copy(modes = emptySet(), bands = emptySet())) }) {
                    Text(Loc.rptReset)
                }
            }
        }

        Button(
            onClick = { search() },
            enabled = !locating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (locating) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(Loc.rptLocating)
            } else {
                Text(Loc.rptShow)
            }
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            Loc.rptCredit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SourceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    onSelect: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Set<String>.toggle(v: String): Set<String> = if (v in this) this - v else this + v

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Opens [uri] in a Custom Tab, falling back to any browser. Returns false if nothing can open it. */
private fun openInBrowser(context: Context, uri: Uri): Boolean = try {
    CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
    true
} catch (e: ActivityNotFoundException) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e2: ActivityNotFoundException) {
        false
    }
}

private const val MAX_LAST_KNOWN_AGE_MS = 10 * 60 * 1000L
private const val LOCATION_TIMEOUT_MS = 20_000L

/**
 * Approximate device location (coarse permission only, no Play Services).
 * Uses a recent last-known fix if available, otherwise asks for a fresh one.
 * Returns null if location is off or nothing arrives in time.
 */
@SuppressLint("MissingPermission")
private suspend fun currentApproxLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    if (!LocationManagerCompat.isLocationEnabled(lm)) return null

    // With coarse permission only the network / fused providers are usable (GPS needs fine).
    val providers = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.PASSIVE_PROVIDER)
    }.filter { p -> runCatching { lm.isProviderEnabled(p) }.getOrDefault(false) }
    if (providers.isEmpty()) return null

    val nowNanos = SystemClock.elapsedRealtimeNanos()
    val recent = providers
        .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        .filter { (nowNanos - it.elapsedRealtimeNanos) / 1_000_000 < MAX_LAST_KNOWN_AGE_MS }
        .maxByOrNull { it.elapsedRealtimeNanos }
    if (recent != null) return recent

    // Passive cannot be asked for a fresh fix.
    val active = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return null
    return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    lm, active, signal, ContextCompat.getMainExecutor(context)
                ) { loc -> if (cont.isActive) cont.resume(loc) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }
}
