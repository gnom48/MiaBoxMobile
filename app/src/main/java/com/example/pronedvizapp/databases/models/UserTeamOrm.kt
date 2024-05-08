package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "users_teams",
    primaryKeys = ["team_id", "user_id"],
    foreignKeys = [
        ForeignKey(entity = UserOrm::class, parentColumns = ["user_id"], childColumns = ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserStatusOrm::class, parentColumns = ["user_status_id"], childColumns = ["user_status"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TeamOrm::class, parentColumns = ["team_id"], childColumns = ["team_id"], onDelete = ForeignKey.CASCADE)
    ]
)
data class UserTeamOrm(
    @ColumnInfo(name = "team_id")
    val teamId: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "user_status")
    val userStatus: Long,
    @ColumnInfo(name = "add_date")
    val addDate: Long?
)