// MessageEntity.kt — Entidad Room para almacenar mensajes de chat
package com.example.proyectofinal.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderName: String,
    val content: String,
    val timestamp: String,
    val isOwn: Boolean
)
