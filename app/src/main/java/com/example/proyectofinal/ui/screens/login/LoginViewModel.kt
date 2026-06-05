package com.example.proyectofinal.ui.screens.login

// === ViewModel de la pantalla de Login/Registro ===
// Maneja la autenticación de usuarios: inicio de sesión y registro,
// con validación de campos (nombre, email, contraseña) y persistencia
// de la sesión en SharedPreferences.

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.repository.GamerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado observable de la UI de login/registro
data class LoginUiState(
    val isRegisterMode: Boolean = false,  // Alterna entre login y registro
    val name: String = "",                 // Nombre (solo en modo registro)
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,     // Error genérico (ej. credenciales inválidas)
    val loginSuccess: Boolean = false     // True cuando la autenticación es exitosa
)

class LoginViewModel(private val repository: GamerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Cambia entre modo inicio de sesión y modo registro, limpiando errores
    fun toggleMode() {
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, nameError = null, emailError = null, passwordError = null, generalError = null) }
    }

    // Actualiza los campos del formulario y limpia errores específicos
    fun onNameChanged(v: String) { _uiState.update { it.copy(name = v, nameError = null) } }
    fun onEmailChanged(v: String) { _uiState.update { it.copy(email = v, emailError = null, generalError = null) } }
    fun onPasswordChanged(v: String) { _uiState.update { it.copy(password = v, passwordError = null) } }

    // Envía el formulario: valida campos, luego registra o inicia sesión según el modo
    fun onSubmit(context: Context) {
        val s = _uiState.value
        var err = false

        // Validaciones: nombre mínimo 3 caracteres (solo registro), email válido, password mínimo 6
        if (s.isRegisterMode && s.name.trim().length < 3) {
            _uiState.update { it.copy(nameError = "Mínimo 3 caracteres") }
            err = true
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(s.email).matches()) {
            _uiState.update { it.copy(emailError = "Email inválido") }
            err = true
        }
        if (s.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Mínimo 6 caracteres") }
            err = true
        }
        if (err) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            if (s.isRegisterMode) {
                val ok = repository.registerUser(s.name.trim(), s.email.trim(), s.password)
                if (!ok) {
                    _uiState.update { it.copy(isLoading = false, generalError = "Email ya registrado") }
                } else {
                    saveSession(context, s.email.trim(), s.name.trim())
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
            } else {
                val user = repository.loginUser(s.email.trim(), s.password)
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, generalError = "Email o contraseña incorrectos") }
                } else {
                    saveSession(context, s.email.trim(), user.name)
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
            }
        }
    }

    // Guarda la sesión del usuario en SharedPreferences para persistencia entre reinicios
    private fun saveSession(context: Context, email: String, name: String) {
        context.getSharedPreferences("gamer_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("display_name", name)
            .apply()
    }
}

// Factory para inyectar el repositorio en el ViewModel
class LoginViewModelFactory(private val repository: GamerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LoginViewModel(repository) as T
    }
}
