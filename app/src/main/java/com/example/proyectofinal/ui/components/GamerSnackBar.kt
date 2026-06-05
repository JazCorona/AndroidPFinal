// GamerSnackBar.kt — SnackbarHostState envuelto en composable con color de error/éxito
package com.example.proyectofinal.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/** SnackbarHost reutilizable con colores de error/éxito del tema */
@Composable
fun GamerSnackBar(
    hostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    SnackbarHost(
        hostState = hostState,
        snackbar = { snackbarData ->
            val isError = snackbarData.visuals.actionLabel == "error"
            Snackbar(
                snackbarData = snackbarData,
                containerColor = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onError
            )
        }
    )
}
