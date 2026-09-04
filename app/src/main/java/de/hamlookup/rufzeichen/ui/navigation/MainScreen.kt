package de.hamlookup.rufzeichen.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.ui.AppViewModelFactory
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.FavoritesViewModel
import de.hamlookup.rufzeichen.ui.SearchViewModel
import de.hamlookup.rufzeichen.ui.SettingsViewModel
import de.hamlookup.rufzeichen.ui.detail.CallsignDetailContent
import de.hamlookup.rufzeichen.ui.favorites.FavoritesScreen
import de.hamlookup.rufzeichen.ui.favorites.SettingsScreen
import de.hamlookup.rufzeichen.ui.search.SearchScreen

private enum class Tab(val icon: ImageVector) {
    Search(Icons.Filled.Search),
    Favorites(Icons.Filled.Star),
    Settings(Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(factory: AppViewModelFactory) {
    val searchVm: SearchViewModel = viewModel(factory = factory)
    val favVm: FavoritesViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val settings by settingsVm.settings.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(Tab.Search) }
    var detail by remember { mutableStateOf<Callsign?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(Loc.appBarTitle) })
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    val lbl = when (t) {
                        Tab.Search -> Loc.tabSearch
                        Tab.Favorites -> Loc.tabFavorites
                        Tab.Settings -> Loc.tabSettings
                    }
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = lbl) },
                        label = { Text(lbl) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.Search -> SearchScreen(searchVm, onOpenDetail = { detail = it })
                Tab.Favorites -> FavoritesScreen(favVm, onOpenDetail = { detail = it })
                Tab.Settings -> SettingsScreen(settingsVm)
            }
        }
    }

    detail?.let { selected ->
        val isFav by searchVm.isFavorite(selected.callsign)
            .collectAsStateWithLifecycle(initialValue = false)
        ModalBottomSheet(
            onDismissRequest = { detail = null },
            sheetState = sheetState
        ) {
            CallsignDetailContent(
                callsign = selected,
                isFavorite = isFav,
                ownLocator = settings.ownLocator.ifBlank { null },
                ownCallsign = settings.ownCallsign.ifBlank { null },
                ownLat = settings.ownLat,
                ownLon = settings.ownLon,
                onToggleFavorite = { makeFav -> searchVm.setFavorite(selected, makeFav) }
            )
        }
    }
}
