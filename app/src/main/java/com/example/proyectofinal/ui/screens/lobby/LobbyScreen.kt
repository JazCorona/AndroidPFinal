package com.example.proyectofinal.ui.screens.lobby

// === Pantalla del Lobby de partida LAN ===
// Muestra los dos equipos, los espectadores, el estado de la partida y el chat.
// Tiene dos pestañas: "Match" (vista del equipo) y "Chat" (mensajería entre jugadores).
// Reutiliza el MatchmakingViewModel para compartir el estado del lobby.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.proyectofinal.data.lan.PlayerSlot
import com.example.proyectofinal.navigation.Routes
import com.example.proyectofinal.ui.components.TopBarWithBack
import com.example.proyectofinal.ui.screens.matchmaking.MatchmakingViewModel

@Composable
fun LobbyScreen(
    navController: NavController,
    onBack: () -> Unit,
    // Obtiene el mismo ViewModel que la pantalla de Matchmaking (comparten el estado)
    viewModel: MatchmakingViewModel = viewModel(
        viewModelStoreOwner = navController.getBackStackEntry(Routes.Matchmaking.route)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lobby = uiState.lobby
    val gameName = lobby.gameType.ifEmpty { "Juego" }
    var tab by remember { mutableIntStateOf(0) }  // 0 = Match, 1 = Chat

    // Intercepta el botón de retroceso para salir del lobby correctamente
    BackHandler {
        viewModel.leaveLobby()
        onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopBarWithBack(title = "$gameName — Lobby", onBack = { viewModel.leaveLobby(); onBack() }) },
        // Barra inferior con dos pestañas: Match y Chat
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f).clickable { tab = 0 }.padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp),
                                tint = if (tab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Match", style = MaterialTheme.typography.labelSmall,
                                color = if (tab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).clickable { tab = 1 }.padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp),
                                tint = if (tab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Chat", style = MaterialTheme.typography.labelSmall,
                                color = if (tab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (tab == 0) {
            MatchTab(uiState, viewModel, padding)
        } else {
            ChatTab(lobby.messages, viewModel, padding)
        }
    }
}

// Pestaña de la partida: muestra datos del juego, equipos A/B, espectadores y botones de acción
@Composable
private fun MatchTab(uiState: com.example.proyectofinal.ui.screens.matchmaking.MatchmakingUiState, viewModel: MatchmakingViewModel, padding: androidx.compose.foundation.layout.PaddingValues) {
    val lobby = uiState.lobby
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(lobby.gameType.ifEmpty { "Juego" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${lobby.teamSize}v${lobby.teamSize} · ${lobby.status}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (lobby.status == "IN_PROGRESS") {
                    Text("EN JUEGO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Equipo A (${lobby.teamACount}/${lobby.teamSize})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                lobby.teamA.forEach { slot -> SlotCard(slot, myName = uiState.myName) }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Equipo B (${lobby.teamBCount}/${lobby.teamSize})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                lobby.teamB.forEach { slot -> SlotCard(slot, myName = uiState.myName) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Espectadores (${lobby.spectatorCount})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (lobby.spectators.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                lobby.spectators.forEach { spec ->
                    Text("${spec.playerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text("Ninguno", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { viewModel.moveToOtherTeam() }, modifier = Modifier.weight(1f)) { Text("Cambiar equipo") }
            OutlinedButton(onClick = { viewModel.spectate() }, modifier = Modifier.weight(1f)) { Text("Espectar") }
        }

        if (uiState.isHost) {
            if (lobby.status == "IN_PROGRESS") {
                Button(
                    onClick = { viewModel.finishMatch() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp)
                ) { Text("Finalizar partida", fontWeight = FontWeight.Bold) }
            } else {
                Button(
                    onClick = { viewModel.startMatch() },
                    enabled = lobby.status == "WAITING",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp)
                ) { Text("Iniciar partida", fontWeight = FontWeight.Bold) }
            }
        } else {
            val text = when (lobby.status) {
                "IN_PROGRESS" -> "Partida en curso..."
                else -> "Esperando que el host inicie..."
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

// Pestaña de chat: lista de mensajes con entrada de texto y botón de envío
@Composable
private fun ChatTab(messages: List<String>, viewModel: MatchmakingViewModel, padding: androidx.compose.foundation.layout.PaddingValues) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Desplaza automáticamente al último mensaje cuando llega uno nuevo
    val lastIndex = messages.lastIndex
    LaunchedEffect(lastIndex) {
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (messages.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Sin mensajes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Escribe un mensaje...") },
                singleLine = true,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { if (text.isNotBlank()) { viewModel.sendChatMessage(text.trim()); text = "" } }
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Colores predefinidos para los avatares de los jugadores (basados en el nombre)
private val avatarColors = listOf(
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF009688),
    Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548)
)

// Asigna un color de avatar consistente basado en el hash del nombre del jugador
private fun avatarColorFor(name: String): Color =
    avatarColors[ kotlin.math.abs(name.hashCode()) % avatarColors.size ]

// Tarjeta individual de un slot del equipo: muestra inicial, nombre y color de fondo
@Composable
fun SlotCard(slot: PlayerSlot, myName: String) {
    val isOccupied = slot.playerName.isNotEmpty()
    val isMe = slot.playerName == myName
    val bgColor = if (isOccupied) avatarColorFor(slot.playerName) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isOccupied) {
                    Text(text = slot.playerName.first().uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOccupied) slot.playerName else "Vacío",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOccupied) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
