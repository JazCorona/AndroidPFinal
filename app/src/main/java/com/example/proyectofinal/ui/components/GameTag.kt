// GameTag.kt — SuggestionChip que muestra el nombre de un juego
package com.example.proyectofinal.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** SuggestionChip con el nombre del juego */
@Composable
fun GameTag(name: String) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    )
}
