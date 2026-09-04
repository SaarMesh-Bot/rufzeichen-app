package de.hamlookup.rufzeichen.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.SearchViewModel
import de.hamlookup.rufzeichen.ui.common.EmptyState
import de.hamlookup.rufzeichen.ui.common.ProvenanceChip
import de.hamlookup.rufzeichen.ui.common.SourceChip

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenDetail: (Callsign) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showHistory by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(Loc.searchLabel) },
            placeholder = { Text(Loc.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = Loc.clearInput)
                        }
                    }
                    if (history.isNotEmpty()) {
                        IconButton(onClick = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            showHistory = true
                        }) {
                            Icon(Icons.Filled.DateRange, contentDescription = Loc.searchHistory)
                        }
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().onFocusChanged { searchFocused = it.isFocused },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                viewModel.search()
            })
        )
        Spacer(Modifier.height(12.dp))

        // Verlauf-Dropdown direkt unter dem Feld: erscheint beim Fokus und
        // filtert live mit der Eingabe (aus dem Suchverlauf).
        val q = state.query.trim().uppercase()
        val dropItems = history.map { it.query.uppercase() }.distinct()
            .let { all -> if (q.isEmpty()) all else all.filter { it.startsWith(q) && it != q } }
            .take(8)
        val dropdownVisible = searchFocused && state.outcome == null &&
            !state.loading && dropItems.isNotEmpty()
        if (dropdownVisible) {
            HistoryDropdown(
                items = dropItems,
                onClick = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    viewModel.search(it)
                },
                onDelete = viewModel::deleteHistory
            )
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(Loc.searching)
                }
            }

            state.error != null -> EmptyState(Loc.errorPrefix(state.error ?: ""))

            state.outcome == null -> {
                if (!dropdownVisible) {
                    EmptyState(Loc.emptySearch)
                }
            }

            else -> {
                val outcome = state.outcome!!
                outcome.message?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (outcome.results.isEmpty()) {
                    EmptyState(Loc.noResults)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(outcome.results, key = { it.callsign }) { c ->
                            CallsignCard(c, onClick = { onOpenDetail(c) })
                        }
                    }
                }
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(Loc.searchHistory, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        Loc.noHistory,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    HistoryList(
                        history = history.map { it.query },
                        onClick = {
                            showHistory = false
                            keyboard?.hide()
                            viewModel.search(it)
                        },
                        onClear = viewModel::clearHistory,
                        onDelete = viewModel::deleteHistory
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CallsignCard(callsign: Callsign, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = callsign.callsign,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val src = callsign.sourceName
                    if (src != null) {
                        ProvenanceChip(src, callsign.official)
                    } else {
                        callsign.sources.forEach { SourceChip(it) }
                    }
                }
            }
            val holder = callsign.holderName
            val subtitle = holder
                ?: callsign.licenceClass
                ?: callsign.country
                ?: callsign.analysis?.let { "${it.country} · ${it.germanClass ?: Loc.amateurRadio}" }
            subtitle?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val secondLine = if (holder != null)
                listOfNotNull(callsign.licenceClass, callsign.qth).joinToString(" · ").ifBlank { null }
            else callsign.qth
            secondLine?.let {
                Text(it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HistoryDropdown(
    items: List<String>,
    onClick: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
            items(items) { q ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                    Text(
                        text = q,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick(q) }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                    IconButton(onClick = { onDelete(q) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = Loc.deleteFromHistory(q),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    history: List<String>,
    onClick: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Loc.historyTitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text(Loc.clearAll) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(history.distinct()) { q ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = q,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick(q) }
                            .padding(vertical = 10.dp)
                    )
                    IconButton(onClick = { onDelete(q) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = Loc.deleteFromHistory(q),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
