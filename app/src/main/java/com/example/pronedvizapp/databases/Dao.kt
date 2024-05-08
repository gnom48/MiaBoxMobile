package com.example.pronedvizapp.databases

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.pronedvizapp.databases.models.ActiveWorkOrm
import com.example.pronedvizapp.databases.models.NoteOrm
import com.example.pronedvizapp.databases.models.UserOrm

@Dao
interface Dao {

    @Query("SELECT * FROM notes WHERE user_id = :id")
    fun getAllNotes(id: Long): LiveData<List<NoteOrm>>

    @Insert
    suspend fun insertNote(item: NoteOrm)

    @Delete
    suspend fun deleteNote(item: NoteOrm)



    @Query("SELECT * FROM users WHERE login = :login")
    fun getUserByLogin(login: String): LiveData<UserOrm>

    @Insert
    suspend fun insertUser(item: UserOrm)



    @Query("SELECT * FROM active_works WHERE user_id = :id")
    fun getActualWorks(id: Long): LiveData<List<ActiveWorkOrm>>

    @Insert
    suspend fun insertWork(item: ActiveWorkOrm)

    @Delete
    suspend fun deleteWork(item: ActiveWorkOrm)
}