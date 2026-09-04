package de.hamlookup.rufzeichen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.hamlookup.rufzeichen.data.model.Callsign
import de.hamlookup.rufzeichen.data.repository.CallsignRepository
import de.hamlookup.rufzeichen.data.repository.SearchOutcome
import de.hamlookup.rufzeichen.data.repository.Settings
import de.hamlookup.rufzeichen.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

class FavoritesViewModel(
    private val repository: CallsignRepository
) : ViewModel() {
    val favorites = repository.favorites.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun remove(callsign: Callsign) = viewModelScope.launch {
        repository.toggleFavorite(callsign, false)
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
            FavoritesViewModel(callsignRepository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(settingsRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
