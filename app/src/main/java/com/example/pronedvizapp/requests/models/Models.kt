package com.example.pronedvizapp.requests.models

import com.example.pronedvizapp.databases.ModelsCaster
import com.example.pronedvizapp.databases.models.INotesAdapterTemplate
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.UUID

enum class UserTypes(var description: String): Serializable {
    COMMERCIAL("Риелтер коммерческой недвижимости"),
    PRIVATE("Риелтер частной недвижимости")
}

enum class WorkTasksTypes(var description: String): Serializable {
    FLYERS("Рассклейка"),
    CALLS("Обзвон"),
    SHOW("Показ объекта"),
    MEET("Встреча по объекту"),
    DEAL("Сделка"),
    DEAL_RENT("Сделка по аренде"),
    DEAL_SALE("Сделка по продаже"),
    DEPOSIT("Получение задатка"),
    SEARCH("Поиск объектов"),
    ANALYTICS("Аналитика рынка"),
    OTHER("Нечто особенное"),
    REGULAR_CONTRACT("Обычный договор"),
    EXCLUSIVE_CONTRACT("Эксклюзивный договор")
}

data class Image(
    @SerializedName("id") var id: String = UUID.randomUUID().toString(),
    @SerializedName("name") var name: String,
    @SerializedName("data") var data: String
): Serializable, ModelsCaster()

data class User(
    @SerializedName("id") var id: String = UUID.randomUUID().toString(),
    @SerializedName("login") var login: String,
    @SerializedName("password") var password: String,
    @SerializedName("type") var type: String,
    @SerializedName("email") var email: String,
    @SerializedName("name") var name: String,
    @SerializedName("gender") var gender: String?,
    @SerializedName("birthday") var birthday: Long?,
    @SerializedName("phone") var phone: String?,
    @SerializedName("reg_date") var regDate: Long,
    @SerializedName("image") var image: String
): Serializable, ModelsCaster()

data class AuthData(
    @SerializedName("login") var login: String,
    @SerializedName("password") var password: String,
): Serializable

data class Note(
    @SerializedName("id") override var id: String = UUID.randomUUID().toString(),
    @SerializedName("title") var title: String,
    @SerializedName("desc") var desc: String?,
    @SerializedName("date_time") override var dateTime: Long,
    @SerializedName("user_id") var userId: String,
    @SerializedName("notification_id") var notificationId: Int
): INotesAdapterTemplate, Serializable, ModelsCaster()

data class Task(
    @SerializedName("id") override var id: String = UUID.randomUUID().toString(),
    @SerializedName("work_type") var workType: String,
    @SerializedName("date_time") override var dateTime: Long,
    @SerializedName("desc") var desc: String?,
    @SerializedName("duration_seconds") var durationSeconds: Int,
    @SerializedName("user_id") var userId: String,
    @SerializedName("notification_id") var notificationId: Int,
    @SerializedName("is_completed") var isCompleted: Boolean
): INotesAdapterTemplate, Serializable, ModelsCaster()

class CompletedTasks(c: MutableCollection<out Task>) : ArrayList<Task>(c)

data class Team(
    @SerializedName("id") var id: String = UUID.randomUUID().toString(),
    @SerializedName("name") var name: String,
    @SerializedName("created_date_time") var createdDateTime: Long
): Serializable, ModelsCaster()

enum class UserStatuses(var description: String): Serializable {
    OWNER("Владелец"),
    USER("Участник")
}

data class UserTeam(
    @SerializedName("team_id") var teamId: String,
    @SerializedName("user_id") var userId: String,
    @SerializedName("role") var role: String
): Serializable, ModelsCaster()

const val DAY_STATISTICS_PERIOD = "day"
const val WEEK_STATISTICS_PERIOD = "week"
const val MONTH_STATISTICS_PERIOD = "month"

interface IStatistic {
    var userId: String
    var flyers: Int
    var calls: Int
    var shows: Int
    var meets: Int
    var dealsRent: Int
    var dealsSale: Int
    var deposits: Int
    var searches: Int
    var analytics: Int
    var regularContracts: Int
    var exclusiveContracts: Int
    var others: Int
}

data class Statistics(
    @SerializedName("user_id") override var userId: String,
    @SerializedName("flyers") override var flyers: Int,
    @SerializedName("calls") override var calls: Int,
    @SerializedName("shows") override var shows: Int,
    @SerializedName("meets") override var meets: Int,
    @SerializedName("deals_rent") override var dealsRent: Int,
    @SerializedName("deals_sale") override var dealsSale: Int,
    @SerializedName("deposits") override var deposits: Int,
    @SerializedName("searches") override var searches: Int,
    @SerializedName("analytics") override var analytics: Int,
    @SerializedName("regular_contracts") override var regularContracts: Int,
    @SerializedName("exclusive_contracts") override var exclusiveContracts: Int,
    @SerializedName("others") override var others: Int
): Serializable, IStatistic, ModelsCaster()

