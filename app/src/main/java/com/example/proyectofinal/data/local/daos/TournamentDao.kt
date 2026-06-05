package com.example.proyectofinal.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.proyectofinal.data.local.entities.TournamentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: String): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTournaments(tournaments: List<TournamentEntity>)

    @Query("UPDATE tournaments SET participants = :participants WHERE id = :id")
    suspend fun updateTournamentParticipants(id: String, participants: Int)

    @Query("DELETE FROM tournaments")
    suspend fun deleteAllTournaments()
}
