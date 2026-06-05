package com.example.proyectofinal.ui.screens.matchmaking

// === Pantalla de Matchmaking (emparejamiento LAN) ===
// Muestra los dispositivos encontrados en la red local, las partidas locales del usuario,
// y permite conectarse a un peer o crear un lobby propio.

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.proyectofinal.navigation.Routes
import com.example.proyectofinal.ui.components.TopBarWithBack

@Composable
fun MatchmakingScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: MatchmakingViewModel = viewModel(factory = MatchmakingViewModelFactory(
        LocalContext.current.applicationContext as Application
    ))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navega automáticamente al lobby cuando el usuario se une o crea uno
    LaunchedEffect(uiState.inLobby) {
        if (uiState.inLobby) {
            navController.navigate(Routes.Lobby.route)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TopBarWithBack(title = "Matchmaking LAN", onBack = onBack)

            // Pantalla de espera: cuando el cliente se conecta pero el host aún no ha creado el lobby
            if (uiState.connectedDevice != null && !uiState.inLobby) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Conectado a:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(uiState.connectedDevice!!.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Esperando que el host cree un lobby...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.disconnect() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Desconectar") }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.rescan() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Escanear", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { viewModel.onShowCreateDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) { Text("Crear lobby", fontWeight = FontWeight.Bold) }
                }

                // Lista de partidas locales guardadas en la base de datos Room
                if (uiState.localMatches.isNotEmpty()) {
                    Text("Tus partidas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    uiState.localMatches.forEach { m ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.game, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${m.currentPlayers}/${m.maxPlayers} · ${m.platform}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                // Botón para borrar la partida de la lista local
                                IconButton(onClick = { viewModel.deleteMatch(m.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Text("Dispositivos en la red", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                if (uiState.statusMessage.isNotEmpty()) {
                    Text(uiState.statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                }

                // Indicador de escaneo mientras se buscan dispositivos
                if (uiState.isScanning && uiState.devices.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Escaneando...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!uiState.isScanning && uiState.devices.isEmpty()) {
                    Text("No se encontraron dispositivos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }

                // Lista de dispositivos encontrados en la LAN con botón "Conectar"
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.devices) { device ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${device.host}:${device.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(onClick = { viewModel.onConnectClicked(device) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                    Text("Conectar", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para crear un nuevo lobby con selección de juego
    if (uiState.showCreateDialog) {
        val gameOptions = listOf(
            Triple("Super Smash Bros", 1, "Consola"),
            Triple("Rocket League", 3, "PC"),
            Triple("Valorant", 5, "PC")
        )
        AlertDialog(
            onDismissRequest = { viewModel.onDismissCreateDialog() },
            title = { Text("Crear lobby") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selecciona el juego:", style = MaterialTheme.typography.bodyMedium)
                    gameOptions.forEach { (name, size, platform) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.onGameTypeSelected(name, size, platform) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.selectedGameName == name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(name, fontWeight = FontWeight.Bold)
                                Text("${size}v${size} · $platform", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.createMatch() }) { Text("Crear", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { viewModel.onDismissCreateDialog() }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Diálogo de confirmación para conectarse a un dispositivo
    if (uiState.showConnectDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDialog() },
            title = { Text("Conectar") },
            text = { Text("¿Conectarte a ${uiState.selectedDevice?.name}?") },
            confirmButton = { TextButton(onClick = { viewModel.onConfirmConnect() }) { Text("Conectar", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { viewModel.onDismissDialog() }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
