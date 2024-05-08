package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = UserOrm::class,
        parentColumns = ["user_id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class NoteOrm(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "note_id")
    val noteId: Long? = null,
    @ColumnInfo(name = "note_title")
    val noteTitle: String,
    @ColumnInfo(name = "note_description")
    val noteDescription: String,
    @ColumnInfo(name = "note_datetime")
    val date_time: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long
)