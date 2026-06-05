package com.example.proyectofinal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.proyectofinal.data.local.daos.GamerDao
import com.example.proyectofinal.data.local.daos.MatchDao
import com.example.proyectofinal.data.local.daos.MessageDao
import com.example.proyectofinal.data.local.daos.TournamentDao
import com.example.proyectofinal.data.local.entities.GamerEntity
import com.example.proyectofinal.data.local.entities.MatchEntity
import com.example.proyectofinal.data.local.entities.MessageEntity
import com.example.proyectofinal.data.local.entities.TournamentEntity

@Database(
    entities = [
        GamerEntity::class,
        MatchEntity::class,
        TournamentEntity::class,
        MessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gamerDao(): GamerDao
    abstract fun matchDao(): MatchDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gamer_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
