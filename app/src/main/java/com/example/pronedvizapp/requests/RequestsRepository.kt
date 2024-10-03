package com.example.pronedvizapp.requests

import android.app.Service
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.CallsInGroupAdapter
import com.example.pronedvizapp.authentication.AuthenticationActivity
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.databases.models.AddressInfoOrm
import com.example.pronedvizapp.databases.models.CRUD
import com.example.pronedvizapp.databases.models.CallRecordOrm
import com.example.pronedvizapp.databases.models.ChangesOrm
import com.example.pronedvizapp.databases.models.DayStatisticsOrm
import com.example.pronedvizapp.databases.models.LastMonthStatisticsWithKpiOrm
import com.example.pronedvizapp.databases.models.MonthStatisticsOrm
import com.example.pronedvizapp.databases.models.NoteOrm
import com.example.pronedvizapp.databases.models.StatisticChangeOptionInfo
import com.example.pronedvizapp.databases.models.SummaryStatisticsWithLevelOrm
import com.example.pronedvizapp.databases.models.TaskOrm
import com.example.pronedvizapp.databases.models.TeamOrm
import com.example.pronedvizapp.databases.models.UserOrm
import com.example.pronedvizapp.databases.models.UserTeamOrm
import com.example.pronedvizapp.databases.models.UsersCallsOrm
import com.example.pronedvizapp.databases.models.WeekStatisticsOrm
import com.example.pronedvizapp.databases.models.getCurrentUnixSeconds
import com.example.pronedvizapp.requests.models.AddressInfo
import com.example.pronedvizapp.requests.models.AddressResponse
import com.example.pronedvizapp.requests.models.AuthData
import com.example.pronedvizapp.requests.models.CallRecord
import com.example.pronedvizapp.requests.models.Coordinates
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Kpi
import com.example.pronedvizapp.requests.models.MONTH_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Member
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.StatisticsPeriods
import com.example.pronedvizapp.requests.models.StatisticsWithKpi
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.Team
import com.example.pronedvizapp.requests.models.TranscriptionTask
import com.example.pronedvizapp.requests.models.TranscriptionTaskStatus
import com.example.pronedvizapp.requests.models.User
import com.example.pronedvizapp.requests.models.UserCall
import com.example.pronedvizapp.requests.models.UserStatuses
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import com.example.pronedvizapp.requests.models.UserTeamsWithInfoItem
import com.example.pronedvizapp.requests.models.WEEK_STATISTICS_PERIOD
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException

object RequestsRepository {

    internal class NoDataException: Exception("No data")
    internal class UnsupportedVersionException: Exception("Текущая версия приложения поддерживается")
    internal class OnlyOnlineException: Exception("Нет подключения к интернету")

    fun usingLocalDataToast(context: Context) = Toast.makeText(context, "Используются локальные данные", Toast.LENGTH_SHORT).show()



    // Users Methods

