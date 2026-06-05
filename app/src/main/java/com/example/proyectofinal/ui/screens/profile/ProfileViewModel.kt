package com.example.proyectofinal.ui.screens.profile

// === ViewModel de la pantalla de Perfil de usuario ===
// Carga y guarda los datos del perfil (nombre, juego favorito, plataforma)
// tanto en Room como en SharedPreferences (para usarlo en el lobby y NSD).

import android.content.Context
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

// Estado observable de la UI del perfil
data class ProfileUiState(
    val gamer: Gamer? = null,
    val editableName: String = "",        // Nombre editable en el campo de texto
    val editableGame: String = "",         // Juego favorito editable
    val editablePlatform: String = "PC",   // Plataforma seleccionada
    val nameError: String? = null,         // Mensaje de error de validación del nombre
    val isSaving: Boolean = false,         // Indicador de guardado en curso
    val saveSuccess: Boolean = false       // True después de guardar exitosamente
)

class ProfileViewModel(private val repository: GamerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Carga el perfil desde la base de datos usando el email guardado en SharedPreferences
    fun loadProfile(context: Context) {
        val prefs = context.getSharedPreferences("gamer_prefs", Context.MODE_PRIVATE)
        val email = prefs.getString("user_email", "") ?: ""
        val displayName = prefs.getString("display_name", "") ?: ""
        viewModelScope.launch {
            val gamer = repository.getGamerByEmail(email)
            if (gamer != null) {
                _uiState.value = ProfileUiState(
                    gamer = gamer,
                    editableName = if (displayName.isNotEmpty()) displayName else gamer.name,
                    editableGame = gamer.game,
                    editablePlatform = gamer.platform
                )
            }
        }
    }

    // Actualiza los campos editables en el estado (limpia errores previos)
    fun onNameChanged(v: String) { _uiState.update { it.copy(editableName = v, nameError = null, saveSuccess = false) } }
    fun onGameChanged(v: String) { _uiState.update { it.copy(editableGame = v, saveSuccess = false) } }
    fun onPlatformChanged(v: String) { _uiState.update { it.copy(editablePlatform = v, saveSuccess = false) } }

    // Guarda el perfil: valida el nombre, actualiza SharedPreferences y Room
    fun saveProfile(context: Context) {
        val s = _uiState.value
        if (s.editableName.trim().length < 3) {
            _uiState.update { it.copy(nameError = "Mínimo 3 caracteres") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Guarda el nombre visible en SharedPreferences (usado por el lobby y NSD)
            context.getSharedPreferences("gamer_prefs", Context.MODE_PRIVATE)
                .edit().putString("display_name", s.editableName.trim()).apply()

            // También persiste los cambios en la base de datos Room
            s.gamer?.let {
                repository.updateGamer(it.id, s.editableName.trim(), s.editableGame, s.editablePlatform, it.reputationScore)
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveSuccess = true,
                    gamer = it.gamer?.copy(name = it.editableName.trim(), game = it.editableGame, platform = it.editablePlatform)
                )
            }
        }
    }
}

// Factory para inyectar el repositorio en el ViewModel
class ProfileViewModelFactory(private val repository: GamerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(repository) as T
    }
}
