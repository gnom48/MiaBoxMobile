package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamOrm(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "team_id")
    val teamId: Long? = null,
    val name: String,
    @ColumnInfo(name = "creation_date")
    val creationDate: Long
)