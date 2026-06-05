// Message.kt — Modelo de datos para un mensaje de chat
package com.example.proyectofinal.data.model

data class Message(
    val id: String,
    val senderName: String,
    val content: String,
    val timestamp: String,
    val isOwn: Boolean
)
