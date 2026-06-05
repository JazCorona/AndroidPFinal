package com.example.proyectofinal.data.model

data class Match(
    val id: String,
    val game: String,
    val hostName: String,
    val platform: String,
    val currentPlayers: Int,
    val maxPlayers: Int
)
