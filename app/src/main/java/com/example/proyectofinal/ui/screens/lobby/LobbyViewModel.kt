package com.example.proyectofinal.ui.screens.lobby

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.proyectofinal.data.lan.LanDiscoveryHelper
import com.example.proyectofinal.data.lan.LobbyState
import com.example.proyectofinal.data.lan.PlayerSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LobbyUiState(
    val lobby: LobbyState = LobbyState(),
    val myName: String = "",
    val isHost: Boolean = false,
    val deviceHost: String = "",
    val devicePort: Int = 0
)

class LobbyViewModel(application: Application) : AndroidViewModel(application) {
    private val lanHelper = LanDiscoveryHelper(application)
    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    fun initLobby(gameType: String, teamSize: Int, deviceHost: String, devicePort: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("gamer_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Anónimo") ?: "Anónimo"

        val lobby = LobbyState(
            lobbyId = "lobby_${System.currentTimeMillis()}",
            gameType = gameType,
            teamSize = teamSize,
            hostName = userName,
            teamA = (0 until teamSize).map { PlayerSlot(it) },
            teamB = (0 until teamSize).map { PlayerSlot(it) },
            spectators = emptyList()
        ).let {
            it.copy(teamA = it.teamA.toMutableList().apply { set(0, PlayerSlot(0, userName, true)) })
        }

        _uiState.value = LobbyUiState(lobby = lobby, myName = userName, isHost = true, deviceHost = deviceHost, devicePort = devicePort)

        lanHelper.onRequestLobby = { lobby.toJson() }
        lanHelper.onLobbyUpdate = { json -> handleLobbyUpdate(json) }

        lanHelper.sendLobbyUpdate(
            com.example.proyectofinal.data.lan.LanDevice("peer", deviceHost, devicePort),
            lobby.toJson()
        ) { ok -> Log.d("LOBBY", "Lobby enviado: $ok") }
    }

    fun joinLobby(deviceHost: String, devicePort: Int) {
        val prefs = getApplication<Application>().getSharedPreferences("gamer_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Anónimo") ?: "Anónimo"

        _uiState.update { it.copy(myName = userName, deviceHost = deviceHost, devicePort = devicePort) }

        lanHelper.onRequestLobby = { _uiState.value.lobby.toJson() }
        lanHelper.onLobbyUpdate = { json -> handleLobbyUpdate(json) }

        lanHelper.fetchLobby(
            com.example.proyectofinal.data.lan.LanDevice("host", deviceHost, devicePort)
        ) { json ->
            if (json != null && json != "{}") {
                val lobby = LobbyState.fromJson(json)
                val updatedLobby = joinTeam(lobby, userName, "A")
                _uiState.update { it.copy(lobby = updatedLobby, isHost = false) }
                lanHelper.sendLobbyUpdate(
                    com.example.proyectofinal.data.lan.LanDevice("host", deviceHost, devicePort),
                    updatedLobby.toJson()
                ) { ok -> Log.d("LOBBY", "Join enviado: $ok") }
            }
        }
    }

    private fun joinTeam(lobby: LobbyState, playerName: String, team: String): LobbyState {
        val targetTeam = if (team == "A") lobby.teamA else lobby.teamB
        val freeSlot = targetTeam.indexOfFirst { it.playerName.isEmpty() }
        if (freeSlot == -1) return lobby

        val cleanedA = lobby.teamA.map { if (it.playerName == playerName) PlayerSlot(it.slotIndex) else it }
        val cleanedB = lobby.teamB.map { if (it.playerName == playerName) PlayerSlot(it.slotIndex) else it }

        return if (team == "A") {
            lobby.copy(teamA = cleanedA.toMutableList().apply { set(freeSlot, PlayerSlot(freeSlot, playerName)) }, teamB = cleanedB)
        } else {
            lobby.copy(teamB = cleanedB.toMutableList().apply { set(freeSlot, PlayerSlot(freeSlot, playerName)) }, teamA = cleanedA)
        }
    }

    fun moveToOtherTeam() {
        val state = _uiState.value
        val lobby = state.lobby
        val myName = state.myName
        val inTeamA = lobby.teamA.any { it.playerName == myName }
        val newLobby = if (inTeamA) joinTeam(lobby, myName, "B") else joinTeam(lobby, myName, "A")
        _uiState.update { it.copy(lobby = newLobby) }
        broadcastLobby(newLobby)
    }

    fun spectate() {
        val state = _uiState.value
        val lobby = state.lobby
        val myName = state.myName
        val cleanedA = lobby.teamA.map { if (it.playerName == myName) PlayerSlot(it.slotIndex) else it }
        val cleanedB = lobby.teamB.map { if (it.playerName == myName) PlayerSlot(it.slotIndex) else it }
        val newLobby = lobby.copy(teamA = cleanedA, teamB = cleanedB, spectators = lobby.spectators + PlayerSlot(0, myName))
        _uiState.update { it.copy(lobby = newLobby) }
        broadcastLobby(newLobby)
    }

    fun startMatch() {
        val lobby = _uiState.value.lobby.copy(status = "IN_PROGRESS")
        _uiState.update { it.copy(lobby = lobby) }
        broadcastLobby(lobby)
    }

    private fun broadcastLobby(lobby: LobbyState) {
        val state = _uiState.value
        if (state.deviceHost.isNotEmpty()) {
            lanHelper.sendLobbyUpdate(
                com.example.proyectofinal.data.lan.LanDevice("peer", state.deviceHost, state.devicePort),
                lobby.toJson()
            ) { ok -> Log.d("LOBBY", "Broadcast: $ok") }
        }
    }

    private fun handleLobbyUpdate(json: String) {
        val lobby = LobbyState.fromJson(json)
        _uiState.update { it.copy(lobby = lobby) }
    }

    override fun onCleared() { super.onCleared(); lanHelper.cleanup() }
}

class LobbyViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LobbyViewModel(application) as T
    }
}
