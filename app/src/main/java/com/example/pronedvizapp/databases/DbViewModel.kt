package com.example.pronedvizapp.databases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.pronedvizapp.databases.models.ActiveWorkOrm
import com.example.pronedvizapp.databases.models.NoteOrm
import com.example.pronedvizapp.databases.models.UserOrm
import com.example.pronedvizapp.requests.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DbViewModel(application: Application): AndroidViewModel(application) {

    private val repository: Repository

    init {
        val dao = LocalDb.getDb(application).getDao()
        repository = Repository(dao)
    }



    fun getUserByLogin(login: String): LiveData<UserOrm>? {
        var tmp: LiveData<UserOrm>? = null
        viewModelScope.launch(Dispatchers.IO) {
             tmp = repository.getUserBuLogin(login)
        }
        return tmp
    }

    fun insertUser(user: UserOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUser(user)
        }
    }



    fun getNotes(id: Long): LiveData<List<NoteOrm>>? {
        var tmp: LiveData<List<NoteOrm>>? = null
        viewModelScope.launch(Dispatchers.IO) {
             tmp = repository.getNotes(id)
        }
        return tmp
    }

    fun insertNote(item: NoteOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(item)
        }
    }

    fun deleteNote(item: NoteOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(item)
        }
    }



    fun getActiveWorks(id: Long): LiveData<List<ActiveWorkOrm>>? {
        var tmp: LiveData<List<ActiveWorkOrm>>? = null
        viewModelScope.launch(Dispatchers.IO) {
             tmp = repository.getActualWorks(id)
        }
        return tmp
    }

    fun insertActiveWork(item: ActiveWorkOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWork(item)
        }
    }

    fun deleteActiveWork(item: ActiveWorkOrm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWork(item)
        }
    }




}