    /**
     * Получить токен для запросов
     * (доступно только онлайн)
     */
    suspend fun authForToken(context: Context, login: String, password: String): Result<String?> = coroutineScope {
        val usersApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)
        return@coroutineScope try {
            val response = usersApi.authorizationSecure(AuthData(login, password))
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                // желательно в будущем переделать, потому что может быть поддержка нескольких версий
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val versionCode = packageInfo.versionCode
                val supportedVersion = try {
                    response.headers()["supported_version"]?.toInt()
                } catch (e: NumberFormatException) { 0 }
                Log.w("AppVersion", "Supported: $supportedVersion | current: $versionCode")
                if (supportedVersion != versionCode && supportedVersion != 0) {
                    Result.failure(UnsupportedVersionException())
                } else {
                    Result.success(response.body())
                }
            }
        } catch (e: ConnectException) {
            val user = MainStatic.dbViewModel.getUserByLoginPasswordAsync(login, password)
            if (user != null) {
                Result.success(AuthenticationActivity.OFFLINE_TOKEN)
            } else {
                Result.failure(NoDataException())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Регистрация нового пользователя
     * (доступно только онлайн)
     */
    suspend fun regNewUser(context: Context, newUser: User): Result<String> = coroutineScope {
        val usersApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)
        return@coroutineScope try {
            val response = usersApi.registration(newUser)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получить информацию о пользователе
     * (доступно оффлайн после кэширования)
     */
    suspend fun getUserInfo(context: Context, login: String, password: String, token: String = MainStatic.currentToken): DataResult<User?> = coroutineScope {
        val usersApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)
        return@coroutineScope try {
            val response = usersApi.info(token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                MainStatic.dbViewModel.insertUser(response.body()!!.castByJsonTo(UserOrm::class.java))
                DataResult.success(response.body())
            }
        } catch (e: Exception) {
            Log.e("tmp", "$e")
            try {
                val localData = MainStatic.dbViewModel.getUserByLoginPasswordAsync(login, password)!!.castByJsonTo(User::class.java)
                DataResult.cached(localData)
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Обновить личные данные пользователя
     * (доступно только онлайн)
     */
    suspend fun synchroniseUserProfileAsync(context: Context, user: User = MainStatic.currentUser, token: String = MainStatic.currentToken): Result<Boolean> =
        coroutineScope {
            val usersApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)
            return@coroutineScope try {
                val response = usersApi.editUserProfile(user, token)
                if (!response.isSuccessful || response.body() == null) {
                    throw NoDataException()
                }
                if (response.body()!!) {
                    Result.success(true)
                } else {
                    throw NoDataException()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Изменить аватар пользователя
     * (доступно только онлайн)
     */
    suspend fun editAvatarImageFileAsync(context: Context, file: File, token: String = MainStatic.currentToken): Result<String> =
        coroutineScope {
            return@coroutineScope try {
                val api = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)

                val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = api.setImageFileToUser(part, token)
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    throw IOException()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Получить изображение и поместить его в указанный виджет
     * (доступно оффлайн по умолчанию)
     */
    suspend fun bindUserImageFileAsync(context: Context, imageView: ImageView, imageId: String, userId: String = MainStatic.currentUser.id) = coroutineScope {
        val fileName = "img-${userId}-${imageId}.jpg"

        val outputDir = File(context.filesDir, "images")
        if (outputDir.exists()) {
            val file = File(outputDir, fileName)
            if (file.exists()) {
                val inputStream = file.inputStream()
                val buffer = inputStream.readBytes()
                inputStream.close()

                val bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }
                return@coroutineScope
            }
        } else {
            outputDir.mkdirs()
        }

        val usersApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)

        try {
            val response = usersApi.getImageFileByUser(userId, MainStatic.currentToken)
            if (response.isSuccessful && response.body() != null) {
                val inputStream: InputStream = response.body()!!.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }

                val file = File(context.filesDir, "images/$fileName")
                try {
                    val outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                imageView.setImageResource(R.drawable.avatar)
                Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
            }
        }
    }



    // Notes Methods

    /**
     * Добавить заметку пользователю
     * (доступно оффлайн с кэшированием)
     */
    suspend fun addNewNote(context: Context, note: Note, token: String = MainStatic.currentToken, isFromUI: Boolean = true): DataResult<String> = coroutineScope {
        val notesApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiNotes::class.java)
        return@coroutineScope try {
            val newNote = note.castByJsonTo(NoteOrm::class.java)
            if (isFromUI) {
                MainStatic.dbViewModel.insertNote(newNote)
            }
            note.id = newNote.id
            val response = notesApi.addNote(note, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                DataResult.success(response.body()!!)
            }
        } catch (e: Exception) {
            try {
                val changesOrm = ChangesOrm(
                    userId = MainStatic.currentUser.id,
                    action = CRUD.INSERT,
                    dataTypeName = Note::class.java.name,
                    recordId = note.id
                )
                if (isFromUI) {
                    MainStatic.dbViewModel.insertChanges(changesOrm)
                }
                DataResult.cached(changesOrm.id)
            } catch (e:Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Изменить заметку пользователю
     * (доступно оффлайн с кэшированием)
     */
    suspend fun editNote(context: Context, note: Note, token: String = MainStatic.currentToken, isFromUI: Boolean = true): DataResult<Boolean> = coroutineScope {
        val notesApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiNotes::class.java)
        return@coroutineScope try {
            val noteOrm = note.castByJsonTo(NoteOrm::class.java)
            noteOrm.version++
            noteOrm.whenLastUpdated = getCurrentUnixSeconds()
            if (isFromUI) {
                MainStatic.dbViewModel.updateNote(noteOrm)
            }
            val response = notesApi.editNote(note, token)
            if (!response.isSuccessful) {
                throw NoDataException()
            } else {
                DataResult.success(true)
            }
        } catch (e: Exception) {
            try {
                val change = ChangesOrm(
                    userId = MainStatic.currentUser.id,
                    action = CRUD.UPDATE,
                    dataTypeName = Note::class.java.name,
                    recordId = note.id
                )
                if (isFromUI) {
                    MainStatic.dbViewModel.insertChanges(change)
                }
                DataResult.cached(true)
            } catch (e:Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Удаление заметки
     * (доступно оффлайн после кэширования)
     */
    suspend fun deleteNoteAsync(context: Context, noteId: String, token: String = MainStatic.currentToken, isFromUI: Boolean = true): DataResult<Boolean> = coroutineScope {
        val notesApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiNotes::class.java)
        return@coroutineScope try {
            if (isFromUI) {
                val noteToDel = MainStatic.dbViewModel.getNotesAsync(MainStatic.currentUser.id).firstOrNull{ it.id == noteId }
                noteToDel?.let { MainStatic.dbViewModel.deleteNote(it) }
            }
            val response = notesApi.deleteNoteAsync(noteId, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                DataResult.success(response.body()!!)
            }
        } catch (e: Exception) {
            try {
                val change = ChangesOrm(
                    userId = MainStatic.currentUser.id,
                    action = CRUD.DELETE,
                    dataTypeName = Note::class.java.name,
                    recordId = noteId
                )
                if (isFromUI) {
                    MainStatic.dbViewModel.insertChanges(change)
                }
                DataResult.cached(true)
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Получить все заметки пользователя
     * (доступно оффлайн после кэширования)
     */
    suspend fun getAllNotesCurrentUser(context: Context, token: String = MainStatic.currentToken): DataResult<ArrayList<Note>?> = coroutineScope {
        val notesApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiNotes::class.java)
        return@coroutineScope try {
            val resp = notesApi.getAllNotes(token)
            if (!resp.isSuccessful || resp.body() == null) {
                throw NoDataException()
            } else {
                val data = resp.body()?.let { ArrayList<Note>(it) }
                MainStatic.dbViewModel.clearNotes(MainStatic.currentUser.id)
                data?.forEach {
                    MainStatic.dbViewModel.insertNote(it.castByJsonTo(NoteOrm::class.java))
                }
                DataResult.success(data)
            }
        } catch (e: Exception) {
            try {
                val localData = MainStatic.dbViewModel.getNotesAsync(MainStatic.currentUser.id)
                val data = localData.map { orm -> orm.castByJsonTo(Note::class.java) }
                DataResult.cached(ArrayList<Note>(data))
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }



    // Tasks Methods

    /**
     * Добавить новую задачу пользователю
     * (доступно оффлайн с кэшированием)
     */
    suspend fun addNewTask(context: Context, task: Task, token: String = MainStatic.currentToken, isFromUI: Boolean = true): DataResult<String> = coroutineScope {
        val tasksApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTasks::class.java)
        return@coroutineScope try {
            val newTask = task.castByJsonTo(TaskOrm::class.java)
            if (isFromUI) {
                MainStatic.dbViewModel.insertTask(newTask)
            }
            task.id = newTask.id
            val response = tasksApi.addTask(task, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                DataResult.success(response.body()!!)
            }
        } catch (e: Exception) {
            try {
                val changesOrm = ChangesOrm(
                    userId = MainStatic.currentUser.id,
                    action = CRUD.INSERT,
                    dataTypeName = Task::class.java.name,
                    recordId = task.id
                )
                if (isFromUI) {
                    MainStatic.dbViewModel.insertChanges(changesOrm)
                }
                DataResult.cached(changesOrm.id)
            } catch (e:Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Удаление задачи без результата
     * (доступно оффлайн после кэширования)
     */
    suspend fun deleteTaskAsync(context: Context, taskId: String, token: String = MainStatic.currentToken, isFromUI: Boolean = true): DataResult<Boolean> = coroutineScope {
        val tasksApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTasks::class.java)
        return@coroutineScope try {
            if (isFromUI) {
                val taskToDel = MainStatic.dbViewModel.getAllTasksAsync(MainStatic.currentUser.id).firstOrNull{ it.id == taskId }
                taskToDel?.let { MainStatic.dbViewModel.deleteTask(it) }
            }
            val response = tasksApi.deleteTaskAsync(taskId, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                DataResult.success(response.body()!!)
            }
        } catch (e: Exception) {
            try {
                val change = ChangesOrm(
                    userId = MainStatic.currentUser.id,
                    action = CRUD.DELETE,
                    dataTypeName = Task::class.java.name,
                    recordId = taskId
                )
                if (isFromUI) {
                    MainStatic.dbViewModel.insertChanges(change)
                }
                DataResult.cached(true)
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Получить текущие (незаконченные), если completed = false, завершенные, если completed = true, задачи пользователя
     * (доступно оффлайн после кэширования)
     * @param completed по умолчанию false
     */
    suspend fun getAllTasksCurrentUser(context: Context, userId: String, token: String = MainStatic.currentToken, completed: Boolean = false): DataResult<ArrayList<Task>?> = coroutineScope {
        val tasksApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTasks::class.java)
        return@coroutineScope try {
            val resp = if (completed) tasksApi.getAllCompletedTasks(userId, token) else tasksApi.getAllTasksSync(token)
            if (!resp.isSuccessful || resp.body() == null) {
                throw NoDataException()
            } else {
                val data = resp.body()?.let { ArrayList<Task>(it) }
                MainStatic.dbViewModel.clearTasks(MainStatic.currentUser.id)
                data?.forEach {
                    MainStatic.dbViewModel.insertTask(it.castByJsonTo(TaskOrm::class.java))
                }
                DataResult.success(data)
            }
        } catch (e: Exception) {
            try {
                val localData = if (completed) MainStatic.dbViewModel.getCompletedTasksAsync(MainStatic.currentUser.id) else MainStatic.dbViewModel.getCurrentTasksAsync(MainStatic.currentUser.id)
                val data = localData.map { orm -> orm.castByJsonTo(Task::class.java) }
                DataResult.cached(ArrayList<Task>(data))
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }



    // Address Methods

    /**
     * Добавить новую запись о посещенном адресе
     * (доступно оффлайн после кэширования)
     */
    suspend fun addAddressRecordAsync(context: Context, addressInfo: AddressInfo, isFromUI: Boolean = true): DataResult<String?> = coroutineScope {
        val serverApiAddress = RetrofitInstance.getRetrofitInstance(context).create(ServerApiAddress::class.java)
        val preferences = context.getSharedPreferences("settings", Service.MODE_PRIVATE)
        return@coroutineScope try {
            if (isFromUI) {
                val newAddressRecord = addressInfo.castByJsonTo(AddressInfoOrm::class.java)
                MainStatic.dbViewModel.insertAddress(newAddressRecord)
            }
            val response = serverApiAddress.addAddressInfo(addressInfo, preferences.getString(SharedPreferencesHelper.TOKEN_TAG, "")!!)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                DataResult.success(response.body()!!)
            }
        } catch (e: Exception) {
            try {
                if (isFromUI) {
                    val change = ChangesOrm(
                        userId = MainStatic.currentUser.id,
                        action = CRUD.INSERT,
                        dataTypeName = AddressInfo::class.java.name,
                        recordId = addressInfo.recordId
                    )
                    MainStatic.dbViewModel.insertChanges(change)
                    DataResult.cached("")
                } else {
                    throw NoDataException()
                }
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Получить все записи об адресах пользователя за указанный промежуток времени
     * (доступно оффлайн после кэширования)
     */
    suspend fun getAllUserAddressesByPeriod(context: Context, userId: String, dateStart: Int, dateEnd: Int, token: String = MainStatic.currentToken): DataResult<ArrayList<AddressInfo>> = coroutineScope {
        val addressApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiAddress::class.java)
        return@coroutineScope try {
            val response = addressApi.getUserAddressesByPeriod(userId, dateStart, dateEnd, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                response.body()!!.forEach {
                    MainStatic.dbViewModel.insertAddress(it.castByJsonTo(AddressInfoOrm::class.java))
                }
                DataResult.success(ArrayList(response.body()!!))
            }
        } catch (e: Exception) {
            try {
                val localData = MainStatic.dbViewModel.getAllAddressesAsync(MainStatic.currentUser.id)
                var data = localData.map { orm -> orm.castByJsonTo(AddressInfo::class.java) }
                data = data.filter { it.dateTime in dateStart..dateEnd }
                DataResult.cached(ArrayList<AddressInfo>(data.sortedByDescending { it.dateTime }))
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Обратное геокодирование - получить адрес по координатам
     * (доступно только онлайн)
     */
    suspend fun getAddressByCoordsAsync(context: Context, mLocation: Location?): Result<AddressResponse> = coroutineScope {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://suggestions.dadata.ru/suggestions/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val dadataApi = retrofit.create(DadataApi::class.java)
        return@coroutineScope try {
            val response = dadataApi.getAddressByCoordinates(Coordinates(mLocation!!.latitude, mLocation!!.longitude))
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Обратное геокодирование - получить адрес по координатам
     * (доступно только онлайн)
     */
    suspend fun getAddressByCoordsAsyncOnlyTrue(context: Context, mLocation: Location?): Result<AddressResponse> = coroutineScope {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://suggestions.dadata.ru/suggestions/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val dadataApi = retrofit.create(DadataApi::class.java)
        return@coroutineScope try {
            val response = dadataApi.getAddressByCoordinates(Coordinates(mLocation!!.latitude, mLocation!!.longitude))
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Result.success(AddressResponse(suggestions = listOf()))
        }
    }



    // Calls Methods

    /**
     * Отправить запись разговора пользователя (с файлом или без)
     * (доступно оффлайн)
     */
    suspend fun uploadCallRecordAsync(
        file: File?,
        phoneNumber: String,
        info: String,
        dateTime: Long,
        contactName: String,
        lengthSeconds: Int,
        callType: Int,
        context: Context,
        recordId: String? = null,
        isFromUI: Boolean = true): DataResult<String?> {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val token = preferences.getString("TOKEN", "")
        val callRecord: CallRecordOrm = if (recordId.isNullOrEmpty()) {
            CallRecordOrm(
                name = if (file != null) file.name else "no file",
                data = ByteArray(0)
            )
        } else {
            CallRecordOrm(
                id = recordId,
                name = if (file != null) file.name else "no file",
                data = ByteArray(0)
            )
        }
        val userCall = UsersCallsOrm(
            recordId = callRecord.id,
            userId = preferences.getString(SharedPreferencesHelper.USER_ID_TAG, "")!!,
            info = info,
            dateTime = dateTime.toInt(),
            phoneNumber = phoneNumber,
            contactName = contactName,
            lengthSeconds = lengthSeconds,
            callType = callType,
            transcription = CallsInGroupAdapter.NO_TRANSCRIPTION
        )
        return try {
            if (isFromUI) {
                MainStatic.dbViewModel.insertCallRecords(callRecord)
                MainStatic.dbViewModel.insertUserCall(userCall)
            }
            if (token != null) {
                val api = RetrofitInstance.getRetrofitInstance(context).create(ServerApiCalls::class.java)
                val response = if (file != null) {
                    val requestFile = RequestBody.create(MediaType.parse("audio/*"), file)
                    val part = MultipartBody.Part.createFormData("file", file.name, requestFile)
                    api.addCallInfoParams(part, info, phoneNumber, dateTime, contactName, lengthSeconds, callType, callRecord.id, token)
                } else {
                    api.addCallInfoParamsWithoutFile(info, phoneNumber, dateTime, contactName, lengthSeconds, callType, callRecord.id, token)
                }
                Log.e(CallRecordingService.DEBUG_TAG, "Error by addCallInfo: ${response.code()} | ${response.errorBody()} | ${response.body()}")
                if (response.isSuccessful) {
                    DataResult.success(response.body())
                } else {
                    throw NoDataException()
                }
            } else {
                throw Exception()
            }
        } catch (e: Exception) {
            try {
                if (isFromUI) {
                    val changeCallRecord = ChangesOrm(
                        userId = preferences.getString(SharedPreferencesHelper.USER_ID_TAG, "")!!,
                        action = CRUD.INSERT,
                        dataTypeName = CallRecord::class.java.name,
                        recordId = callRecord.id
                    )
                    val changeUserCall = ChangesOrm(
                        userId = preferences.getString(SharedPreferencesHelper.USER_ID_TAG, "")!!,
                        action = CRUD.INSERT,
                        dataTypeName = UserCall::class.java.name,
                        recordId = userCall.recordId!!
                    )
                    MainStatic.dbViewModel.insertChanges(changeCallRecord)
                    MainStatic.dbViewModel.insertChanges(changeUserCall)
                    DataResult.cached(callRecord.id)
                } else {
                    DataResult.failure(e)
                }
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }


    /**
     * Заказать расшифровку записи разговора
     * (доступно только онлайн)
     */
    suspend fun orderCallTranscription(context: Context, userId: String, recordId: String, model: String = "base", tokenAuthorization: String?): Result<TranscriptionTask?> =
        coroutineScope {
            val callsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiCalls::class.java)
            return@coroutineScope try {
                val resp = callsApi.orderCallTranscription(userId, recordId, model, tokenAuthorization)
                if (resp.isSuccessful) {
                    val response = resp.body()
                    Result.success(response)
                } else {
                    Result.failure(Exception("Response is not successful: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Получить информацию о состоянии заказа на расшифровку записи разговора
     * (доступно только онлайн)
     */
    suspend fun getOrderTranscriptionStatus(context: Context, taskId: String, tokenAuthorization: String?): Result<TranscriptionTaskStatus?> =
        coroutineScope {
            val callsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiCalls::class.java)
            return@coroutineScope try {
                val resp = callsApi.getOrderTranscriptionStatus(taskId, tokenAuthorization)
                if (resp.isSuccessful) {
                    val response = resp.body()
                    Result.success(response)
                } else {
                    Result.failure(Exception("Response is not successful: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Получить информацию о файлах записей разговоров пользователя
     * (доступно оффлайн после кэширования)
     */
    suspend fun getAllUserCallsRecords(context: Context, userId: String, token: String = MainStatic.currentToken): DataResult<List<CallRecord>?> = coroutineScope {
        val api = RetrofitInstance.getRetrofitInstance(context).create(ServerApiCalls::class.java)
        return@coroutineScope try {
            val resp = api.getAllRecordsInfo(userId, token)
            if (!resp.isSuccessful || resp.body() == null) {
                throw NoDataException()
            } else {
                val data = resp.body()
                data?.forEach {
                    MainStatic.dbViewModel.insertCallRecords(it.castByJsonTo(CallRecordOrm::class.java))
                }
                DataResult.success(data)
            }
        } catch (e: Exception) {
            try {
                val localData = MainStatic.dbViewModel.getAllCallRecordsAsync(MainStatic.currentUser.id)
                val data = localData.map { orm -> orm.castByJsonTo(CallRecord::class.java) }
                DataResult.cached(data)
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Получить все записи о звонках пользователя
     * (доступно оффлайн после кэширования)
     */
    suspend fun getAllCallsByUserId(context: Context, userId: String, token: String = MainStatic.currentToken): DataResult<ArrayList<UserCall>?> =
        coroutineScope {
            val callsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiCalls::class.java)
            return@coroutineScope try {
                val resp = callsApi.getAllCalls(userId, token)
                if (!resp.isSuccessful || resp.body() == null) {
                    throw NoDataException()
                } else {
                    val data = resp.body()?.let { ArrayList<UserCall>(it) }
                    data?.forEach {
                        MainStatic.dbViewModel.insertUserCall(it.castByJsonTo(UsersCallsOrm::class.java))
                    }
                    DataResult.success(data)
                }
            } catch (e: Exception) {
                try {
                    val localData = MainStatic.dbViewModel.getAllUserCallsAsync(MainStatic.currentUser.id)
                    val data = localData.map { orm -> orm.castByJsonTo(UserCall::class.java) }
                    DataResult.cached(ArrayList<UserCall>(data))
                } catch (e: Exception) {
                    DataResult.failure(e)
                }
            }
        }



    // Teams Methods

    /**
     * Получить всю информацию о команде, участниках и их статистиках
     * (TODO: доступно оффлайн после кэширования)
     */
    suspend fun getMyTeamsInfo(context: Context, token: String): DataResult<UserTeamsWithInfo> = coroutineScope {
        val teamsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTeams::class.java)
        return@coroutineScope try {
            val response = teamsApi.getMyTeams(token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                val data = response.body()!!
                MainStatic.dbViewModel.also { db ->
                    data.forEach { team ->
                        db.clearUsersTeams(teamId = team.team.id)
                        db.insertTeam(team.team.castByJsonTo(TeamOrm::class.java))
                        team.members.forEach { member ->
                            if (member.user.id != MainStatic.currentUser.id) {
                                db.insertUser(member.user.castByJsonTo(UserOrm::class.java))
                            }
                            db.insertUserTeam(UserTeamOrm(teamId = team.team.id, userId = member.user.id, role = member.role.description))
                            db.insertLastMonthStatisticsWithKpi(member.kpi!!.castByJsonTo(LastMonthStatisticsWithKpiOrm::class.java))
                            member.addresses.forEach { a ->
                                val t = a.castByJsonTo(AddressInfoOrm::class.java)
                                Log.d("tmp", "$t")
                                db.insertAddress(t)
                            }
                            member.calls.forEach { c ->
                                db.insertUserCall(c.castByJsonTo(UsersCallsOrm::class.java))
                            }
                            db.insertDayStatistics(member.statistics!!.day.castByJsonTo(DayStatisticsOrm::class.java))
                            db.insertWeekStatistics(member.statistics!!.week.castByJsonTo(WeekStatisticsOrm::class.java))
                            db.insertMonthStatistics(member.statistics!!.month.castByJsonTo(MonthStatisticsOrm::class.java))
                        }
                    }
                }
                DataResult.success(response.body()!!)
            }
        } catch (e: Exception) {
            try {
                val userTeamsWithInfo: ArrayList<UserTeamsWithInfoItem> = arrayListOf<UserTeamsWithInfoItem>()
                val teams = MainStatic.dbViewModel.getAllTeamsByUserAsync(MainStatic.currentUser.id)
                teams.forEach { team ->
                    val members: ArrayList<Member> = arrayListOf()
                    val usersInTeam = MainStatic.dbViewModel.getAllUserTeamsAsync(team.id)
                    usersInTeam.forEach { userTeam ->
                        MainStatic.dbViewModel.getUserById(userTeam.userId)?.let { user ->
                            members.add(Member(
                                user = user.castByJsonTo(User::class.java),
                                role = if (UserStatuses.OWNER.description == userTeam.role) UserStatuses.OWNER else UserStatuses.USER,
                                calls = MainStatic.dbViewModel.getAllUserCallsAsync(user.id).map { it.castByJsonTo(UserCall::class.java) }.toList(),
                                addresses = MainStatic.dbViewModel.getAllAddressesAsync(user.id).map { it.castByJsonTo(AddressInfo::class.java) }.toList(),
                                statistics = try {
                                    StatisticsPeriods(
                                        day = MainStatic.dbViewModel.getDayStatisticsAsync(user.id)!!.castByJsonTo(Statistics::class.java),
                                        week = MainStatic.dbViewModel.getWeekStatisticsAsync(user.id)!!.castByJsonTo(Statistics::class.java),
                                        month = MainStatic.dbViewModel.getMonthStatisticsAsync(user.id)!!.castByJsonTo(Statistics::class.java),
                                    )
                                } catch (e: Exception) {
                                    null
                                },
                                kpi = MainStatic.dbViewModel.getLastMonthStatisticsWithKpiAsync(user.id)?.castByJsonTo(StatisticsWithKpi::class.java)
                            ))
                        }
                    }
                    userTeamsWithInfo.add(UserTeamsWithInfoItem(members, team.castByJsonTo(Team::class.java)))
                }
                val res = UserTeamsWithInfo()
                res.addAll(userTeamsWithInfo)
                DataResult.cached(res)
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Создать команду
     * (доступно только с интернетом)
     */
    suspend fun postCreateTeam(context: Context, token: String, team: Team): Result<String?> = coroutineScope {
        val teamsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTeams::class.java)
        return@coroutineScope try {
            val response = teamsApi.createTeam(team, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Изменить роль участника команды
     * (доступно только онлайн)
     */
    suspend fun moveTeamMemberRole(context: Context, userId: String, teamId: String, role: UserStatuses, token: String = MainStatic.currentToken): Result<Boolean?> = coroutineScope {
        val api = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTeams::class.java)
        return@coroutineScope try {
            val response = api.moveTeamRole(teamId, userId, role.description, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                Result.success(response.body())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Присоединиться к команде
     * (доступно только с интернетом)
     */
    suspend fun joinToTeam(context: Context, token: String, teamId: String, authorId: String): Result<Boolean> = coroutineScope {
        val teamsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTeams::class.java)
        return@coroutineScope try {
            val response = teamsApi.joinTeam(teamId, authorId, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Покинуть команду
     * (доступно только с интернетом)
     */
    suspend fun leaveTeam(context: Context, teamId: String, token: String = MainStatic.currentToken): Result<Boolean> = coroutineScope {
        val teamsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiTeams::class.java)
        return@coroutineScope try {
            val response = teamsApi.leaveTeam(teamId, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                Result.success(response.body()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    // Statistics Methods

    /**
     * Изменить поле в статистике (сразу за все периоды) пользователя
     * (доступно оффлайн, если данные были кэшированы)
     */
    suspend fun editUserStatisticsAsync(context: Context, columnName: String, addValue: Int, token: String = MainStatic.currentToken, isFromUI: Boolean = true): DataResult<Boolean> =
        coroutineScope {
            val userId = MainStatic.currentUser.id
            val usersApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)
            return@coroutineScope try {
                val editableDayStatistic = MainStatic.dbViewModel.getDayStatisticsAsync(userId)
                editableDayStatistic?.let {
                    it.editStatisticFieldByName(columnName, addValue)
                    MainStatic.dbViewModel.updateDayStatistics(it)
                }
                val editableWeekStatistic = MainStatic.dbViewModel.getWeekStatisticsAsync(userId)
                editableWeekStatistic?.let {
                    it.editStatisticFieldByName(columnName, addValue)
                    MainStatic.dbViewModel.updateWeekStatistics(it)
                }
                val editableMonthStatistic = MainStatic.dbViewModel.getMonthStatisticsAsync(userId)
                editableMonthStatistic?.let {
                    it.editStatisticFieldByName(columnName, addValue)
                    MainStatic.dbViewModel.updateMonthStatistics(it)
                }
                val response = usersApi.updateStatisticsAsync(columnName, addValue, token)
                if (!response.isSuccessful || response.body() == null) {
                    throw NoDataException()
                } else {
                    DataResult.success(true)
                }
            } catch (e: Exception) {
                if (isFromUI) {
                    val change = ChangesOrm(
                        userId = userId,
                        action = CRUD.UPDATE,
                        dataTypeName = Statistics::class.java.name,
                        recordId = userId,
                        optionalInfo = Gson().toJson(StatisticChangeOptionInfo(columnName, addValue))
                    )
                    MainStatic.dbViewModel.insertChanges(change)
                    DataResult.cached(true)
                } else {
                    DataResult.failure(e)
                }
            }
        }

    /**
     * Получить статистику пользователя за период
     * (доступно оффлайн после кэширования)
     */
    suspend fun getUserStatisticsByPeriod(
        period: String,
        context: Context,
        token: String = MainStatic.currentToken
    ): DataResult<Statistics> = coroutineScope {
        val usersApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)
        return@coroutineScope try {
            val response = usersApi.getStatisticsByPeriod(period, token)
            if (!response.isSuccessful || response.body() == null) {
                throw NoDataException()
            } else {
                response.body()?.let {
                    when (period) {
                        DAY_STATISTICS_PERIOD -> MainStatic.dbViewModel.insertDayStatistics(it.castByJsonTo(DayStatisticsOrm::class.java))
                        WEEK_STATISTICS_PERIOD -> MainStatic.dbViewModel.insertWeekStatistics(it.castByJsonTo(WeekStatisticsOrm::class.java))
                        MONTH_STATISTICS_PERIOD -> MainStatic.dbViewModel.insertMonthStatistics(it.castByJsonTo(MonthStatisticsOrm::class.java))
                        else -> null
                    }
                }
                DataResult.success(response.body()!!)
            }
        } catch (e: Exception) {
            try {
                val localData = when (period) {
                    DAY_STATISTICS_PERIOD -> MainStatic.dbViewModel.getDayStatisticsAsync(MainStatic.currentUser.id)
                    WEEK_STATISTICS_PERIOD -> MainStatic.dbViewModel.getWeekStatisticsAsync(MainStatic.currentUser.id)
                    MONTH_STATISTICS_PERIOD -> MainStatic.dbViewModel.getMonthStatisticsAsync(MainStatic.currentUser.id)
                    else -> null
                }!!.castByJsonTo(Statistics::class.java)
                DataResult.cached(localData)
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }

    /**
     * Получить информацию о статистике для KPI пользователя
     * (доступно оффлайн после кэширования)
     */
    suspend fun getUserStatisticsWithKpi(context: Context, token: String = MainStatic.currentToken): DataResult<Kpi> = coroutineScope {
        val statisticsApi = RetrofitInstance.getRetrofitInstance(context).create(ServerApiUsers::class.java)
        return@coroutineScope try {
            val resp = statisticsApi.getStatisticsWithKpi(token)
            if (!resp.isSuccessful || resp.body() == null) {
                throw NoDataException()
            } else {
                val data = resp.body()
                data?.let { kpi: Kpi ->
                    if (kpi.lastMonthKpi != null) {
                        MainStatic.dbViewModel.insertLastMonthStatisticsWithKpi(kpi.lastMonthKpi!!.castByJsonTo(LastMonthStatisticsWithKpiOrm::class.java))
                    }
                    if (kpi.summaryDealsRent != null && kpi.summaryDealsSale != null && kpi.userLevel != null && kpi.currentMonthKpi != null) {
                        MainStatic.dbViewModel.insertSummaryStatisticsWithLevel(
                            SummaryStatisticsWithLevelOrm(
                                MainStatic.currentUser.id,
                                dealsRent = kpi.summaryDealsRent!!,
                                dealsSale = kpi.summaryDealsSale!!,
                                basePercent = kpi.currentMonthKpi!!,
                                userLevel = kpi.userLevel!!
                            )
                        )
                    }
                }
                DataResult.success(data!!)
            }
        } catch (e: Exception) {
            try {
                val localData: Kpi? = MainStatic.dbViewModel.getKpi(MainStatic.currentUser.id)
                DataResult.cached(localData!!)
            } catch (e: Exception) {
                DataResult.failure(e)
            }
        }
    }
}