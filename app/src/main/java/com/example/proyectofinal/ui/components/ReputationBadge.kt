package com.example.proyectofinal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ReputationBadge(score: Int) {
    val badgeColor = when {
        score <= 25 -> Color(0xFFCD7F32)
        score <= 50 -> Color(0xFF9E9E9E)
        score <= 75 -> Color(0xFFFFD700)
        else -> Color(0xFF00E5FF)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = badgeColor
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor
        )
    }
}
