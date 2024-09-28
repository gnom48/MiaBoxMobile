package com.example.pronedvizapp.databases.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pronedvizapp.databases.ModelsCaster
import com.example.pronedvizapp.requests.models.IStatistic
import com.google.gson.annotations.SerializedName
import java.util.UUID

fun getCurrentUnixSeconds(): Long = System.currentTimeMillis() / 1000

interface IOrmVersion {
    var version: Int
    var whenLastUpdated: Long
}

interface ICombinedPrimaryKey {
    var combinedKey: String
    fun getCombinedKeyValue(): String
}

object CRUD {
    const val INSERT: String = "insert"
    const val UPDATE: String = "update"
    const val DELETE: String = "delete"
    const val SELECT: String = "select"
}

@Entity(tableName = "changes")
data class ChangesOrm(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    var action: String,
    @ColumnInfo("data_type_name")
    @SerializedName("data_type_name")
    var dataTypeName: String,
    @ColumnInfo("record_id")
    @SerializedName("record_id")
    var recordId: String ,
    @ColumnInfo("optional_info")
    @SerializedName("optional_info")
    var optionalInfo: String? = null
): ModelsCaster()

@Entity(tableName = "images")
data class ImageOrm(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var data: ByteArray? = ByteArray(0),
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IOrmVersion

@Entity(tableName = "addresses")
data class AddressInfoOrm(
    @PrimaryKey
    @ColumnInfo("record_id")
    @SerializedName("record_id")
    var recordId: String = UUID.randomUUID().toString(),
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    var address: String,
    var lat: Float,
    var lon: Float,
    @ColumnInfo("date_time")
    @SerializedName("date_time")
    var dateTime: Int,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IOrmVersion

@Entity(tableName = "notes")
data class NoteOrm(
    @PrimaryKey()
    override var id: String = UUID.randomUUID().toString(),
    var title: String,
    var desc: String?,
    @ColumnInfo("date_time")
    @SerializedName("date_time")
    override var dateTime: Long,
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    @ColumnInfo("notification_id")
    @SerializedName("notification_id")
    var notificationId: String ?,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): INotesAdapterTemplate, ModelsCaster(), IOrmVersion

@Entity(tableName = "day_statistics")
data class DayStatisticsOrm(
    @PrimaryKey
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    override var userId: String ,
    override var flyers: Int,
    override var calls: Int,
    override var shows: Int,
    override var meets: Int,
    @ColumnInfo("deals_rent")
    @SerializedName("deals_rent")
    override var dealsRent: Int,
    @ColumnInfo("deals_sale")
    @SerializedName("deals_sale")
    override var dealsSale: Int,
    override var deposits: Int,
    override var searches: Int,
    override var analytics: Int,
    @ColumnInfo("regular_contracts")
    @SerializedName("regular_contracts")
    override var regularContracts: Int,
    @ColumnInfo("exclusive_contracts")
    @SerializedName("exclusive_contracts")
    override var exclusiveContracts: Int,
    override var others: Int,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IStatistic, IOrmVersion

@Entity(tableName = "week_statistics")
data class WeekStatisticsOrm(
    @PrimaryKey
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    override var userId: String ,
    override var flyers: Int,
    override var calls: Int,
    override var shows: Int,
    override var meets: Int,
    @ColumnInfo("deals_rent")
    @SerializedName("deals_rent")
    override var dealsRent: Int,
    @ColumnInfo("deals_sale")
    @SerializedName("deals_sale")
    override var dealsSale: Int,
    override var deposits: Int,
    override var searches: Int,
    override var analytics: Int,
    @ColumnInfo("regular_contracts")
    @SerializedName("regular_contracts")
    override var regularContracts: Int,
    @ColumnInfo("exclusive_contracts")
    @SerializedName("exclusive_contracts")
    override var exclusiveContracts: Int,
    override var others: Int,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IStatistic, IOrmVersion

@Entity(tableName = "month_statistics")
data class MonthStatisticsOrm(
    @PrimaryKey
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    override var userId: String ,
    override var flyers: Int,
    override var calls: Int,
    override var shows: Int,
    override var meets: Int,
    @ColumnInfo("deals_rent")
    @SerializedName("deals_rent")
    override var dealsRent: Int,
    @ColumnInfo("deals_sale")
    @SerializedName("deals_sale")
    override var dealsSale: Int,
    override var deposits: Int,
    override var searches: Int,
    override var analytics: Int,
    @ColumnInfo("regular_contracts")
    @SerializedName("regular_contracts")
    override var regularContracts: Int,
    @ColumnInfo("exclusive_contracts")
    @SerializedName("exclusive_contracts")
    override var exclusiveContracts: Int,
    override var others: Int,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IStatistic, IOrmVersion

@Entity(tableName = "tasks")
data class TaskOrm(
    @PrimaryKey()
    override var id: String = UUID.randomUUID().toString(),
    @ColumnInfo("work_type")
    @SerializedName("work_type")
    var workType: String,
    @ColumnInfo("date_time")
    @SerializedName("date_time")
    override var dateTime: Long,
    var desc: String?,
    @ColumnInfo("duration_seconds")
    @SerializedName("duration_seconds")
    var durationSeconds: Int,
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    @ColumnInfo("notification_id")
    @SerializedName("notification_id")
    var notificationId: String ?,
    @ColumnInfo("is_completed")
    @SerializedName("is_completed")
    var isCompleted: Boolean,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): INotesAdapterTemplate, ModelsCaster(), IOrmVersion

@Entity(tableName = "teams")
data class TeamOrm(
    @PrimaryKey()
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    @ColumnInfo("created_date_time")
    @SerializedName("created_date_time")
    var createdDateTime: Long,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IOrmVersion

@Entity(tableName = "users")
data class UserOrm(
    @PrimaryKey()
    var id: String = UUID.randomUUID().toString(),
    var login: String,
    var password: String,
    @ColumnInfo("type")
    @SerializedName("type")
    var type: String,
    var email: String?,
    var name: String,
    var gender: String?,
    var birthday: Long?,
    var phone: String?,
    @ColumnInfo("reg_date")
    @SerializedName("reg_date")
    var regDate: Long,
    @ColumnInfo("image")
    @SerializedName("image")
    var image: String,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IOrmVersion

@Entity(tableName = "users_calls")
data class UsersCallsOrm(
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    @ColumnInfo("record_id")
    @SerializedName("record_id")
    var recordId: String ?,
    var info: String = "",
    @ColumnInfo("date_time")
    @SerializedName("date_time")
    var dateTime: Int,
    @ColumnInfo("phone_number")
    @SerializedName("phone_number")
    var phoneNumber: String,
    @ColumnInfo("contact_name")
    @SerializedName("contact_name")
    var contactName: String,
    @ColumnInfo("length_seconds")
    @SerializedName("length_seconds")
    var lengthSeconds: Int,
    @ColumnInfo("call_type")
    @SerializedName("call_type")
    var callType: Int,
    var transcription: String,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds(),
    @PrimaryKey
    override var combinedKey: String = "$userId-$recordId"
): ModelsCaster(), IOrmVersion, ICombinedPrimaryKey {
    override fun getCombinedKeyValue(): String = "$userId-$recordId"
}

@Entity(tableName = "calls_records")
data class CallRecordOrm(
    @PrimaryKey()
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var data: ByteArray? = ByteArray(0),
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IOrmVersion

@Entity(tableName = "user_teams")
data class UserTeamOrm(
    @ColumnInfo("team_id")
    @SerializedName("team_id")
    var teamId: String ,
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    var role: String,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds(),
    @PrimaryKey
    override var combinedKey: String = "$teamId-$userId"
): ModelsCaster(), IOrmVersion, ICombinedPrimaryKey {
    override fun getCombinedKeyValue(): String = "$teamId-$userId"
}

@Entity(tableName = "last_month_statistics_with_kpi")
data class LastMonthStatisticsWithKpiOrm(
    @PrimaryKey()
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    var flyers: Int,
    var calls: Int,
    var shows: Int,
    var meets: Int,
    @ColumnInfo("deals_rent")
    @SerializedName("deals_rent")
    var dealsRent: Int,
    @ColumnInfo("deals_sale")
    @SerializedName("deals_sale")
    var dealsSale: Int,
    var deposits: Int,
    var searches: Int,
    var analytics: Int,
    var others: Int,
    @ColumnInfo("regular_contracts")
    @SerializedName("regular_contracts")
    var regularContracts: Int,
    @ColumnInfo("exclusive_contracts")
    @SerializedName("exclusive_contracts")
    var exclusiveContracts: Int,
    @ColumnInfo("user_level")
    @SerializedName("user_level")
    var userLevel: String,
    @ColumnInfo("salary_percentage")
    @SerializedName("salary_percentage")
    var salaryPercentage: Float,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IOrmVersion

@Entity(tableName = "summary_statistics_with_level")
data class SummaryStatisticsWithLevelOrm(
    @PrimaryKey()
    @ColumnInfo("user_id")
    @SerializedName("user_id")
    var userId: String ,
    @ColumnInfo("deals_rent")
    @SerializedName("deals_rent")
    var dealsRent: Int,
    @ColumnInfo("deals_sale")
    @SerializedName("deals_sale")
    var dealsSale: Int,
    @ColumnInfo("base_percent")
    @SerializedName("base_percent")
    var basePercent: Float,
    @ColumnInfo("user_level")
    @SerializedName("user_level")
    var userLevel: String,
    override var version: Int = 1,
    @ColumnInfo("when_last_updated")
    @SerializedName("when_last_updated")
    override var whenLastUpdated: Long = getCurrentUnixSeconds()
): ModelsCaster(), IOrmVersion