package de.hamlookup.rufzeichen.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.hamlookup.rufzeichen.data.model.DataSourceType
import de.hamlookup.rufzeichen.ui.Loc

/** Small colored chip that labels the source of a result. */
@Composable
fun SourceChip(source: DataSourceType, modifier: Modifier = Modifier) {
    val color = when (source) {
        DataSourceType.BNETZA -> MaterialTheme.colorScheme.primary
        DataSourceType.HAMQTH -> MaterialTheme.colorScheme.tertiary
        DataSourceType.BACKEND -> MaterialTheme.colorScheme.primary
        DataSourceType.OFFLINE -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = source.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Provenance badge that makes the trust level explicit: an official authority
 * vs. a community database. Never claims "official" for community data.
 */
@Composable
fun ProvenanceBadge(sourceName: String, official: Boolean?, modifier: Modifier = Modifier) {
    val color = when (official) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    val text = when (official) {
        true -> Loc.officialSource(sourceName)
        false -> Loc.communityData(sourceName)
        else -> Loc.sourceGeneric(sourceName)
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Compact provenance chip for list cards. */
@Composable
fun ProvenanceChip(sourceName: String, official: Boolean?, modifier: Modifier = Modifier) {
    val color = when (official) {
        true -> MaterialTheme.colorScheme.primary
        false -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    val prefix = if (official == true) "✓ " else ""
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = "$prefix$sourceName",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A labelled key/value line used in the detail view. */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
