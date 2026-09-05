package de.hamlookup.rufzeichen.ui.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.data.repository.FavoriteItem
import de.hamlookup.rufzeichen.ui.FavSort
import de.hamlookup.rufzeichen.ui.FavoriteRow
import de.hamlookup.rufzeichen.ui.FavoritesViewModel
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.common.EmptyState
import de.hamlookup.rufzeichen.ui.search.CallsignCard
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenDetail: (Callsign) -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val hasOwnQth by viewModel.hasOwnQth.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val selectedList by viewModel.selectedList.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<FavoriteItem?>(null) }
    var creatingList by remember { mutableStateOf(false) }
    var deletingList by remember { mutableStateOf<String?>(null) }

    val noFavoritesAtAll = favorites.isEmpty() && selectedList == null && lists.isEmpty()
    if (noFavoritesAtAll) {
        EmptyState(Loc.emptyFavorites)
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Lists filter row (Alle · <lists> · +)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedList == null,
                    onClick = { viewModel.selectList(null) },
                    label = { Text(Loc.favListAll) }
                )
            }
            items(lists, key = { it }) { name ->
                FilterChip(
                    selected = selectedList == name,
                    onClick = { viewModel.selectList(name) },
                    modifier = Modifier.combinedClickable(
                        onClick = { viewModel.selectList(name) },
                        onLongClick = { deletingList = name }
                    ),
                    label = { Text(name) }
                )
            }
            item {
                AssistChip(
                    onClick = { creatingList = true },
                    label = { Text(Loc.favNewList) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            }
        }

        if (lists.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                Loc.favManageHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(10.dp))

        // Sort row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

        if (favorites.isEmpty()) {
            Text(
                Loc.emptyFavorites,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favorites, key = { it.item.callsign.callsign }) { row ->
                    FavoriteRowItem(
                        row = row,
                        onOpen = { onOpenDetail(row.item.callsign) },
                        onEdit = { editing = row.item }
                    )
                }
            }
        }
    }

    editing?.let { item ->
        EditFavoriteDialog(
            item = item,
            lists = lists,
            onDismiss = { editing = null },
            onSave = { note, listName ->
                viewModel.updateNote(item.callsign.callsign, note)
                viewModel.assignList(item.callsign.callsign, listName)
                editing = null
            },
            onCreateAndAssign = { newName ->
                viewModel.createList(newName)
                viewModel.assignList(item.callsign.callsign, newName)
            }
        )
    }

    if (creatingList) {
        NewListDialog(
            onDismiss = { creatingList = false },
            onCreate = { name ->
                viewModel.createList(name)
                creatingList = false
            }
        )
    }

    deletingList?.let { name ->
        AlertDialog(
            onDismissRequest = { deletingList = null },
            title = { Text(Loc.favDeleteListTitle) },
            text = { Text(Loc.favDeleteListMsg(name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteList(name); deletingList = null }) {
                    Text(Loc.favDelete)
                }
            },
            dismissButton = { TextButton(onClick = { deletingList = null }) { Text(Loc.favCancel) } }
        )
    }
}

@Composable
private fun FavoriteRowItem(
    row: FavoriteRow,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    Column {
        CallsignCard(row.item.callsign, onClick = onOpen)
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.distanceKm?.let {
                        Text(
                            Loc.favDistanceKm(it.roundToInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    row.item.listName?.let {
                        Text(
                            "▸ $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
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
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = Loc.favEditFavorite,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditFavoriteDialog(
    item: FavoriteItem,
    lists: List<String>,
    onDismiss: () -> Unit,
    onSave: (note: String, listName: String?) -> Unit,
    onCreateAndAssign: (String) -> Unit
) {
    var note by remember { mutableStateOf(item.note.orEmpty()) }
    var chosen by remember { mutableStateOf(item.listName) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.favEditFavorite) },
        text = {
            Column {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(Loc.favNoteHint) },
                    singleLine = false
                )
                Spacer(Modifier.height(14.dp))
                Text(Loc.favListLabel, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = chosen == null,
                        onClick = { chosen = null },
                        label = { Text(Loc.favNoList) }
                    )
                    lists.forEach { name ->
                        FilterChip(
                            selected = chosen == name,
                            onClick = { chosen = name },
                            label = { Text(name) }
                        )
                    }
                    AssistChip(
                        onClick = { creating = !creating },
                        label = { Text(Loc.favNewListShort) }
                    )
                }
                if (creating) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text(Loc.favListNameHint) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = newName.trim()
                if (creating && trimmed.isNotEmpty()) {
                    onCreateAndAssign(trimmed)
                    onSave(note, trimmed)
                } else {
                    onSave(note, chosen)
                }
            }) { Text(Loc.favSave) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Loc.favCancel) } }
    )
}

@Composable
private fun NewListDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.favNewList) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(Loc.favListNameHint) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.trim().isNotEmpty()) onCreate(name.trim()) }
            ) { Text(Loc.favCreate) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Loc.favCancel) } }
    )
}
