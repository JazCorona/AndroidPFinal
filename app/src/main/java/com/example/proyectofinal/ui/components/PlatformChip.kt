package com.example.proyectofinal.ui.components

import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlatformChip(platform: String) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = platform,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = Modifier.wrapContentWidth()
    )
}
