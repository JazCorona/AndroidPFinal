package com.example.proyectofinal.ui.screens.matchmaking

// === ViewModel de la pantalla de Matchmaking (emparejamiento LAN) ===
// Orquesta el descubrimiento de dispositivos en la red local, la creación/unión a lobbies
// y la sincronización del estado del lobby entre dispositivos mediante el helper LanDiscoveryHelper.

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.lan.LanDevice
import com.example.proyectofinal.data.lan.LanDiscoveryHelper
import com.example.proyectofinal.data.lan.LobbyState
import com.example.proyectofinal.data.lan.PlayerSlot
import com.example.proyectofinal.data.model.Match
import com.example.proyectofinal.data.repository.GamerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// Estado completo de la UI de matchmaking, observable desde la pantalla
data class MatchmakingUiState(
    val isScanning: Boolean = true,
    val devices: List<LanDevice> = emptyList(),
    val statusMessage: String = "",
    val showConnectDialog: Boolean = false,
    val selectedDevice: LanDevice? = null,
    val connectedDevice: LanDevice? = null,
    val showCreateDialog: Boolean = false,
    val selectedGameName: String = "Valorant",
    val selectedTeamSize: Int = 5,
    val selectedPlatform: String = "PC",
    val localMatches: List<Match> = emptyList(),
    val inLobby: Boolean = false,
    val lobby: LobbyState = LobbyState(),
    val isHost: Boolean = false,
    val hostDeviceIp: String = "",
    val hostDevicePort: Int = 0,
    val myName: String = ""
)

class MatchmakingViewModel(application: Application) : AndroidViewModel(application) {
    private val lanHelper = LanDiscoveryHelper(application)
    private val repository = GamerRepository(application)
    private val _uiState = MutableStateFlow(MatchmakingUiState())
    val uiState: StateFlow<MatchmakingUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null  // Trabajo periódico para sondear el lobby remoto

    init {
        // Recupera el nombre de usuario guardado en SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences("gamer_prefs", Context.MODE_PRIVATE)
        val loginName = prefs.getString("user_name", "Anónimo") ?: "Anónimo"
        val displayName = prefs.getString("display_name", loginName) ?: loginName

        _uiState.update { it.copy(myName = displayName) }

        // Configura los callbacks que LanDiscoveryHelper usará para responder a peers
        lanHelper.onRequestData = { getLocalMatchesJson() }
        lanHelper.onRequestUser = { displayName }
        lanHelper.onRequestLobby = { _uiState.value.lobby.toJson() }
        lanHelper.onLobbyUpdate = { json -> handleLobbyUpdate(json) }
        lanHelper.onDataReceived = { data -> receiveRemoteMatch(data) }
        lanHelper.onChatReceived = { from, text ->
            val msg = "$from: $text"
            _uiState.update { it.copy(lobby = it.lobby.copy(messages = it.lobby.messages + msg)) }
        }

        // Publica este dispositivo en la red y empieza a buscar otros
        lanHelper.registerService(displayName)
        startDiscovery()

        // Observa las partidas locales desde la base de datos Room
        viewModelScope.launch {
            repository.getAllMatches().collect { matches ->
                _uiState.update { it.copy(localMatches = matches) }
            }
        }
    }

    // Inicia el descubrimiento de dispositivos y suscribe los StateFlows del helper a la UI
    private fun startDiscovery() {
        _uiState.update { it.copy(isScanning = true, devices = emptyList()) }
        viewModelScope.launch { lanHelper.devices.collect { d -> _uiState.update { it.copy(devices = d) } } }
        viewModelScope.launch { lanHelper.isScanning.collect { s -> _uiState.update { it.copy(isScanning = s) } } }
        viewModelScope.launch { lanHelper.statusMessage.collect { m -> _uiState.update { it.copy(statusMessage = m) } } }
        lanHelper.startDiscovery()
    }

    // Reinicia el escaneo de la red local
    fun rescan() { lanHelper.stopDiscovery(); startDiscovery() }

    // Muestra el diálogo de confirmación de conexión a un dispositivo
    fun onConnectClicked(device: LanDevice) { _uiState.update { it.copy(showConnectDialog = true, selectedDevice = device) } }
    fun onDismissDialog() { _uiState.update { it.copy(showConnectDialog = false, selectedDevice = null) } }

    // Confirma la conexión: obtiene el lobby remoto del dispositivo seleccionado
    fun onConfirmConnect() {
        val device = _uiState.value.selectedDevice ?: return
        _uiState.update { it.copy(showConnectDialog = false, selectedDevice = null) }
        lanHelper.stopDiscovery()

        lanHelper.fetchLobby(device) { json ->
            val s = _uiState.value
            if (json != null && json.isNotBlank() && json.contains("game")) {
                val lobby = LobbyState.fromJson(json)
                _uiState.update {
                    it.copy(
                        connectedDevice = device.copy(username = device.name),
                        inLobby = true,
                        isHost = false,
                        hostDeviceIp = device.host,
                        hostDevicePort = device.port,
                        lobby = lobby
                    )
                }
            }
            // Empieza a sondear el lobby remoto cada 2 segundos para mantenerlo sincronizado
            startLobbyPolling(device)
        }
    }

