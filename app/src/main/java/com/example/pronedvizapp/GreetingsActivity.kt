package com.example.pronedvizapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pronedvizapp.authentication.AuthenticationActivity
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.databases.DbViewModel


class GreetingsActivity : AppCompatActivity() {
    private val preferences: SharedPreferences by lazy { getSharedPreferences(SharedPreferencesHelper.SETTINGS_PREFS_KEY, Context.MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainStatic.dbViewModel = DbViewModel(this.application)

        val intent = Intent(this, AuthenticationActivity::class.java)
        startActivity(intent)
        finish()
    }
}