package com.example.proyectofinal.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.proyectofinal.data.local.entities.GamerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamerDao {
    @Query("SELECT * FROM gamers")
    fun getAllGamers(): Flow<List<GamerEntity>>

    @Query("SELECT * FROM gamers WHERE id = :id")
    suspend fun getGamerById(id: String): GamerEntity?

    @Query("SELECT * FROM gamers WHERE email = :email LIMIT 1")
    suspend fun getGamerByEmail(email: String): GamerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamer(gamer: GamerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGamers(gamers: List<GamerEntity>)

    @Query("UPDATE gamers SET name = :name WHERE id = :id")
    suspend fun updateGamer(id: String, name: String)

    @Query("UPDATE gamers SET name = :name, game = :game, platform = :platform, reputationScore = :reputationScore WHERE id = :id")
    suspend fun updateGamerFull(id: String, name: String, game: String, platform: String, reputationScore: Int)

    @Query("DELETE FROM gamers")
    suspend fun deleteAllGamers()
}
