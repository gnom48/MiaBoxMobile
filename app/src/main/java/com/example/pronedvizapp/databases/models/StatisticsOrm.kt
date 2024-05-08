package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "statistics",
    foreignKeys = [ForeignKey(
        entity = UserOrm::class,
        parentColumns = ["user_id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class StatisticsOrm(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val analytics: Int,
    val search: Int,
    val deals: Int,
    val meets: Int,
    val shows: Int,
    val calls: Int,
    val adverts: Int,
    val deposits: Int,
    @ColumnInfo(name = "event_date")
    val eventDate: Long
)