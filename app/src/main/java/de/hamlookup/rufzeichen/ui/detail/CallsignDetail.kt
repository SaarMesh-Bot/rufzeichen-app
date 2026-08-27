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
import kotlin.math.roundToInt
import de.hamlookup.rufzeichen.ui.common.DetailRow
import de.hamlookup.rufzeichen.ui.common.ProvenanceBadge
import de.hamlookup.rufzeichen.ui.common.SectionTitle
import de.hamlookup.rufzeichen.ui.common.SourceChip

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CallsignDetailContent(
    callsign: Callsign,
    isFavorite: Boolean,
    ownLocator: String? = null,
    ownCallsign: String? = null,
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

        // Relation to the user's own QTH (shared by the map line and the
        // distance section). "Own station" if the call matches the stored own
        // call sign, or the computed distance is under ~2 km.
        val stationPoint: Pair<Double, Double>? = when {
            callsign.latitude != null && callsign.longitude != null ->
                callsign.latitude to callsign.longitude
            else -> maidenheadToCenter(callsign.locator)
        }
        val ownPoint = maidenheadToCenter(ownLocator)
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
            callsign.locator?.let { DetailRow("Locator", it) }
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
