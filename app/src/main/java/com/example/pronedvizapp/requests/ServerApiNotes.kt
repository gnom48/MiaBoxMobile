package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Note
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ServerApiNotes {

    @GET("note/all")
    fun getAllNotes(
        @Header("token-authorization") tokenAuthorization: String
    ): Call<List<Note>>

    @POST("note/add")
    fun addNote(
        @Body note: Note,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Int?>

    @DELETE("note/delete")
    fun deleteNote(
        @Query("note_id") noteId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Void>

    @PUT("note/edit")
    fun editNote(
        @Body note: Note,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Void>

}