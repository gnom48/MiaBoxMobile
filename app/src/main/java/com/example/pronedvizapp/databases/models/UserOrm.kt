package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserOrm(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    val userId: Long? = null,
    @ColumnInfo(name = "first_name")
    val firstName: String?,
    val gender: String?,
    val surname: String?,
    val patronumic: String?,
    val login: String,
    val phone: String?,
    val password: String,
    val birthday: Long?,
    val photo: String?
)