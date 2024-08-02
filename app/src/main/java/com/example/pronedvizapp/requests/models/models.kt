package com.example.pronedvizapp.requests.models

import com.example.pronedvizapp.databases.models.INotesAdapterTemplete
import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class UserTypes(val description: String): Serializable {
    COMMERCIAL("Риелтер коммерческой недвижимости"),
    PRIVATE("Риелтер частной недвижимости")
}

enum class WorkTasksTypes(val description: String): Serializable {
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

data class Image(
    @SerializedName("id") var id: Int,
    @SerializedName("name") var name: String,
    @SerializedName("data") var data: String
): Serializable

data class User(
    @SerializedName("id") var id: Int,
    @SerializedName("login") var login: String,
    @SerializedName("password") var password: String,
    @SerializedName("type") var type: String,
    @SerializedName("photo") var photo: String,
    @SerializedName("name") var name: String,
    @SerializedName("gender") var gender: String?,
    @SerializedName("birthday") var birthday: Long?,
    @SerializedName("phone") var phone: String?,
    @SerializedName("reg_date") val regDate: Long,
    @SerializedName("image") var image: Int
): Serializable

data class Note(
    @SerializedName("id") override val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("desc") val desc: String?,
    @SerializedName("date_time") override val date_time: Long,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("notification_id") var notificationId: Int
): INotesAdapterTemplete, Serializable

data class Task(
    @SerializedName("id") override val id: Int,
    @SerializedName("work_type") val workType: String,
    @SerializedName("date_time") override val date_time: Long,
    @SerializedName("desc") val desc: String?,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("notification_id") var notificationId: Int
): INotesAdapterTemplete, Serializable

data class Team(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("created_date_time") val createdDateTime: Long
): Serializable

enum class UserStatuses(val description: String): Serializable {
    OWNER("Владелец"),
    USER("Участник")
}

data class UserTeam(
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String
): Serializable

const val DAY_STATISTICS_PERIOD = "day"
const val WEEK_STATISTICS_PERIOD = "week"
const val MONTH_STATISTICS_PERIOD = "month"

data class Statistics(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("flyers") var flyers: Int,
    @SerializedName("calls") var calls: Int,
    @SerializedName("shows") var shows: Int,
    @SerializedName("meets") var meets: Int,
    @SerializedName("deals") var deals: Int,
    @SerializedName("deposits") var deposits: Int,
    @SerializedName("searches") var searches: Int,
    @SerializedName("analytics") var analytics: Int,
    @SerializedName("others") var others: Int
): Serializable

data class StatisticsPeriods(
    @SerializedName("day") val day: Statistics,
    @SerializedName("month") val month: Statistics,
    @SerializedName("week") val week: Statistics
): Serializable

data class Member(
    @SerializedName("statistics") val statistics: StatisticsPeriods,
    @SerializedName("calls") val calls: List<UsersCalls>,
    @SerializedName("addresses") val addresses: List<AddresInfo>,
    @SerializedName("user") val user: User,
    @SerializedName("role") val role: UserStatuses
): Serializable

data class UserTeamsWithInfoItem(
    @SerializedName("members") val members: List<Member>,
    @SerializedName("team") val team: Team
): Serializable

class UserTeamsWithInfo : ArrayList<UserTeamsWithInfoItem>(), Serializable

data class AddresInfo(
    @SerializedName("record_id") val recordId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("address") val address: String,
    @SerializedName("lat") val lat: Float,
    @SerializedName("lon") val lon: Float,
    @SerializedName("date_time") val dateTime: Int
): Serializable

enum class UserKpiLevels(val description: String): Serializable {
    TRAINEE("Стажер"),
    SPECIALIST("Специалист"),
    EXPERT("Эксперт"),
    TOP("ТОП")
}

data class StatisticsWithKpi(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("flyers") var flyers: Int,
    @SerializedName("calls") var calls: Int,
    @SerializedName("shows") var shows: Int,
    @SerializedName("meets") var meets: Int,
    @SerializedName("deals") var deals: Int,
    @SerializedName("deposits") var deposits: Int,
    @SerializedName("searches") var searches: Int,
    @SerializedName("analytics") var analytics: Int,
    @SerializedName("others") var others: Int,
    @SerializedName("user_level") var userLevel: String,
    @SerializedName("salary_percentage") var salaryPercentage: Float
): Serializable

data class CallsRecords(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("data") val data: ByteArray
): Serializable

data class UsersCalls(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("record_id") val recordId: Int,
    @SerializedName("info") val info: String = "",
    @SerializedName("date_time") val dateTime: Int,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("length_seconds") val lengthSeconds: Int,
    @SerializedName("call_type") val callType: Int,
    @SerializedName("transcription") val transcription: String = "no transcription"
): Serializable

interface ITaskStatus {
    val status: String
}

data class TranscriptionTask(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("status") override val status: String
): ITaskStatus, Serializable

data class TranscriptionTaskStatus(
    @SerializedName("status") override val status: String,
    @SerializedName("file") val file: String,
    @SerializedName("model") val model: String,
    @SerializedName("result") val result: String
): ITaskStatus, Serializable