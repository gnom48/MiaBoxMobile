package com.example.pronedvizapp

import androidx.lifecycle.MutableLiveData
import com.example.pronedvizapp.databases.DbViewModel
import com.example.pronedvizapp.requests.models.User

object MainStatic {
    lateinit var currentUser: User
    lateinit var currentToken: String
    lateinit var dbViewModel: DbViewModel
    val isCurrentOnline = MutableLiveData<Boolean>(true)
}