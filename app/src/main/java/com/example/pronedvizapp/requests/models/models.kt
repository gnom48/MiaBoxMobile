package com.example.pronedvizapp.requests.models

import android.os.Parcelable
import com.example.pronedvizapp.bisness.Analytics
import com.example.pronedvizapp.databases.models.INotesAdapterTemplete
import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class UserTypes(val description: String) {
    COMMERCIAL("Риелтер коммерческой недвижимости"),
    PRIVATE("Риелтер частной недвижимости")
}

enum class WorkTasksTypes(val description: String) {
    FLYERS("Рассклейка"),
    CALLS("Обзвон"),
    SHOW("Показ объекта"),
    MEET("Встреча по объекту"),
    DEAL("Сделка"),
    DEPOSIT("Получение задатка"),
    SEARCH("Поиск объектов"),
    ANALYTICS("Аналитика рынка"),
    OTHER("Нечто особенное")
}

data class User(
    var id: Int,
    var login: String,
    var password: String,
    var type: String,
    var photo: String,
    var name: String,
    var gender: String?,
    var birthday: Long?,
    var phone: String?,
    val reg_date: Long
)

data class Note(
    override val id: Int,
    val title: String,
    val desc: String?,
    override val date_time: Long,
    val user_id: Int,
    var notification_id: Int
): INotesAdapterTemplete

data class Task(
    override val id: Int,
    val work_type: String,
    override val date_time: Long,
    val desc: String?,
    val duration_seconds: Int,
    val user_id: Int,
    var notification_id: Int
): INotesAdapterTemplete

data class Team(
    val id: Int,
    val name: String,
    val created_date_time: Long
)

enum class UserStatuses(val description: String) {
    OWNER("Владелец"),
    USER("Участник")
}

data class UserTeam(
    val team_id: Int,
    val user_id: Int,
    val role: String
)

const val DAY_STATISTICS_PERIOD = "day"
const val WEEK_STATISTICS_PERIOD = "week"
const val MONTH_STATISTICS_PERIOD = "month"

data class Statistics(
    val user_id: Int,
    var flyers: Int,
    var calls: Int,
    var shows: Int,
    var meets: Int,
    var deals: Int,
    var deposits: Int,
    var searches: Int,
    var analytics: Int,
    var others: Int
)

data class StatisticsPeriods(
    val day: Statistics,
    val month: Statistics,
    val week: Statistics
)

data class Member(
    val statistics: StatisticsPeriods,
    val user: User,
    val role: UserStatuses
)

data class UserTeamsWithInfoItem(
    val members: List<Member>,
    val team: Team
)

class UserTeamsWithInfo : ArrayList<UserTeamsWithInfoItem>()
