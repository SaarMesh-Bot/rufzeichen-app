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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.model.Callsign
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
    var showHistory by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Rufzeichen") },
            placeholder = { Text("z. B. DL1ABC, W1AW oder db2*k") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (history.isNotEmpty()) {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Suchverlauf")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                viewModel.search()
            })
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Suche läuft …")
                }
            }

            state.error != null -> EmptyState("Fehler: ${state.error}")

            state.outcome == null -> {
                if (history.isEmpty()) {
                    EmptyState("Gib ein Rufzeichen ein. '*' ist als Platzhalter für ein Zeichen erlaubt.")
                } else {
                    HistoryList(
                        history = history.map { it.query },
                        onClick = { viewModel.search(it) },
                        onClear = viewModel::clearHistory,
                        onDelete = viewModel::deleteHistory
                    )
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
                    EmptyState("Keine Treffer gefunden.")
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
                Text("Suchverlauf", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        "Noch keine Suchanfragen.",
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
                ?: callsign.analysis?.let { "${it.country} · ${it.germanClass ?: "Amateurfunk"}" }
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
            Text("Verlauf", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Alle löschen") }
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
                            contentDescription = "„$q“ aus dem Verlauf löschen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
