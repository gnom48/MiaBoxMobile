package com.example.pronedvizapp.bisness

import android.content.Context

class SharedPreferencesHelper(private val context: Context) {
    companion object{
        const val SETTINGS_PREFS_KEY = "settings"

        const val LAST_LOGIN_TAG = "LAST_LOGIN"
        const val LAST_PASSWORD_TAG = "LAST_PASSWORD"
        const val TOKEN_TAG = "TOKEN"
        const val USER_ID_TAG = "USER_ID"
        const val CALLS_RECORDS_PATH_TAG = "RECORDINGS_PATH"
        const val EVENING_MESSAGE_TAG = "EVENING_MESSAGE"
        const val LAST_GEO_POINT_UNIX_PREF_TAG = "LAST_GEO_POINT_UNIX"
    }

    fun saveStringData(key: String, data: String?) {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFS_KEY, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key,data).apply()
    }

    fun getStringData(key: String): String? {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_PREFS_KEY, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }
}