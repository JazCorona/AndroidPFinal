package com.example.proyectofinal.ui.screens.tournaments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyectofinal.data.repository.GamerRepository
import com.example.proyectofinal.ui.components.TournamentCard
import com.example.proyectofinal.ui.components.TopBarWithBack

@Composable
fun TournamentsScreen(
    repository: GamerRepository,
    onBack: () -> Unit,
    viewModel: TournamentsViewModel = viewModel(factory = TournamentsViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.inscriptionSuccess) {
        uiState.inscriptionSuccess?.let {
            snackbarHostState.showSnackbar("Inscrito en $it")
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TopBarWithBack(title = "Torneos", onBack = onBack)

            Button(
                onClick = { viewModel.onShowCreateDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("+ Crear torneo", fontWeight = FontWeight.Bold) }

            if (uiState.tournaments.isEmpty()) {
                Text("No hay torneos. Crea uno.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.tournaments) { tournament ->
                    Column {
                        TournamentCard(tournament = tournament, onJoin = { viewModel.onJoinClicked(tournament) })
                        Button(
                            onClick = { viewModel.onJoinClicked(tournament) },
                            enabled = !tournament.isFull,
                            colors = ButtonDefaults.buttonColors(containerColor = if (tournament.isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) { Text(if (tournament.isFull) "Llena" else "Inscribirse", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissCreateDialog() },
            title = { Text("Crear torneo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uiState.createName, onValueChange = { viewModel.onCreateNameChanged(it) }, label = { Text("Nombre") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(cursorColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.createGame, onValueChange = { viewModel.onCreateGameChanged(it) }, label = { Text("Juego") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(cursorColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.createDate, onValueChange = { viewModel.onCreateDateChanged(it) }, label = { Text("Fecha (ej: 2026-07-01)") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(cursorColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.createMax, onValueChange = { viewModel.onCreateMaxChanged(it) }, label = { Text("Máx. participantes") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(cursorColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.createPrize, onValueChange = { viewModel.onCreatePrizeChanged(it) }, label = { Text("Premio") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(cursorColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.createTournament() }) { Text("Crear", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { viewModel.onDismissCreateDialog() }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (uiState.showFullDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissFullDialog() },
            title = { Text("Torneo lleno") },
            text = { Text("El torneo ${uiState.selectedTournament?.name} alcanzó el cupo máximo.") },
            confirmButton = { TextButton(onClick = { viewModel.onDismissFullDialog() }) { Text("Aceptar", color = MaterialTheme.colorScheme.primary) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissConfirmDialog() },
            title = { Text("Inscribirse") },
            text = { Text("¿Inscribirte en ${uiState.selectedTournament?.name}?") },
            confirmButton = { TextButton(onClick = { viewModel.onConfirmInscription() }) { Text("Confirmar", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { viewModel.onDismissConfirmDialog() }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