    // Sondear periódicamente (cada 2s) el estado del lobby del host remoto
    private fun startLobbyPolling(device: LanDevice) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(2000)
                lanHelper.fetchLobby(device) { json ->
                    if (json != null && json.isNotBlank() && json.contains("game")) {
                        handleLobbyUpdate(json)
                    }
                }
            }
        }
    }

    private fun stopLobbyPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // Desconecta del lobby remoto y vuelve al escaneo
    fun disconnect() {
        stopLobbyPolling()
        _uiState.update {
            it.copy(
                connectedDevice = null,
                inLobby = false,
                lobby = LobbyState(),
                isHost = false,
                hostDeviceIp = "",
                hostDevicePort = 0
            )
        }
        startDiscovery()
    }

    fun onShowCreateDialog() { _uiState.update { it.copy(showCreateDialog = true) } }
    fun onDismissCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }
    fun onGameTypeSelected(name: String, teamSize: Int, platform: String) {
        _uiState.update { it.copy(selectedGameName = name, selectedTeamSize = teamSize, selectedPlatform = platform) }
    }

    // Elimina una partida de la base de datos local
    fun deleteMatch(matchId: String) {
        viewModelScope.launch { repository.deleteMatch(matchId) }
    }

    // Crea un nuevo lobby (como host) con el juego y tamaño de equipo seleccionados
    fun createMatch() {
        val s = _uiState.value
        val userName = s.myName
        // Inicializa el lobby: el host ocupa el primer slot del equipo A
        val lobby = LobbyState(
            lobbyId = "lobby_${System.currentTimeMillis()}",
            gameType = s.selectedGameName,
            teamSize = s.selectedTeamSize,
            hostName = userName,
            teamA = (0 until s.selectedTeamSize).map { PlayerSlot(it) }.toMutableList().apply {
                set(0, PlayerSlot(0, userName, true))
            },
            teamB = (0 until s.selectedTeamSize).map { PlayerSlot(it) },
            spectators = emptyList()
        )

        _uiState.update {
            it.copy(
                showCreateDialog = false,
                inLobby = true,
                isHost = true,
                lobby = lobby,
                hostDeviceIp = "",
                hostDevicePort = 0
            )
        }

        viewModelScope.launch {
            repository.createMatch(s.selectedGameName, userName, s.selectedPlatform, s.selectedTeamSize)
        }

        // Si ya estaba conectado a un peer, le envía el lobby creado
        val device = _uiState.value.connectedDevice
        if (device != null) broadcastLobby(device)
    }

    // === Funciones del lobby ===

    // Mueve al jugador al equipo contrario
    fun moveToOtherTeam() {
        val state = _uiState.value
        val myName = state.myName
        val lobby = state.lobby
        val inTeamA = lobby.teamA.any { it.playerName == myName }
        val newLobby = if (inTeamA) joinTeam(lobby, myName, "B") else joinTeam(lobby, myName, "A")
        _uiState.update { it.copy(lobby = newLobby) }
        broadcastIfConnected(newLobby)
    }

    // Mueve al jugador a la lista de espectadores
    fun spectate() {
        val state = _uiState.value
        val myName = state.myName
        val lobby = state.lobby
        val cleanedA = lobby.teamA.map { if (it.playerName == myName) PlayerSlot(it.slotIndex) else it }
        val cleanedB = lobby.teamB.map { if (it.playerName == myName) PlayerSlot(it.slotIndex) else it }
        val newLobby = lobby.copy(teamA = cleanedA, teamB = cleanedB, spectators = lobby.spectators + PlayerSlot(0, myName))
        _uiState.update { it.copy(lobby = newLobby) }
        broadcastIfConnected(newLobby)
    }

    // Cambia el estado del lobby a "IN_PROGRESS" (solo el host)
    fun startMatch() {
        val lobby = _uiState.value.lobby.copy(status = "IN_PROGRESS")
        _uiState.update { it.copy(lobby = lobby) }
        broadcastIfConnected(lobby)
    }

    // Vuelve el estado del lobby a "WAITING" (solo el host)
    fun finishMatch() {
        val lobby = _uiState.value.lobby.copy(status = "WAITING")
        _uiState.update { it.copy(lobby = lobby) }
        broadcastIfConnected(lobby)
    }

    // Asigna al jugador al primer hueco libre del equipo indicado (A o B)
    private fun joinTeam(lobby: LobbyState, playerName: String, team: String): LobbyState {
        val targetTeam = if (team == "A") lobby.teamA else lobby.teamB
        val freeSlot = targetTeam.indexOfFirst { it.playerName.isEmpty() }
        // Si no hay hueco libre, devuelve el lobby sin cambios
        if (freeSlot == -1) return lobby
        // Limpia al jugador del equipo donde estuviera y lo coloca en el nuevo
        val cleanedA = lobby.teamA.map { if (it.playerName == playerName) PlayerSlot(it.slotIndex) else it }
        val cleanedB = lobby.teamB.map { if (it.playerName == playerName) PlayerSlot(it.slotIndex) else it }
        return if (team == "A") {
            lobby.copy(teamA = cleanedA.toMutableList().apply { set(freeSlot, PlayerSlot(freeSlot, playerName)) }, teamB = cleanedB)
        } else {
            lobby.copy(teamB = cleanedB.toMutableList().apply { set(freeSlot, PlayerSlot(freeSlot, playerName)) }, teamA = cleanedA)
        }
    }

    // Envía el lobby al host remoto solo si estamos conectados a uno
    private fun broadcastIfConnected(lobby: LobbyState) {
        val state = _uiState.value
        if (state.hostDeviceIp.isNotEmpty()) {
            broadcastLobby(LanDevice("peer", state.hostDeviceIp, state.hostDevicePort))
        }
    }

    // Envía el estado actual del lobby al dispositivo indicado
    private fun broadcastLobby(device: LanDevice) {
        lanHelper.sendLobbyUpdate(device, _uiState.value.lobby.toJson()) { ok -> Log.d("LOBBY", "Broadcast: $ok") }
    }

    // Procesa una actualización de lobby recibida del host remoto
    private fun handleLobbyUpdate(json: String) {
        try {
            val lobby = LobbyState.fromJson(json)
            _uiState.update { it.copy(lobby = lobby, inLobby = true) }
        } catch (e: Exception) { Log.e("LOBBY", "Parse error: ${e.message}") }
    }

    // Procesa una partida remota recibida (actualmente trata el JSON como lobby)
    private fun receiveRemoteMatch(json: String) {
        try {
            val obj = JSONObject(json)
            val lobby = LobbyState.fromJson(json)
            _uiState.update { it.copy(inLobby = true, lobby = lobby) }
        } catch (e: Exception) { Log.e("LOBBY", "receiveRemoteMatch error: ${e.message}") }
    }

    // Convierte las partidas locales a JSON para servirlas a otros dispositivos via LAN
    private fun getLocalMatchesJson(): String {
        val arr = JSONArray()
        _uiState.value.localMatches.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("game", m.game); put("host", m.hostName)
                put("platform", m.platform); put("current", m.currentPlayers); put("max", m.maxPlayers)
            })
        }
        return arr.toString()
    }

    // Sale del lobby: si no es host, avisa al host remoto; luego vuelve al escaneo
    fun leaveLobby() {
        val state = _uiState.value
        if (!state.isHost && state.hostDeviceIp.isNotEmpty()) {
            val myName = state.myName
            val lobby = state.lobby
            val cleanedA = lobby.teamA.map { if (it.playerName == myName) PlayerSlot(it.slotIndex) else it }
            val cleanedB = lobby.teamB.map { if (it.playerName == myName) PlayerSlot(it.slotIndex) else it }
            val updated = lobby.copy(teamA = cleanedA, teamB = cleanedB,
                spectators = lobby.spectators.filter { it.playerName != myName })
            _uiState.update { it.copy(lobby = updated) }
            lanHelper.sendLobbyUpdate(
                LanDevice("peer", state.hostDeviceIp, state.hostDevicePort),
                updated.toJson()
            ) { ok -> Log.d("LOBBY", "Leave broadcast: $ok") }
        }
        stopLobbyPolling()
        _uiState.update {
            it.copy(
                inLobby = false, isHost = false, lobby = LobbyState(),
                connectedDevice = null, hostDeviceIp = "", hostDevicePort = 0
            )
        }
        startDiscovery()
    }

    // Envía un mensaje de chat localmente y también al host remoto si está conectado
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val state = _uiState.value
        val msg = "${state.myName}: $text"
        _uiState.update { it.copy(lobby = it.lobby.copy(messages = it.lobby.messages + msg)) }

        if (state.hostDeviceIp.isNotEmpty()) {
            lanHelper.sendChat(LanDevice("peer", state.hostDeviceIp, state.hostDevicePort), state.myName, text) { ok ->
                Log.d("CHAT", "Enviado: $ok")
            }
        }
    }

    // Limpieza al destruir el ViewModel
    override fun onCleared() { super.onCleared(); stopLobbyPolling(); lanHelper.cleanup() }
}

// Factory para crear el ViewModel con el Application requerido
class MatchmakingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MatchmakingViewModel(application) as T
    }
}
