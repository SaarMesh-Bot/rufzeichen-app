package de.hamlookup.rufzeichen.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.repository.FavoriteItem
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.ui.FavSort
import de.hamlookup.rufzeichen.ui.FavoriteRow
import de.hamlookup.rufzeichen.ui.FavoritesViewModel
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.common.EmptyState
import de.hamlookup.rufzeichen.ui.search.CallsignCard
import kotlin.math.roundToInt

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenDetail: (Callsign) -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val hasOwnQth by viewModel.hasOwnQth.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<FavoriteItem?>(null) }

    if (favorites.isEmpty()) {
        EmptyState(Loc.emptyFavorites)
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = sort == FavSort.ADDED,
                onClick = { viewModel.setSort(FavSort.ADDED) },
                label = { Text(Loc.favSortAdded) }
            )
            FilterChip(
                selected = sort == FavSort.DISTANCE,
                onClick = { viewModel.setSort(FavSort.DISTANCE) },
                label = { Text(Loc.favSortDistance) }
            )
        }

        if (sort == FavSort.DISTANCE && !hasOwnQth) {
            Spacer(Modifier.height(8.dp))
            Text(
                Loc.favNoQth,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(favorites, key = { it.item.callsign.callsign }) { row ->
                FavoriteRowItem(
                    row = row,
                    onOpen = { onOpenDetail(row.item.callsign) },
                    onEditNote = { editing = row.item }
                )
            }
        }
    }

    editing?.let { item ->
        NoteDialog(
            initial = item.note.orEmpty(),
            onDismiss = { editing = null },
            onSave = { text ->
                viewModel.updateNote(item.callsign.callsign, text)
                editing = null
            }
        )
    }
}

@Composable
private fun FavoriteRowItem(
    row: FavoriteRow,
    onOpen: () -> Unit,
    onEditNote: () -> Unit
) {
    Column {
        CallsignCard(row.item.callsign, onClick = onOpen)
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                row.distanceKm?.let {
                    Text(
                        Loc.favDistanceKm(it.roundToInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val note = row.item.note
                if (!note.isNullOrBlank()) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEditNote) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = Loc.favEditNote,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NoteDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.favEditNote) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(Loc.favNoteHint) },
                singleLine = false
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(Loc.favSave) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Loc.favCancel) } }
    )
}
