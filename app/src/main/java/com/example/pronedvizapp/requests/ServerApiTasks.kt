package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Task
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApiTasks {

    @GET("task/all")
    fun getAllTasks(
        @Header("token-authorization") tokenAuthorization: String
    ): Call<List<Task>>

    @POST("task/add")
    fun addTask(
        @Body task: Task,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Int?>

    @DELETE("task/delete")
    fun deleteTask(
        @Query("task_id") taskId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Void>

}