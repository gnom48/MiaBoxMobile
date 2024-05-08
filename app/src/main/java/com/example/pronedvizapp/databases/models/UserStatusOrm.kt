package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_statuses")
data class UserStatusOrm(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_status_id")
    val userStatusId: Long? = null,
    val name: String
)