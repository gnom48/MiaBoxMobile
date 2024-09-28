package com.example.pronedvizapp.databases

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

@Dao
interface DbDao {

    // Changes
    @Query("SELECT * FROM changes WHERE user_id = :userId")
    suspend fun getAllChanges(userId: String): List<ChangesOrm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChange(item: ChangesOrm)

    @Delete
    suspend fun deleteChange(item: ChangesOrm)

    @Query("DELETE FROM changes")
    suspend fun clearChanges()

    // Images
    @Query("SELECT * FROM images WHERE id = :imageId")
    suspend fun getImage(imageId: Int): ImageOrm

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(item: ImageOrm)

    @Update
    suspend fun updateImage(item: ImageOrm)

    @Delete
    suspend fun deleteImage(item: ImageOrm)

    // Addresses
    @Query("SELECT * FROM addresses WHERE user_id = :userId")
    suspend fun getAllAddresses(userId: String): List<AddressInfoOrm>

    @Query("SELECT * FROM addresses WHERE user_id = :userId AND record_id = :recordId")
    suspend fun getAddressInfo(userId: String, recordId: String): AddressInfoOrm

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(item: AddressInfoOrm)

    @Update
    suspend fun updateAddress(item: AddressInfoOrm)

    @Delete
    suspend fun deleteAddress(item: AddressInfoOrm)

    // Notes
    @Query("SELECT * FROM notes WHERE user_id = :userId")
    suspend fun getAllNotes(userId: String): List<NoteOrm>

    @Query("SELECT * FROM notes WHERE user_id = :userId AND id = :noteId")
    suspend fun getNote(userId: String, noteId: String): NoteOrm

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(item: NoteOrm)

    @Update
    suspend fun updateNote(item: NoteOrm)

    @Delete
    suspend fun deleteNote(item: NoteOrm)

    @Query("DELETE FROM notes WHERE user_id = :userId")
    suspend fun clearNotes(userId: String)

    // Day Statistics
    @Query("SELECT * FROM day_statistics WHERE user_id = :userId")
    suspend fun getDayStatistics(userId: String): DayStatisticsOrm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayStatistics(item: DayStatisticsOrm)

    @Update
    suspend fun updateDayStatistics(item: DayStatisticsOrm)

    @Delete
    suspend fun deleteDayStatistics(item: DayStatisticsOrm)

    // Week Statistics
    @Query("SELECT * FROM week_statistics WHERE user_id = :userId")
    suspend fun getWeekStatistics(userId: String): WeekStatisticsOrm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeekStatistics(item: WeekStatisticsOrm)

    @Update
    suspend fun updateWeekStatistics(item: WeekStatisticsOrm)

    @Delete
    suspend fun deleteWeekStatistics(item: WeekStatisticsOrm)

    // Month Statistics
    @Query("SELECT * FROM month_statistics WHERE user_id = :userId")
    suspend fun getMonthStatistics(userId: String): MonthStatisticsOrm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthStatistics(item: MonthStatisticsOrm)

    @Update
    suspend fun updateMonthStatistics(item: MonthStatisticsOrm)

    @Delete
    suspend fun deleteMonthStatistics(item: MonthStatisticsOrm)

    // Tasks

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND id = :taskId")
    suspend fun getTask(userId: String, taskId: String): TaskOrm

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND is_completed = 0")
    suspend fun getCurrentTasks(userId: String): List<TaskOrm>

    @Query("SELECT * FROM tasks WHERE user_id = :userId")
    suspend fun getAllTasksAsync(userId: String): List<TaskOrm>

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND is_completed = 1")
    suspend fun getCompletedTasks(userId: String): List<TaskOrm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(item: TaskOrm)

    @Update
    suspend fun updateTask(item: TaskOrm)

    @Delete
    suspend fun deleteTask(item: TaskOrm)

    @Query("DELETE FROM tasks WHERE user_id = :userId AND is_completed = 0")
    suspend fun clearTasks(userId: String)

    // Teams
    @Query("SELECT * FROM teams WHERE id in (SELECT team_id FROM user_teams WHERE user_id = :userId)")
    suspend fun getAllTeamsByUser(userId: String): List<TeamOrm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(item: TeamOrm)

    @Update
    suspend fun updateTeam(item: TeamOrm)

    @Delete
    suspend fun deleteTeam(item: TeamOrm)

    // Users
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserOrm>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserOrm?

    @Query("SELECT * FROM users WHERE login = :login AND password = :password")
    suspend fun getUserByLoginPassword(login: String, password: String): UserOrm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(item: UserOrm)

    @Update
    suspend fun updateUser(item: UserOrm)

    @Delete
    suspend fun deleteUser(item: UserOrm)

    // UsersCalls
    @Query("SELECT * FROM users_calls WHERE user_id = :userId")
    suspend fun getAllUserCalls(userId: String): List<UsersCallsOrm>

    @Query("SELECT * FROM users_calls WHERE user_id = :userId AND record_id = :recordId")
    suspend fun getUserCall(userId: String, recordId: String): UsersCallsOrm

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCall(item: UsersCallsOrm)

    @Update
    suspend fun updateUserCall(item: UsersCallsOrm)

    @Delete
    suspend fun deleteUserCall(item: UsersCallsOrm)

    // UserTeams
    @Query("SELECT * FROM user_teams WHERE team_id = :teamId")
    suspend fun getAllUserTeams(teamId: String): List<UserTeamOrm>

    @Query("SELECT * FROM user_teams WHERE team_id = :teamId")
    suspend fun getAllMembersTeamAsync(teamId: String): List<UserTeamOrm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserTeam(item: UserTeamOrm)

    @Update
    suspend fun updateUserTeam(item: UserTeamOrm)

    @Delete
    suspend fun deleteUserTeam(item: UserTeamOrm)

    @Query("DELETE FROM user_teams WHERE team_id = :teamId")
    suspend fun clearUsersTeams(teamId: String)

    // LastMonthStatisticsWithKPI
    @Query("SELECT * FROM last_month_statistics_with_kpi WHERE user_id = :userId")
    suspend fun getLastMonthStatisticsWithKpi(userId: String): LastMonthStatisticsWithKpiOrm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLastMonthStatisticsWithKpi(item: LastMonthStatisticsWithKpiOrm)

    @Update
    suspend fun updateLastMonthStatisticsWithKpi(item: LastMonthStatisticsWithKpiOrm)

    @Delete
    suspend fun deleteLastMonthStatisticsWithKpi(item: LastMonthStatisticsWithKpiOrm)

    // Summary Statistics With Level
    @Query("SELECT * FROM summary_statistics_with_level WHERE user_id = :userId")
    suspend fun getSummaryStatisticsWithLevel(userId: String): SummaryStatisticsWithLevelOrm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaryStatisticsWithLevel(item: SummaryStatisticsWithLevelOrm)

    @Update
    suspend fun updateSummaryStatisticsWithLevel(item: SummaryStatisticsWithLevelOrm)

    @Delete
    suspend fun deleteSummaryStatisticsWithLevel(item: SummaryStatisticsWithLevelOrm)


    // Calls Records
    @Query("SELECT * FROM calls_records WHERE id IN (SELECT record_id FROM users_calls WHERE user_id = :userId)")
    suspend fun getAllCallRecords(userId: String): List<CallRecordOrm>

    @Query("SELECT * FROM calls_records WHERE id = :id")
    suspend fun getCallRecord(id: String): CallRecordOrm

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(item: CallRecordOrm)

    @Update
    suspend fun updateCallRecord(item: CallRecordOrm)

    @Delete
    suspend fun deleteCallRecord(item: CallRecordOrm)
}
