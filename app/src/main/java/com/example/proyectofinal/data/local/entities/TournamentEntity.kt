// TournamentEntity.kt — Entidad Room para almacenar datos de un torneo
package com.example.proyectofinal.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val game: String,
    val date: String,
    val participants: Int,
    val maxParticipants: Int,
    val prize: String
)
