package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "active_works",
    foreignKeys = [ForeignKey(
        entity = UserOrm::class,
        parentColumns = ["user_id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class ActiveWorkOrm(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "work_id")
    val workId: Long? = null,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "work_type")
    val workType: String,
    @ColumnInfo(name = "work_start")
    val date_time: Long,
    @ColumnInfo(name = "desc")
    val desc: String?,
    @ColumnInfo(name = "work_duration")
    val workDuration: Long,
    @ColumnInfo(name = "notification_id")
    val notificationId: String?
)