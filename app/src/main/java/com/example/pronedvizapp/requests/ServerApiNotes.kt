package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Note
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ServerApiNotes {

    @GET("note/all")
    suspend fun getAllNotes(
        @Header("token-authorization") tokenAuthorization: String
    ): Response<List<Note>?>

    @POST("note/add")
    suspend fun addNote(
        @Body note: Note,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<String?>

    @DELETE("note/delete")
    fun deleteNoteSync(
        @Query("note_id") noteId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Void>

    @DELETE("note/delete")
    suspend fun deleteNoteAsync(
        @Query("note_id") noteId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>

    @PUT("note/edit")
    suspend fun editNote(
        @Body note: Note,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>

}