package com.example.proyectofinal.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val game: String,
    val hostName: String,
    val platform: String,
    val currentPlayers: Int,
    val maxPlayers: Int
)