data class StatisticsPeriods(
    @SerializedName("day") var day: Statistics,
    @SerializedName("month") var month: Statistics,
    @SerializedName("week") var week: Statistics
): Serializable

enum class ContractTypes(var description: String): Serializable {
    NO("Договор не заключен"),
    REGULAR("Заключен договор"),
    EXCLUSIVE("Заключен эксклюзивный договор")
}

data class Member(
    @SerializedName("statistics") var statistics: StatisticsPeriods?,
    @SerializedName("calls") var calls: List<UserCall>,
    @SerializedName("addresses") var addresses: List<AddressInfo>,
    @SerializedName("user") var user: User,
    @SerializedName("kpi") var kpi: StatisticsWithKpi?,
    @SerializedName("role") var role: UserStatuses
): Serializable

data class UserTeamsWithInfoItem(
    @SerializedName("members") var members: List<Member>,
    @SerializedName("team") var team: Team
): Serializable

class UserTeamsWithInfo : ArrayList<UserTeamsWithInfoItem>(), Serializable

data class AddressInfo(
    @SerializedName("record_id") var recordId: String = UUID.randomUUID().toString(),
    @SerializedName("user_id") var userId: String,
    @SerializedName("address") var address: String,
    @SerializedName("lat") var lat: Float,
    @SerializedName("lon") var lon: Float,
    @SerializedName("date_time") var dateTime: Int
): Serializable, ModelsCaster()

enum class UserKpiLevels(var description: String): Serializable {
    TRAINEE("Стажер"),
    SPECIALIST("Специалист"),
    EXPERT("Эксперт"),
    TOP("ТОП")
}

data class StatisticsWithKpi(
    @SerializedName("user_id") override var userId: String,
    @SerializedName("flyers") override var flyers: Int,
    @SerializedName("calls") override var calls: Int,
    @SerializedName("shows") override var shows: Int,
    @SerializedName("meets") override var meets: Int,
    @SerializedName("deals_rent") override var dealsRent: Int,
    @SerializedName("deals_sale") override var dealsSale: Int,
    @SerializedName("deposits") override var deposits: Int,
    @SerializedName("searches") override var searches: Int,
    @SerializedName("analytics") override var analytics: Int,
    @SerializedName("others") override var others: Int,
    @SerializedName("regular_contracts") override var regularContracts: Int,
    @SerializedName("exclusive_contracts") override var exclusiveContracts: Int,
    @SerializedName("user_level") var userLevel: String,
    @SerializedName("salary_percentage") var salaryPercentage: Float
): Serializable, IStatistic, ModelsCaster()

data class Kpi(
    @SerializedName("last_month_kpi") var lastMonthKpi: StatisticsWithKpi?,
    @SerializedName("current_month_kpi") var currentMonthKpi: Float?,
    @SerializedName("level") var userLevel: String?,
    @SerializedName("summary_deals_rent") var summaryDealsRent: Int?,
    @SerializedName("summary_deals_sale") var summaryDealsSale: Int?
): Serializable

data class CallRecord(
    @SerializedName("id") var id: String = UUID.randomUUID().toString(),
    @SerializedName("name") var name: String,
    @SerializedName("data") var data: ByteArray
): Serializable, ModelsCaster()

data class UserCall(
    @SerializedName("user_id") var userId: String,
    @SerializedName("record_id") var recordId: String,
    @SerializedName("info") var info: String = "",
    @SerializedName("date_time") var dateTime: Int,
    @SerializedName("phone_number") var phoneNumber: String,
    @SerializedName("contact_name") var contactName: String,
    @SerializedName("length_seconds") var lengthSeconds: Int,
    @SerializedName("call_type") var callType: Int,
    @SerializedName("transcription") var transcription: String
): Serializable, ModelsCaster()

interface ITaskStatus {
    var status: String
}

data class TranscriptionTask(
    @SerializedName("task_id") var taskId: String,
    @SerializedName("status") override var status: String
): ITaskStatus, Serializable

data class TranscriptionTaskStatus(
    @SerializedName("status") override var status: String,
    @SerializedName("file") var file: String,
    @SerializedName("model") var model: String,
    @SerializedName("result") var result: String
): ITaskStatus, Serializable