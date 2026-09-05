package de.hamlookup.rufzeichen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.data.repository.CallsignRepository
import de.hamlookup.rufzeichen.data.repository.FavoriteItem
import de.hamlookup.rufzeichen.data.repository.SearchOutcome
import de.hamlookup.rufzeichen.data.repository.Settings
import de.hamlookup.rufzeichen.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import de.hamlookup.rufzeichen.ui.detail.greatCircleKm
import de.hamlookup.rufzeichen.ui.detail.maidenheadToCenter
import kotlinx.coroutines.launch

/** UI state for the search screen. */
data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val outcome: SearchOutcome? = null,
    val error: String? = null
)

class SearchViewModel(
    private val repository: CallsignRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val history = repository.history.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val favoriteCalls = repository.favorites
        .map { list -> list.map { it.callsign } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun search(query: String = _state.value.query) {
        val q = query.trim()
        if (q.isEmpty()) return
        _state.value = _state.value.copy(query = q, loading = true, error = null)
        viewModelScope.launch {
            try {
                val outcome = repository.search(q)
                _state.value = _state.value.copy(loading = false, outcome = outcome)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: Loc.unknownError
                )
            }
        }
    }

    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }

    fun deleteHistory(query: String) = viewModelScope.launch { repository.deleteHistoryEntry(query) }

    fun isFavorite(callsign: String) = repository.isFavorite(callsign)

    fun setFavorite(callsign: Callsign, favorite: Boolean) = viewModelScope.launch {
        repository.toggleFavorite(callsign, favorite)
    }
}

enum class FavSort { ADDED, DISTANCE }

/** A favourite plus the distance from the user's own QTH (null if unknown). */
data class FavoriteRow(val item: FavoriteItem, val distanceKm: Double?)

class FavoritesViewModel(
    private val repository: CallsignRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _sort = MutableStateFlow(FavSort.ADDED)
    val sort: StateFlow<FavSort> = _sort.asStateFlow()

    // null = show all favourites; otherwise the selected list name.
    private val _selectedList = MutableStateFlow<String?>(null)
    val selectedList: StateFlow<String?> = _selectedList.asStateFlow()

    val lists: StateFlow<List<String>> = repository.favoriteLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasOwnQth: StateFlow<Boolean> = settingsRepository.settings
        .map { ownPoint(it) != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val favorites: StateFlow<List<FavoriteRow>> =
        combine(repository.favoriteItems, settingsRepository.settings, _sort, _selectedList) { items, settings, sort, sel ->
            val own = ownPoint(settings)
            val filtered = if (sel == null) items else items.filter { it.listName == sel }
            val rows = filtered.map { fi ->
                val d = if (own != null) {
                    favPoint(fi)?.let { p -> greatCircleKm(own.first, own.second, p.first, p.second) }
                } else null
                FavoriteRow(fi, d)
            }
            when (sort) {
                FavSort.ADDED -> rows.sortedByDescending { it.item.addedAt }
                FavSort.DISTANCE -> rows.sortedWith(
                    compareBy(nullsLast()) { it.distanceKm }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSort(mode: FavSort) { _sort.value = mode }

    fun selectList(name: String?) { _selectedList.value = name }

    fun createList(name: String) = viewModelScope.launch { repository.createList(name) }

    fun deleteList(name: String) = viewModelScope.launch {
        if (_selectedList.value == name) _selectedList.value = null
        repository.deleteList(name)
    }

    fun assignList(callsign: String, listName: String?) = viewModelScope.launch {
        repository.setFavoriteList(callsign, listName)
    }

    fun updateNote(callsign: String, note: String?) = viewModelScope.launch {
        repository.updateFavoriteNote(callsign, note)
    }

    fun remove(callsign: Callsign) = viewModelScope.launch {
        repository.toggleFavorite(callsign, false)
    }

    private fun ownPoint(s: Settings): Pair<Double, Double>? = when {
        s.ownLat != null && s.ownLon != null -> s.ownLat to s.ownLon
        else -> maidenheadToCenter(s.ownLocator.ifBlank { null })
    }

    private fun favPoint(fi: FavoriteItem): Pair<Double, Double>? {
        val c = fi.callsign
        return when {
            c.latitude != null && c.longitude != null -> c.latitude to c.longitude
            else -> maidenheadToCenter(c.locator)
        }
    }
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Settings()
    )

    fun update(settings: Settings) = viewModelScope.launch {
        settingsRepository.update(settings)
    }
}

/** Factory that wires ViewModels to the shared repositories. */
class AppViewModelFactory(
    private val callsignRepository: CallsignRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SearchViewModel::class.java) ->
            SearchViewModel(callsignRepository) as T
        modelClass.isAssignableFrom(FavoritesViewModel::class.java) ->
            FavoritesViewModel(callsignRepository, settingsRepository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(settingsRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
