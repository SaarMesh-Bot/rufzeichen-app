package de.hamlookup.rufzeichen.ui.bands

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.hamlookup.rufzeichen.data.bandplan.BandPlan
import de.hamlookup.rufzeichen.ui.Loc

private enum class Country(val label: String) { DE("DE"), US("US"), CA("CA") }

@Composable
fun BandPlanScreen() {
    var country by remember { mutableStateOf(Country.DE) }
    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text(
            Loc.bandsTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Country.entries.forEach { c ->
                FilterChip(
                    selected = country == c,
                    onClick = { country = c },
                    label = { Text(c.label) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        when (country) {
            Country.DE -> GermanyTable()
            Country.US -> ClassList(BandPlan.usa())
            Country.CA -> ClassList(BandPlan.canada())
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            Loc.bandsDisclaimer,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GermanyTable() {
    Text(
        Loc.bandsIntroDE,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))

    // Header
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(Loc.bandsColBand, 0.9f)
        HeaderCell(Loc.bandsColRange, 1.8f)
        HeaderCell("A", 1f, TextAlign.End)
        HeaderCell("E", 1f, TextAlign.End)
        HeaderCell("N", 1f, TextAlign.End)
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))

    BandPlan.germany.forEach { r ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            BodyCell(r.band, 0.9f, bold = true)
            BodyCell(r.range, 1.8f)
            PowerCell(r.classA, 1f)
            PowerCell(r.classE, 1f)
            PowerCell(r.classN, 1f)
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(
        Loc.bandsClassSummary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(6.dp))
    listOf(Loc.bandsClassN, Loc.bandsClassE, Loc.bandsClassA).forEach {
        Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
    }
}

@Composable
private fun ClassList(items: List<BandPlan.ClassInfo>) {
    items.forEach { info ->
        Text(info.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            info.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = align
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BodyCell(
    text: String,
    weight: Float,
    bold: Boolean = false
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PowerCell(text: String, weight: Float) {
    val muted = text == BandPlan.NO
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.End,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.onSurface
    )
}
