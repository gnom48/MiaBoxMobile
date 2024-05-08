package com.example.pronedvizapp.databases

import androidx.lifecycle.LiveData
import com.example.pronedvizapp.databases.models.ActiveWorkOrm
import com.example.pronedvizapp.databases.models.NoteOrm
import com.example.pronedvizapp.databases.models.UserOrm

class Repository(private val dao: Dao) {

    fun getUserBuLogin(login: String): LiveData<UserOrm> {
        return dao.getUserByLogin(login)
    }

    suspend fun insertUser(user: UserOrm) {
        dao.insertUser(user)
    }



    fun getActualWorks(id: Long): LiveData<List<ActiveWorkOrm>> {
        return dao.getActualWorks(id)
    }

    suspend fun insertWork(item: ActiveWorkOrm) {
        dao.insertWork(item)
    }

    suspend fun deleteWork(item: ActiveWorkOrm) {
        dao.deleteWork(item)
    }



    fun getNotes(id: Long): LiveData<List<NoteOrm>> {
        return dao.getAllNotes(id)
    }

    suspend fun insertNote(item: NoteOrm) {
        dao.insertNote(item)
    }

    suspend fun deleteNote(item: NoteOrm) {
        dao.deleteNote(item)
    }

}