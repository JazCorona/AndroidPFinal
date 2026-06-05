package com.example.proyectofinal.ui.screens.chat

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.model.Message
import com.example.proyectofinal.data.repository.GamerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false
)

class ChatViewModel(private val repository: GamerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val botReplies = listOf(
        "¡Genial! Vamos a ganar esta.",
        "Estoy en el lobby, ¿vienes?",
        "Buena jugada!",
        "Espera, estoy en otra partida.",
        "¿Ranked o normal?",
        "Jugamos otra despues?",
        "GG WP!"
    )

    init {
        viewModelScope.launch {
            repository.getAllMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendClicked(): Boolean {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return false

        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val message = Message(
            id = "msg_${System.currentTimeMillis()}",
            senderName = "Tú",
            content = text,
            timestamp = timestamp,
            isOwn = true
        )

        viewModelScope.launch {
            repository.sendMessage(message)
            _uiState.update { it.copy(inputText = "") }

            _uiState.update { it.copy(isTyping = true) }
            delay(1500)
            _uiState.update { it.copy(isTyping = false) }

            val reply = botReplies.random()
            repository.sendMessage(
                Message(
                    id = "msg_${System.currentTimeMillis()}",
                    senderName = "Alex Gamer",
                    content = reply,
                    timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                    isOwn = false
                )
            )
        }
        return true
    }
}

class ChatViewModelFactory(private val repository: GamerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(repository) as T
    }
}
