package com.example.pronedvizapp

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.authentication.AuthorizationFragment.Companion.getUserInfo
import com.example.pronedvizapp.authentication.RegistrationFragment
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.requests.models.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

const val PERMISSION_REQUEST_CODE_SUCCESS = 1
class InitialActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)

        preferences = this.getSharedPreferences("settings", Context.MODE_PRIVATE)

        checkPermission()
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

    private fun checkPermission() {
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
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.PROCESS_OUTGOING_CALLS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.FOREGROUND_SERVICE
        )
        val deniedPermissions = mutableListOf<String>()

        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)
            }
        }
        // Костыть для старых версий: при повышении minApiLevel - просто убрать
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            deniedPermissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (deniedPermissions.isNotEmpty()) {
            Log.w("MiaBox", "Denied permissions: ${deniedPermissions.toTypedArray()}")
            MaterialAlertDialogBuilder(this)
                .setTitle("Для полноценной работы приложения необходимо получить некоторые разрешения.")
                .setMessage("Пожалуйста, разрешите доступ к вашим геоданным, камере и уведомлениям для работы приложения")
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
        if (preferences.contains("LAST_LOGIN") && preferences.contains("LAST_PASSWORD")) {
            val login = preferences.getString("LAST_LOGIN", "")
            val password = preferences.getString("LAST_PASSWORD", "")

            if (login != null && password != null) {
                var userFromServer: User? = null
                var token: String? = null
                lifecycleScope.launch {
                    val res = RegistrationFragment.Companion.authForToken(this@InitialActivity, login, password)
                    res.onSuccess {
                        token = it
                        if (token == null) {
                            Toast.makeText(this@InitialActivity, "Ошибка получения токена", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val resUser = getUserInfo(this@InitialActivity, token!!)
                        resUser.onSuccess {
                            userFromServer = it

                            if (userFromServer == null) {
                                Toast.makeText(this@InitialActivity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            if (!(userFromServer!!.login == login && userFromServer!!.password == password)) {
                                Toast.makeText(this@InitialActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            MainStatic.currentUser = userFromServer
                            MainStatic.currentToken = token

                            val editor = preferences.edit()
                            editor.putString("LAST_LOGIN", login).apply()
                            editor.putString("LAST_PASSWORD", password).apply()
                            editor.putString("TOKEN", token).apply()
                            editor.putInt("USER_ID", userFromServer!!.id).apply()
                            if (!preferences.contains("RECORDINGS_PATH")) {
                                editor.putString("RECORDINGS_PATH", "Recordings/Call").apply()
                            }

                            if(this@InitialActivity.intent.getBooleanExtra("IS_OLD_ENTER", true)) {
                                val intent = Intent(this@InitialActivity, MainActivity::class.java)
                                startActivity(intent)
                                this@InitialActivity.finish()
                            } else {
                                this@InitialActivity.finish()
                            }

                        }
                        resUser.onFailure {
                            userFromServer = null
                            Toast.makeText(this@InitialActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                    res.onFailure {
                        token = null
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