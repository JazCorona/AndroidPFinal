package com.example.proyectofinal.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.proyectofinal.data.local.entities.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMatches(matches: List<MatchEntity>)

    @Query("UPDATE matches SET currentPlayers = :players WHERE id = :id")
    suspend fun updateMatchPlayers(id: String, players: Int)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatchById(id: String)
}
