package de.hamlookup.rufzeichen.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.ui.common.DetailRow
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

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            callsign.sources.forEach { SourceChip(it) }
        }

        Spacer(Modifier.height(12.dp))

        // Data from the sources
        val hasSourceData = callsign.holderName != null || callsign.licenceClass != null ||
            callsign.qth != null || callsign.extra.isNotEmpty()
        if (hasSourceData) {
            SectionTitle("Zuteilungsdaten")
            callsign.holderName?.let { DetailRow("Inhaber", it) }
            callsign.licenceClass?.let { DetailRow("Klasse", it) }
            callsign.qth?.let { DetailRow("Standort", it) }
            callsign.country?.let { DetailRow("Land", it) }
            // Any additional key/value pairs not already shown.
            val shown = setOf("Name", "Inhaber", "Klasse", "Ort", "QTH", "Standort", "Land")
            callsign.extra.forEach { (k, v) ->
                if (shown.none { k.contains(it, true) }) DetailRow(k, v)
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
