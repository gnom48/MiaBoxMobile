package com.example.pronedvizapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.bisness.network.isNetworkAvailable
import com.example.pronedvizapp.requests.RequestsRepository.authForToken
import com.example.pronedvizapp.requests.RequestsRepository.getUserInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

const val PERMISSION_REQUEST_CODE_SUCCESS = 1
class InitialActivity : AppCompatActivity() {

    private val preferences: SharedPreferences by lazy { this.getSharedPreferences(SharedPreferencesHelper.SETTINGS_PREFS_KEY, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)
        val networkStatus = isNetworkAvailable(this)
        MainStatic.isCurrentOnline.value = networkStatus
//        if (!networkStatus) {
//            MaterialAlertDialogBuilder(this)
//                .setTitle("Внимание")
//                .setMessage("Отсутствует подключение к интернету, вы будете использовать локальные данные. Синхронизация произойдет автоматически при подключении к сети.")
//                .setPositiveButton("Ок") { dialog, _ ->
//                    dialog.dismiss()
//                }
//                .setOnDismissListener {
//                    checkPermissions()
//                }
//                .create()
//                .show()
//        } else {
//            checkPermissions()
//        }
        checkPermissions()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_CODE_SUCCESS && permissions.isNotEmpty()){
            tryEnter()
        }
    }

    private fun checkPermissions() {
        val intent = Intent()
        val packN = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packN)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packN")
            startActivity(intent)
        }

        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.SCHEDULE_EXACT_ALARM,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.PROCESS_OUTGOING_CALLS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.FOREGROUND_SERVICE
        )
        val deniedPermissions = mutableListOf<String>()

        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)
                Log.w("permissions", "Denied permission: $permission")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.READ_MEDIA_AUDIO
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)
                Log.w("permissions", "Add denied permission: $permission")
            }
        } else {
            val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)
                Log.w("permissions", "Add denied permission: $permission")
            }
        }

        if (deniedPermissions.isNotEmpty()) {
            Log.w("permissions", "Denied permissions: $deniedPermissions")
            MaterialAlertDialogBuilder(this)
                .setTitle("Для полноценной работы приложения необходимо получить некоторые разрешения.")
                .setMessage("Пожалуйста, разрешите доступ к вашим геоданным, камере, уведомлениям и другим функциям для работы приложения")
                .setPositiveButton("Ок"){ _,_ ->
                    ActivityCompat.requestPermissions(
                        this,
                        deniedPermissions.toTypedArray(),
                        PERMISSION_REQUEST_CODE_SUCCESS
                    )
                }
                .setNegativeButton("Отмена") { dialog,_ -> dialog.dismiss(); this@InitialActivity.finish() }
                .create()
                .show()
        } else {
            tryEnter()
        }
    }

    private fun tryEnter() {
        if (preferences.contains(SharedPreferencesHelper.LAST_LOGIN_TAG) && preferences.contains(SharedPreferencesHelper.LAST_PASSWORD_TAG)) {
            val login = preferences.getString(SharedPreferencesHelper.LAST_LOGIN_TAG, "")
            val password = preferences.getString(SharedPreferencesHelper.LAST_PASSWORD_TAG, "")

            if (login != null && password != null) {
                lifecycleScope.launch {
                    val res = authForToken(this@InitialActivity, login, password)
                    res.onSuccess { token ->
                        if (token == null) {
                            Toast.makeText(this@InitialActivity, "Ошибка получения токена", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val resUser = getUserInfo(this@InitialActivity, login, password, token)
                        resUser.onSuccess { user ->
                            if (user == null) {
                                Toast.makeText(this@InitialActivity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            MainStatic.currentUser = user
                            MainStatic.currentToken = token

                            val editor = preferences.edit()
                            editor.putString(SharedPreferencesHelper.LAST_LOGIN_TAG, login).apply()
                            editor.putString(SharedPreferencesHelper.LAST_PASSWORD_TAG, password).apply()
                            editor.putString(SharedPreferencesHelper.TOKEN_TAG, token).apply()
                            editor.putString(SharedPreferencesHelper.USER_ID_TAG, user.id).apply()
                            if (!preferences.contains(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG)) {
                                editor.putString(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG, "Recordings/Call").apply()
                            }

                            if (this@InitialActivity.intent.getBooleanExtra("IS_OLD_ENTER", true)) {
                                val intent = Intent(this@InitialActivity, MainActivity::class.java)
                                val lastEveningMessage = intent.getIntExtra("EVENING_MESSAGE", -1000000)
                                if (lastEveningMessage > 0) {
                                    intent.putExtra("EVENING_MESSAGE", (System.currentTimeMillis() / 1000).toInt())
                                }
                                startActivity(intent)
                                this@InitialActivity.finish()
                            } else {
                                this@InitialActivity.finish()
                            }

                        }
                        resUser.onCached { user ->
                            if (user == null) {
                                Toast.makeText(this@InitialActivity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            MainStatic.currentUser = user!!
                            MainStatic.currentToken = token!!

                            val editor = preferences.edit()
                            editor.putString(SharedPreferencesHelper.LAST_LOGIN_TAG, login).apply()
                            editor.putString(SharedPreferencesHelper.LAST_PASSWORD_TAG, password).apply()
                            editor.putString(SharedPreferencesHelper.TOKEN_TAG, token).apply()
                            editor.putString(SharedPreferencesHelper.USER_ID_TAG, user!!.id).apply()
                            if (!preferences.contains(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG)) {
                                editor.putString(SharedPreferencesHelper.CALLS_RECORDS_PATH_TAG, "Recordings/Call").apply()
                            }

                            val intent = Intent(this@InitialActivity, MainActivity::class.java)
                            startActivity(intent)
                            this@InitialActivity.finish()
                        }
                        resUser.onFailure {
                            Toast.makeText(this@InitialActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                    res.onFailure {
                        Toast.makeText(this@InitialActivity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                        this@InitialActivity.finish()
                        return@launch
                    }
                }
            }
        } else {
            this@InitialActivity.finish()
        }
    }
}