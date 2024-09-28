package com.example.pronedvizapp.databases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.pronedvizapp.requests.models.Kpi
import com.example.pronedvizapp.requests.models.StatisticsWithKpi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DbViewModel(
    application: Application,
    private val dao: DbDao = LocalDb.getDb(application).getDao()
) : AndroidViewModel(application) {

    // User Methods
    suspend fun getUserByLoginPasswordAsync(login: String, password: String) = dao.getUserByLoginPassword(login, password)

    suspend fun getUserById(id: String) = dao.getUserById(id)

    fun getUserByLoginPassword(login: String, password: String, callback: (UserOrm?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = dao.getUserByLoginPassword(login, password)
            withContext(Dispatchers.Main) {
                callback(user)
            }
        }
    }

    fun insertUser(user: UserOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertUser(user)
        }
    }

    fun updateUser(user: UserOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateUser(user)
        }
    }

    fun deleteUser(user: UserOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteUser(user)
        }
    }

    fun getAllUsers(callback: (List<UserOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = dao.getAllUsers()
            withContext(Dispatchers.Main) {
                callback(users)
            }
        }
    }

    // Notes Methods
    suspend fun getNotesAsync(userId: String) = dao.getAllNotes(userId)

    fun getNotes(id: String, callback: (List<NoteOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val notes = dao.getAllNotes(id)
            withContext(Dispatchers.Main) {
                callback(notes)
            }
        }
    }

    suspend fun getNote(userId: String, noteId: String) = dao.getNote(userId, noteId)

    fun insertNote(item: NoteOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertNote(item)
        }
    }

    fun deleteNote(item: NoteOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteNote(item)
        }
    }

    fun clearNotes(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearNotes(userId)
        }
    }

    fun updateNote(item: NoteOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateNote(item)
        }
    }

    // Changes Methods
    suspend fun getChangesByUserIdAsync(userId: String) = dao.getAllChanges(userId)
    fun getChangesByUserId(userId: String, callback: (List<ChangesOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val changesList = dao.getAllChanges(userId)
            withContext(Dispatchers.Main) {
                callback(changesList)
            }
        }
    }

    fun insertChanges(item: ChangesOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            var isNewChangeRecord = true
            getChangesByUserIdAsync(item.userId).forEach {
                it.id = item.id
                if (it == item) {
                    isNewChangeRecord = false
                    return@forEach
                }
            }
            if (isNewChangeRecord) {
                dao.insertChange(item)
            }
        }
    }

    fun deleteChange(item: ChangesOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteChange(item)
        }
    }

    fun clearChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearChanges()
        }
    }

    // Task Methods

    suspend fun getTask(userId: String, noteId: String) = dao.getTask(userId, noteId)

    suspend fun getAllTasksAsync(userId: String) = dao.getAllTasksAsync(userId)

    suspend fun getCurrentTasksAsync(userId: String) = dao.getCurrentTasks(userId)

    fun getCurrentTasks(id: String, callback: (List<TaskOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = dao.getCurrentTasks(id)
            withContext(Dispatchers.Main) {
                callback(tasks)
            }
        }
    }

    suspend fun getCompletedTasksAsync(userId: String) = dao.getCompletedTasks(userId)

    fun getCompletedTasks(id: String, callback: (List<TaskOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = dao.getCompletedTasks(id)
            withContext(Dispatchers.Main) {
                callback(tasks)
            }
        }
    }

    fun insertTask(item: TaskOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertTask(item)
        }
    }

    fun deleteTask(item: TaskOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTask(item)
        }
    }

    fun clearTasks(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearTasks(userId)
        }
    }

    fun updateTask(item: TaskOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateTask(item)
        }
    }

    // Team Methods
    suspend fun getAllTeamsByUserAsync(userId: String) = dao.getAllTeamsByUser(userId)

    fun getAllTeamsByUser(userId: String, callback: (List<TeamOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val teams = dao.getAllTeamsByUser(userId)
            withContext(Dispatchers.Main) {
                callback(teams)
            }
        }
    }

    fun insertTeam(item: TeamOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertTeam(item)
        }
    }

    fun updateTeam(item: TeamOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateTeam(item)
        }
    }

    fun deleteTeam(item: TeamOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTeam(item)
        }
    }

    // UserCalls Methods
    suspend fun getAllUserCallsAsync(userId: String) = dao.getAllUserCalls(userId)

    fun getAllUserCalls(userId: String, callback: (List<UsersCallsOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val calls = dao.getAllUserCalls(userId)
            withContext(Dispatchers.Main) {
                callback(calls)
            }
        }
    }

    suspend fun getUserCall(userId: String, recordId: String) = dao.getUserCall(userId, recordId)

    fun insertUserCall(item: UsersCallsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertUserCall(item)
        }
    }

    fun updateUserCall(item: UsersCallsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateUserCall(item)
        }
    }

    fun deleteUserCall(item: UsersCallsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteUserCall(item)
        }
    }

    // UserTeams Methods
    suspend fun getAllUserTeamsAsync(teamId: String) = dao.getAllUserTeams(teamId)

    suspend fun getAllMembersTeamAsync(teamId: String) = dao.getAllMembersTeamAsync(teamId)

    fun getAllUserTeams(userId: String, callback: (List<UserTeamOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userTeams = dao.getAllUserTeams(userId)
            withContext(Dispatchers.Main) {
                callback(userTeams)
            }
        }
    }

    fun insertUserTeam(item: UserTeamOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertUserTeam(item)
        }
    }

    fun updateUserTeam(item: UserTeamOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateUserTeam(item)
        }
    }

    fun deleteUserTeam(item: UserTeamOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteUserTeam(item)
        }
    }

    fun clearUsersTeams(teamId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearUsersTeams(teamId)
        }
    }

    // Statistics Methods
    suspend fun getLastMonthStatisticsWithKpiAsync(userId: String) = dao.getLastMonthStatisticsWithKpi(userId)

    fun getLastMonthStatisticsWithKpi(userId: String, callback: (LastMonthStatisticsWithKpiOrm?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = dao.getLastMonthStatisticsWithKpi(userId)
            withContext(Dispatchers.Main) {
                callback(stats)
            }
        }
    }

    suspend fun getKpi(userId: String): Kpi = coroutineScope {
            val last_month_kpi = dao.getLastMonthStatisticsWithKpi(userId)
            val summary = dao.getSummaryStatisticsWithLevel(userId)
            Kpi(
                lastMonthKpi = last_month_kpi?.castByJsonTo(StatisticsWithKpi::class.java),
                currentMonthKpi = null,
                userLevel = summary?.userLevel,
                summaryDealsRent = summary?.dealsRent,
                summaryDealsSale = summary?.dealsSale
            )
        }


    fun insertLastMonthStatisticsWithKpi(item: LastMonthStatisticsWithKpiOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertLastMonthStatisticsWithKpi(item)
        }
    }

    fun updateLastMonthStatisticsWithKpi(item: LastMonthStatisticsWithKpiOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateLastMonthStatisticsWithKpi(item)
        }
    }

    fun deleteLastMonthStatisticsWithKpi(item: LastMonthStatisticsWithKpiOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteLastMonthStatisticsWithKpi(item)
        }
    }

    suspend fun getSummaryStatisticsWithLevelAsync(userId: String) = dao.getSummaryStatisticsWithLevel(userId)

    fun getSummaryStatisticsWithLevel(userId: String, callback: (SummaryStatisticsWithLevelOrm?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = dao.getSummaryStatisticsWithLevel(userId)
            withContext(Dispatchers.Main) {
                callback(stats)
            }
        }
    }

    fun insertSummaryStatisticsWithLevel(item: SummaryStatisticsWithLevelOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSummaryStatisticsWithLevel(item)
        }
    }

    fun updateSummaryStatisticsWithLevel(item: SummaryStatisticsWithLevelOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSummaryStatisticsWithLevel(item)
        }
    }

    fun deleteSummaryStatisticsWithLevel(item: SummaryStatisticsWithLevelOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSummaryStatisticsWithLevel(item)
        }
    }

    // Image Methods
    fun getImage(imageId: Int, callback: (ImageOrm?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val image = dao.getImage(imageId)
            withContext(Dispatchers.Main) {
                callback(image)
            }
        }
    }

    fun insertImage(item: ImageOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertImage(item)
        }
    }

    fun updateImage(item: ImageOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateImage(item)
        }
    }

    fun deleteImage(item: ImageOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteImage(item)
        }
    }

    // Address Methods
    suspend fun getAllAddressesAsync(userId: String) = dao.getAllAddresses(userId)

    fun getAllAddresses(userId: String, callback: (List<AddressInfoOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val addresses = dao.getAllAddresses(userId)
            withContext(Dispatchers.Main) {
                callback(addresses)
            }
        }
    }

    suspend fun getAddressInfo(userId: String, recordId: String) = dao.getAddressInfo(userId, recordId)

    fun insertAddress(item: AddressInfoOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertAddress(item)
        }
    }

    fun updateAddress(item: AddressInfoOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateAddress(item)
        }
    }

    fun deleteAddress(item: AddressInfoOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAddress(item)
        }
    }

    // Day Statistics Methods
    suspend fun getDayStatisticsAsync(userId: String) = dao.getDayStatistics(userId)

    fun getDayStatistics(userId: String, callback: (DayStatisticsOrm?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = dao.getDayStatistics(userId)
            withContext(Dispatchers.Main) {
                callback(stats)
            }
        }
    }

    fun insertDayStatistics(item: DayStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertDayStatistics(item)
        }
    }

    fun updateDayStatistics(item: DayStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateDayStatistics(item)
        }
    }

    fun deleteDayStatistics(item: DayStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteDayStatistics(item)
        }
    }

    // Week Statistics Methods
    suspend fun getWeekStatisticsAsync(userId: String) = dao.getWeekStatistics(userId)

    fun getWeekStatistics(userId: String, callback: (WeekStatisticsOrm?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = dao.getWeekStatistics(userId)
            withContext(Dispatchers.Main) {
                callback(stats)
            }
        }
    }

    fun insertWeekStatistics(item: WeekStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertWeekStatistics(item)
        }
    }

    fun updateWeekStatistics(item: WeekStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateWeekStatistics(item)
        }
    }

    fun deleteWeekStatistics(item: WeekStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteWeekStatistics(item)
        }
    }

    // Month Statistics Methods
    suspend fun getMonthStatisticsAsync(userId: String) = dao.getMonthStatistics(userId)

    fun getMonthStatistics(userId: String, callback: (MonthStatisticsOrm?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = dao.getMonthStatistics(userId)
            withContext(Dispatchers.Main) {
                callback(stats)
            }
        }
    }

    fun insertMonthStatistics(item: MonthStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMonthStatistics(item)
        }
    }

    fun updateMonthStatistics(item: MonthStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateMonthStatistics(item)
        }
    }

    fun deleteMonthStatistics(item: MonthStatisticsOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteMonthStatistics(item)
        }
    }

    // Calls Records Methods
    suspend fun getAllCallRecordsAsync(userId: String) = dao.getAllCallRecords(userId)

    suspend fun getCallRecordAsync(recordId: String) = dao.getCallRecord(recordId)


    fun getAllCallRecords(userId: String, callback: (List<CallRecordOrm>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = dao.getAllCallRecords(userId)
            withContext(Dispatchers.Main) {
                if (data != null) {
                    callback(data)
                }
            }
        }
    }

    fun insertCallRecords(item: CallRecordOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCallRecord(item)
        }
    }

    fun updateCallRecords(item: CallRecordOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCallRecord(item)
        }
    }

    fun deleteCallRecords(item: CallRecordOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCallRecord(item)
        }
    }

}
