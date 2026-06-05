package com.example.proyectofinal.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.model.Gamer
import com.example.proyectofinal.data.repository.GamerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val allGamers: List<Gamer> = emptyList(),
    val filteredGamers: List<Gamer> = emptyList(),
    val minLevel: Int = 1,
    val selectedPlatform: String? = null,
    val showFilters: Boolean = false,
    val hasSearched: Boolean = false
)

class SearchViewModel(private val repository: GamerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllGamers().collect { gamers ->
                _uiState.update { it.copy(allGamers = gamers) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length >= 2) {
            applyFilters(query, _uiState.value.minLevel, _uiState.value.selectedPlatform)
        } else {
            _uiState.update { it.copy(filteredGamers = emptyList(), hasSearched = false) }
        }
    }

    fun onSearch() {
        val query = _uiState.value.query
        if (query.length >= 2) {
            applyFilters(query, _uiState.value.minLevel, _uiState.value.selectedPlatform)
        }
    }

    fun onFilterApplied(minLevel: Int, platform: String?) {
        _uiState.update { it.copy(minLevel = minLevel, selectedPlatform = platform, showFilters = false) }
        applyFilters(_uiState.value.query, minLevel, platform)
    }

    fun onShowFilters() { _uiState.update { it.copy(showFilters = true) } }
    fun onDismissFilters() { _uiState.update { it.copy(showFilters = false) } }

    private fun applyFilters(query: String, minLevel: Int, platform: String?) {
        val results = _uiState.value.allGamers.filter { gamer ->
            gamer.name.contains(query, ignoreCase = true) &&
                    gamer.level >= minLevel &&
                    (platform == null || gamer.platform == platform)
        }
        _uiState.update { it.copy(filteredGamers = results, hasSearched = true) }
    }
}

class SearchViewModelFactory(private val repository: GamerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(repository) as T
    }
}
