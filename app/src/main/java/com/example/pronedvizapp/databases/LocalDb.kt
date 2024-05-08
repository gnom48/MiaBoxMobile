package com.example.pronedvizapp.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pronedvizapp.databases.models.ActiveWorkOrm
import com.example.pronedvizapp.databases.models.NoteOrm
import com.example.pronedvizapp.databases.models.StatisticsOrm
import com.example.pronedvizapp.databases.models.TeamOrm
import com.example.pronedvizapp.databases.models.UserOrm
import com.example.pronedvizapp.databases.models.UserStatusOrm
import com.example.pronedvizapp.databases.models.UserTeamOrm

@Database(entities = [NoteOrm::class, ActiveWorkOrm::class, UserOrm::class, TeamOrm::class, StatisticsOrm::class, UserStatusOrm::class, UserTeamOrm::class], version = 1)
abstract class LocalDb: RoomDatabase() {

    abstract fun getDao(): Dao

    companion object {
        @Volatile
        private var database: RoomDatabase? = null

        @Synchronized
        fun getDb(context: Context): LocalDb {
            if(database == null) {
                synchronized(this) {
                    database = Room.databaseBuilder(
                        context.applicationContext,
                        LocalDb::class.java,
                        "LocalDb.db"
                    ).build()
                }
            }

            return database as LocalDb
        }
    }
}