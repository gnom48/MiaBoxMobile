package com.example.pronedvizapp.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pronedvizapp.databases.models.AddressInfoOrm
import com.example.pronedvizapp.databases.models.CallRecordOrm
import com.example.pronedvizapp.databases.models.ChangesOrm
import com.example.pronedvizapp.databases.models.DayStatisticsOrm
import com.example.pronedvizapp.databases.models.ImageOrm
import com.example.pronedvizapp.databases.models.LastMonthStatisticsWithKpiOrm
import com.example.pronedvizapp.databases.models.MonthStatisticsOrm
import com.example.pronedvizapp.databases.models.NoteOrm
import com.example.pronedvizapp.databases.models.SummaryStatisticsWithLevelOrm
import com.example.pronedvizapp.databases.models.TaskOrm
import com.example.pronedvizapp.databases.models.TeamOrm
import com.example.pronedvizapp.databases.models.UserOrm
import com.example.pronedvizapp.databases.models.UserTeamOrm
import com.example.pronedvizapp.databases.models.UsersCallsOrm
import com.example.pronedvizapp.databases.models.WeekStatisticsOrm

@Database(
    entities = [
        ChangesOrm::class,
        LastMonthStatisticsWithKpiOrm::class,
        SummaryStatisticsWithLevelOrm::class,
        NoteOrm::class,
        TaskOrm::class,
        UserOrm::class,
        TeamOrm::class,
        DayStatisticsOrm::class,
        WeekStatisticsOrm::class,
        MonthStatisticsOrm::class,
        AddressInfoOrm::class,
        CallRecordOrm::class,
        UsersCallsOrm::class,
        UserTeamOrm::class,
        ImageOrm::class
               ],
    version = 3)
abstract class LocalDb: RoomDatabase() {
    abstract fun getDao(): DbDao

    companion object {
        @Volatile
        private var database: RoomDatabase? = null

        @Synchronized
        fun getDb(context: Context): LocalDb {
            if(database == null) {
                synchronized(this) {
                    database = Room.databaseBuilder(
                        context = context.applicationContext,
                        klass = LocalDb::class.java,
                        name = "local_database.db"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }

            return database as LocalDb
        }
    }
}