// Tournament.kt — Modelo de datos para un torneo
package com.example.proyectofinal.data.model

data class Tournament(
    val id: String,
    val name: String,
    val game: String,
    val date: String,
    val participants: Int,
    val maxParticipants: Int,
    val isFull: Boolean = participants >= maxParticipants,
    val prize: String
)
