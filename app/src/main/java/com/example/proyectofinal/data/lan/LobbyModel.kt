package com.example.proyectofinal.data.lan

// === Modelos de datos para el lobby de partidas LAN ===
// PlayerSlot: representa un hueco en un equipo (índice, nombre del jugador, si es host)
// LobbyState: estado completo del lobby (equipos, espectadores, chat, estado de partida)

// Representa un hueco en un equipo: índice, nombre del jugador y si es el anfitrión
data class PlayerSlot(
    val slotIndex: Int,
    val playerName: String = "",
    val isHost: Boolean = false
)

// Estado completo del lobby, incluyendo equipos, espectadores, estado y mensajes de chat
data class LobbyState(
    val lobbyId: String = "",
    val gameType: String = "",
    val teamSize: Int = 5,
    val teamA: List<PlayerSlot> = emptyList(),
    val teamB: List<PlayerSlot> = emptyList(),
    val spectators: List<PlayerSlot> = emptyList(),
    val hostName: String = "",
    val status: String = "WAITING",         // WAITING | IN_PROGRESS
    val messages: List<String> = emptyList()
) {
    // Propiedades calculadas para consultar rápidamente la ocupación de cada equipo
    val teamACount get() = teamA.count { it.playerName.isNotEmpty() }
    val teamBCount get() = teamB.count { it.playerName.isNotEmpty() }
    val spectatorCount get() = spectators.size
    val isFull get() = teamACount >= teamSize && teamBCount >= teamSize

    // Serializa el lobby a JSON para transmitirlo por la red
    fun toJson(): String {
        val ta = teamA.joinToString(",") { "{\"slot\":${it.slotIndex},\"name\":\"${it.playerName}\",\"host\":${it.isHost}}" }
        val tb = teamB.joinToString(",") { "{\"slot\":${it.slotIndex},\"name\":\"${it.playerName}\",\"host\":${it.isHost}}" }
        val sp = spectators.joinToString(",") { "{\"name\":\"${it.playerName}\"}" }
        val msgs = messages.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
        return """{"id":"$lobbyId","game":"$gameType","teamSize":$teamSize,"host":"$hostName","status":"$status","teamA":[$ta],"teamB":[$tb],"spectators":[$sp],"messages":[$msgs]}"""
    }

    companion object {
        // Deserializa un JSON a un objeto LobbyState (usado al recibir datos del otro dispositivo)
        fun fromJson(json: String): LobbyState {
            try {
                val obj = org.json.JSONObject(json)
                val teamSize = obj.getInt("teamSize")
                val ta = obj.getJSONArray("teamA").let { arr ->
                    (0 until arr.length()).map { i ->
                        val s = arr.getJSONObject(i)
                        PlayerSlot(s.getInt("slot"), s.getString("name"), s.optBoolean("host", false))
                    }
                }
                val tb = obj.getJSONArray("teamB").let { arr ->
                    (0 until arr.length()).map { i ->
                        val s = arr.getJSONObject(i)
                        PlayerSlot(s.getInt("slot"), s.getString("name"), s.optBoolean("host", false))
                    }
                }
                val sp = obj.getJSONArray("spectators").let { arr ->
                    (0 until arr.length()).map { i ->
                        val s = arr.getJSONObject(i)
                        PlayerSlot(0, s.getString("name"))
                    }
                }
                val msgs = obj.optJSONArray("messages")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                return LobbyState(
                    lobbyId = obj.getString("id"),
                    gameType = obj.getString("game"),
                    teamSize = teamSize,
                    teamA = ta,
                    teamB = tb,
                    spectators = sp,
                    hostName = obj.getString("host"),
                    status = obj.getString("status"),
                    messages = msgs
                )
            } catch (e: Exception) {
                // Si el JSON es inválido, devuelve un lobby vacío por defecto
                return LobbyState()
            }
        }
    }
}
