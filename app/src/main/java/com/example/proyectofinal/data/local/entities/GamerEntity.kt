package com.example.proyectofinal.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gamers")
data class GamerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val level: Int,
    val game: String,
    val platform: String,
    val reputationScore: Int,
    val email: String = "",
    val password: String = ""
)
