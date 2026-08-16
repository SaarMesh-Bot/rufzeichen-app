package de.hamlookup.rufzeichen.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.ui.FavoritesViewModel
import de.hamlookup.rufzeichen.ui.common.EmptyState
import de.hamlookup.rufzeichen.ui.search.CallsignCard

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenDetail: (Callsign) -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    if (favorites.isEmpty()) {
        EmptyState("Noch keine Favoriten gespeichert. Tippe in der Detailansicht auf den Stern.")
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(favorites, key = { it.callsign }) { c ->
                CallsignCard(c, onClick = { onOpenDetail(c) })
            }
        }
    }
}
