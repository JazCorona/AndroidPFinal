package com.example.proyectofinal.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.model.Gamer
import com.example.proyectofinal.data.model.Match
import com.example.proyectofinal.data.repository.GamerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentUser: Gamer? = null,
    val matches: List<Match> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(private val repository: GamerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadData(context: Context) {
        val prefs = context.getSharedPreferences("gamer_prefs", Context.MODE_PRIVATE)
        val email = prefs.getString("user_email", "") ?: ""

        viewModelScope.launch {
            val user = repository.getGamerByEmail(email)
            _uiState.update { it.copy(currentUser = user, isLoading = false) }
        }
        viewModelScope.launch {
            repository.getAllMatches().collect { matches ->
                _uiState.update { it.copy(matches = matches) }
            }
        }
    }

    fun joinMatch(matchId: String) {
        viewModelScope.launch {
            val userName = _uiState.value.currentUser?.name ?: "Anónimo"
            repository.joinMatch(matchId, userName)
        }
    }
}

class HomeViewModelFactory(private val repository: GamerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
