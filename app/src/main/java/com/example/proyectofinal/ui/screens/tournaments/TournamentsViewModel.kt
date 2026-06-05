package com.example.proyectofinal.ui.screens.tournaments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.model.Tournament
import com.example.proyectofinal.data.repository.GamerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TournamentsUiState(
    val tournaments: List<Tournament> = emptyList(),
    val showCreateDialog: Boolean = false,
    val createName: String = "",
    val createGame: String = "",
    val createDate: String = "",
    val createMax: String = "",
    val createPrize: String = "",
    val showConfirmDialog: Boolean = false,
    val showFullDialog: Boolean = false,
    val selectedTournament: Tournament? = null,
    val inscriptionSuccess: String? = null
)

class TournamentsViewModel(private val repository: GamerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TournamentsUiState())
    val uiState: StateFlow<TournamentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllTournaments().collect { t -> _uiState.update { it.copy(tournaments = t) } }
        }
    }

    fun onShowCreateDialog() { _uiState.update { it.copy(showCreateDialog = true) } }
    fun onDismissCreateDialog() { _uiState.update { it.copy(showCreateDialog = false, createName = "", createGame = "", createDate = "", createMax = "", createPrize = "") } }
    fun onCreateNameChanged(v: String) { _uiState.update { it.copy(createName = v) } }
    fun onCreateGameChanged(v: String) { _uiState.update { it.copy(createGame = v) } }
    fun onCreateDateChanged(v: String) { _uiState.update { it.copy(createDate = v) } }
    fun onCreateMaxChanged(v: String) { _uiState.update { it.copy(createMax = v) } }
    fun onCreatePrizeChanged(v: String) { _uiState.update { it.copy(createPrize = v) } }

    fun createTournament() {
        val s = _uiState.value
        if (s.createName.isBlank() || s.createGame.isBlank()) return
        val max = s.createMax.toIntOrNull() ?: 16
        viewModelScope.launch {
            repository.createTournament(s.createName.trim(), s.createGame.trim(), s.createDate.trim(), max, s.createPrize.trim())
            _uiState.update { it.copy(showCreateDialog = false, createName = "", createGame = "", createDate = "", createMax = "", createPrize = "") }
        }
    }

    fun onJoinClicked(tournament: Tournament) {
        if (tournament.isFull) _uiState.update { it.copy(showFullDialog = true, selectedTournament = tournament) }
        else _uiState.update { it.copy(showConfirmDialog = true, selectedTournament = tournament) }
    }

    fun onDismissFullDialog() { _uiState.update { it.copy(showFullDialog = false, selectedTournament = null) } }
    fun onDismissConfirmDialog() { _uiState.update { it.copy(showConfirmDialog = false, selectedTournament = null) } }

    fun onConfirmInscription() {
        val t = _uiState.value.selectedTournament ?: return
        viewModelScope.launch {
            repository.joinTournament(t.id)
            _uiState.update { it.copy(showConfirmDialog = false, selectedTournament = null, inscriptionSuccess = t.name) }
        }
    }

    fun clearSuccess() { _uiState.update { it.copy(inscriptionSuccess = null) } }
}

class TournamentsViewModelFactory(private val repository: GamerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TournamentsViewModel(repository) as T
    }
}
