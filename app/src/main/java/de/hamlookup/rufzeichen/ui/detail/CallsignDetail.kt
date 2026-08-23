package de.hamlookup.rufzeichen.ui.detail

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.ui.common.DetailRow
import de.hamlookup.rufzeichen.ui.common.ProvenanceBadge
import de.hamlookup.rufzeichen.ui.common.SectionTitle
import de.hamlookup.rufzeichen.ui.common.SourceChip

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CallsignDetailContent(
    callsign: Callsign,
    isFavorite: Boolean,
    onToggleFavorite: (Boolean) -> Unit
) {
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
                modifier = Modifier.weight(1f)
            )
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
            callsign.locator?.let { DetailRow("Locator", it) }
            // Any additional key/value pairs not already shown.
            val shown = setOf("Name", "Inhaber", "Klasse", "Ort", "QTH", "Standort", "Land")
            callsign.extra.forEach { (k, v) ->
                if (shown.none { k.contains(it, true) }) DetailRow(k, v)
            }

            // Interactive OpenStreetMap view of the location, from explicit
            // backend coordinates or (fallback) the centre of the locator grid.
            val mapPoint: Pair<Double, Double>? = when {
                callsign.latitude != null && callsign.longitude != null ->
                    callsign.latitude to callsign.longitude
                else -> maidenheadToCenter(callsign.locator)
            }
            if (mapPoint != null) {
                Spacer(Modifier.height(12.dp))
                SectionTitle("Standort (Karte)")
                Spacer(Modifier.height(8.dp))
                LocationMap(
                    lat = mapPoint.first,
                    lon = mapPoint.second,
                    label = callsign.holderName ?: callsign.callsign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Karte: © OpenStreetMap-Mitwirkende (ODbL). Position gemäß " +
                        (if (callsign.latitude != null) "ermittelter Anschrift." else "QTH-Locator (ungefähr)."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        }

        // Offline analysis
        callsign.analysis?.let { a ->
            SectionTitle("Rufzeichen-Analyse (offline)")
            DetailRow("Präfix", a.prefix)
            if (a.number.isNotEmpty()) DetailRow("Ziffer", a.number)
            if (a.suffix.isNotEmpty()) DetailRow("Suffix", a.suffix)
            DetailRow("Land (ITU)", "${a.country} (${a.countryCode})")
            a.germanClass?.let { DetailRow("Klasse (geschätzt)", it) }
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
