package com.example.proyectofinal.data.repository

// === Repositorio principal de datos ===
// Capa de abstracción entre la base de datos Room y los ViewModels.
// Proporciona operaciones CRUD para gamers, partidas, torneos y mensajes.

import android.content.Context
import com.example.proyectofinal.data.local.AppDatabase
import com.example.proyectofinal.data.local.entities.GamerEntity
import com.example.proyectofinal.data.local.entities.MatchEntity
import com.example.proyectofinal.data.local.entities.MessageEntity
import com.example.proyectofinal.data.local.entities.TournamentEntity
import com.example.proyectofinal.data.model.Gamer
import com.example.proyectofinal.data.model.Match
import com.example.proyectofinal.data.model.Message
import com.example.proyectofinal.data.model.Tournament
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GamerRepository(context: Context) {
    // Inicializa la base de datos y los DAOs
    private val db = AppDatabase.getDatabase(context)
    private val gamerDao = db.gamerDao()
    private val matchDao = db.matchDao()
    private val tournamentDao = db.tournamentDao()
    private val messageDao = db.messageDao()

    // === Gestión de usuarios (gamers) ===

    // Registra un nuevo usuario si el email no existe ya. Devuelve true si se creó.
    suspend fun registerUser(name: String, email: String, password: String): Boolean {
        val existing = gamerDao.getGamerByEmail(email)
        if (existing != null) return false
        // Usa el hashCode del email como ID único
        gamerDao.insertGamer(GamerEntity(email.hashCode().toString(), name, 1, "Sin juego", "PC", 50, email, password))
        return true
    }

    // Inicia sesión verificando email y contraseña. Devuelve null si no coincide.
    suspend fun loginUser(email: String, password: String): GamerEntity? {
        val user = gamerDao.getGamerByEmail(email) ?: return null
        return if (user.password == password) user else null
    }

    suspend fun getGamerByEmail(email: String): Gamer? = gamerDao.getGamerByEmail(email)?.toDomain()
    suspend fun getGamerById(id: String): Gamer? = gamerDao.getGamerById(id)?.toDomain()

    fun getAllGamers(): Flow<List<Gamer>> = gamerDao.getAllGamers().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun updateGamer(id: String, name: String, game: String, platform: String, reputationScore: Int) {
        gamerDao.updateGamerFull(id, name, game, platform, reputationScore)
    }

    // === Gestión de partidas (matches) ===

    // Obtiene todas las partidas como Flow reactivo
    fun getAllMatches(): Flow<List<Match>> = matchDao.getAllMatches().map { list ->
        list.map { Match(it.id, it.game, it.hostName, it.platform, it.currentPlayers, it.maxPlayers) }
    }

    // Crea una nueva partida en la base de datos
    suspend fun createMatch(game: String, hostName: String, platform: String, maxPlayers: Int): String {
        val id = "match_${System.currentTimeMillis()}"
        matchDao.insertMatch(MatchEntity(id, game, hostName, platform, 1, maxPlayers))
        return id
    }

    // Incrementa el contador de jugadores de una partida si no está llena
    suspend fun joinMatch(matchId: String, userName: String) {
        val match = matchDao.getMatchById(matchId) ?: return
        if (match.currentPlayers < match.maxPlayers) {
            matchDao.updateMatchPlayers(matchId, match.currentPlayers + 1)
        }
    }

    suspend fun deleteMatch(matchId: String) {
        matchDao.deleteMatchById(matchId)
    }

    // === Gestión de torneos ===

    fun getAllTournaments(): Flow<List<Tournament>> = tournamentDao.getAllTournaments().map { list ->
        list.map { Tournament(it.id, it.name, it.game, it.date, it.participants, it.maxParticipants, prize = it.prize) }
    }

    suspend fun createTournament(name: String, game: String, date: String, maxParticipants: Int, prize: String): String {
        val id = "tour_${System.currentTimeMillis()}"
        tournamentDao.insertTournament(TournamentEntity(id, name, game, date, 0, maxParticipants, prize))
        return id
    }

    suspend fun joinTournament(tournamentId: String) {
        val t = tournamentDao.getTournamentById(tournamentId) ?: return
        if (t.participants < t.maxParticipants) {
            tournamentDao.updateTournamentParticipants(tournamentId, t.participants + 1)
        }
    }

    // === Gestión de mensajes de chat ===

    fun getAllMessages(): Flow<List<Message>> = messageDao.getAllMessages().map { list ->
        list.map { Message(it.id, it.senderName, it.content, it.timestamp, it.isOwn) }
    }

    suspend fun sendMessage(message: Message) {
        messageDao.insertMessage(
            MessageEntity(message.id, message.senderName, message.content, message.timestamp, message.isOwn)
        )
    }

    // Convierte una entidad Room a modelo de dominio
    private fun GamerEntity.toDomain() = Gamer(id, name, level, game, platform, reputationScore)
}
