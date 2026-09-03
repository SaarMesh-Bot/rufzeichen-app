package de.hamlookup.rufzeichen.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.hamlookup.rufzeichen.data.model.Callsign
import kotlin.math.roundToInt
import de.hamlookup.rufzeichen.ui.common.DetailRow
import de.hamlookup.rufzeichen.ui.common.ProvenanceBadge
import de.hamlookup.rufzeichen.ui.common.SectionTitle
import de.hamlookup.rufzeichen.ui.common.SourceChip

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CallsignDetailContent(
    callsign: Callsign,
    isFavorite: Boolean,
    ownLocator: String? = null,
    ownCallsign: String? = null,
    ownLat: Double? = null,
    ownLon: Double? = null,
    onToggleFavorite: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = callsign.callsign,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { copyToClipboard(context, "Rufzeichen", callsign.callsign) }
                    )
            )
            IconButton(onClick = { shareCallsign(context, callsign) }) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Teilen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onToggleFavorite(!isFavorite) }) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = if (isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten",
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        // Provenance: an explicit official vs. community badge when we know the
        // concrete source; otherwise the generic on-device/offline chips.
        val src = callsign.sourceName
        if (src != null) {
            ProvenanceBadge(src, callsign.official)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                callsign.sources.forEach { SourceChip(it) }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Relation to the user's own QTH (shared by the map line and the
        // distance section). "Own station" if the call matches the stored own
        // call sign, or the computed distance is under ~2 km.
        val stationPoint: Pair<Double, Double>? = when {
            callsign.latitude != null && callsign.longitude != null ->
                callsign.latitude to callsign.longitude
            else -> maidenheadToCenter(callsign.locator)
        }
        val ownPoint = if (ownLat != null && ownLon != null) ownLat to ownLon
            else maidenheadToCenter(ownLocator)
        val selfByCallsign = !ownCallsign.isNullOrBlank() &&
            callsign.callsign.equals(ownCallsign, ignoreCase = true)
        val distanceKm = if (stationPoint != null && ownPoint != null)
            greatCircleKm(ownPoint.first, ownPoint.second, stationPoint.first, stationPoint.second)
        else null
        val isOwnStation = selfByCallsign || (distanceKm != null && distanceKm < 2.0)

        // Data from the sources
        val hasSourceData = callsign.holderName != null || callsign.licenceClass != null ||
            callsign.qth != null || callsign.licenseStatus != null ||
            callsign.locator != null || callsign.extra.isNotEmpty()
        if (hasSourceData) {
            SectionTitle("Zuteilungsdaten")
            callsign.holderName?.let { DetailRow("Inhaber", it) }
            callsign.licenceClass?.let { DetailRow("Klasse", it) }
            callsign.licenseStatus?.let { DetailRow("Status", it) }
            callsign.qth?.let { DetailRow("Standort", it) }
            val land = when {
                callsign.country != null && callsign.countryCode != null ->
                    "${callsign.country} (${callsign.countryCode})"
                else -> callsign.country
            }
            land?.let { DetailRow("Land", it) }
            callsign.locator?.let { loc ->
                Box(Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { copyToClipboard(context, "Locator", loc) }
                )) { DetailRow("Locator", loc) }
            }
            // Any additional key/value pairs not already shown.
            val shown = setOf("Name", "Inhaber", "Klasse", "Ort", "QTH", "Standort", "Land")
            callsign.extra.forEach { (k, v) ->
                if (shown.none { k.contains(it, true) }) DetailRow(k, v)
            }

            // Interactive OpenStreetMap view of the location, from explicit
            // backend coordinates or (fallback) the centre of the locator grid.
            if (stationPoint != null) {
                Spacer(Modifier.height(12.dp))
                SectionTitle("Standort (Karte)")
                Spacer(Modifier.height(8.dp))
                val showLine = ownPoint != null && !isOwnStation
                LocationMap(
                    lat = stationPoint.first,
                    lon = stationPoint.second,
                    label = callsign.holderName ?: callsign.callsign,
                    fromLat = if (showLine) ownPoint?.first else null,
                    fromLon = if (showLine) ownPoint?.second else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Karte: © OpenStreetMap-Mitwirkende (ODbL). Position gemäß " +
                        (if (callsign.latitude != null) "ermittelter Anschrift." else "QTH-Locator (ungefähr).") +
                        (if (showLine) " Blaue Linie: Großkreis von deinem Standort." else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "In Karte öffnen",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            openInMaps(context, stationPoint.first, stationPoint.second,
                                callsign.holderName ?: callsign.callsign)
                        },
                        onLongClick = {}
                    )
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        }

        // Distance & bearing from the user's own QTH to this station.
        if (isOwnStation) {
            SectionTitle("Standort")
            Text(
                "Das ist dein eigener Standort.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        } else if (stationPoint != null && ownPoint != null) {
            val km = distanceKm ?: 0.0
            val shortBrg = initialBearing(ownPoint.first, ownPoint.second,
                stationPoint.first, stationPoint.second).roundToInt() % 360
            val longBrg = (shortBrg + 180) % 360
            SectionTitle("Entfernung & Peilung")
            DetailRow("Entfernung", "${km.roundToInt()} km")
            DetailRow("Peilung (Kurzpfad)", "$shortBrg°")
            DetailRow("Peilung (Langpfad)", "$longBrg°")
            ownLocator?.let { DetailRow("von deinem Standort", it) }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        } else if (stationPoint != null && ownLocator.isNullOrBlank()) {
            Text(
                "Tipp: Hinterlege deinen Standort (Locator) in den Einstellungen, um " +
                    "Entfernung und Peilung zu dieser Station zu sehen.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }

        // Offline analysis
        callsign.analysis?.let { a ->
            SectionTitle("Rufzeichen-Analyse (offline)")
            DetailRow("Präfix", a.prefix)
            if (a.number.isNotEmpty()) DetailRow("Ziffer", a.number)
            if (a.suffix.isNotEmpty()) DetailRow("Suffix", a.suffix)
            DetailRow("Land (ITU)", "${a.country} (${a.countryCode})")
            a.continent?.let { DetailRow("Kontinent", continentName(it)) }
            a.cqZone?.let { DetailRow("CQ-Zone", it.toString()) }
            a.ituZone?.let { DetailRow("ITU-Zone", it.toString()) }
            a.germanClass?.let { DetailRow("Klasse (geschätzt)", it) }
            a.currentLocation?.let { loc ->
                DetailRow("Aktueller Standort", a.currentLocationCode?.let { "$loc ($it)" } ?: loc)
            }
            a.operatingMode?.let { DetailRow("Betriebsart", it) }
            if (a.notes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                a.notes.forEach { note ->
                    Text(
                        text = "• $note",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun continentName(code: String): String = when (code.uppercase()) {
    "EU" -> "Europa (EU)"
    "AS" -> "Asien (AS)"
    "NA" -> "Nordamerika (NA)"
    "SA" -> "Südamerika (SA)"
    "AF" -> "Afrika (AF)"
    "OC" -> "Ozeanien (OC)"
    "AN" -> "Antarktis (AN)"
    else -> code
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label kopiert", Toast.LENGTH_SHORT).show()
}

private fun shareCallsign(context: Context, c: Callsign) {
    val lines = buildList {
        val holder = c.holderName
        add(if (holder != null) "${c.callsign} – $holder" else c.callsign)
        c.licenceClass?.let { add("Klasse: $it") }
        val land = when {
            c.country != null && c.countryCode != null -> "${c.country} (${c.countryCode})"
            else -> c.country
        }
        land?.let { add("Land: $it") }
        c.qth?.let { add("Standort: $it") }
        c.locator?.let { add("Locator: $it") }
        c.analysis?.let { a ->
            a.currentLocation?.let { cur ->
                add("Aktuell in: " + (a.currentLocationCode?.let { "$cur ($it)" } ?: cur))
            }
            a.operatingMode?.let { add("Betriebsart: $it") }
        }
        c.sourceName?.let { src ->
            add("Quelle: $src" + if (c.official == true) " (offiziell)" else "")
        }
        add("")
        add("via Rufzeichen – Amateurfunk")
    }
    val text = lines.joinToString("\n")
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Rufzeichen ${c.callsign}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Teilen")) }
}
