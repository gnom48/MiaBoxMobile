package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Task
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApiTasks {

    @GET("task/all")
    fun getAllTasksSync(
        @Header("token-authorization") tokenAuthorization: String
    ): Response<List<Task>>

    @GET("task/all")
    suspend fun getAllTasksAsync(
        @Header("token-authorization") tokenAuthorization: String
    ): Response<List<Task>>

    @GET("task/completed")
    suspend fun getAllCompletedTasks(
        @Query("user_id") userId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<List<Task>>

    @POST("task/add")
    suspend fun addTask(
        @Body task: Task,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<String?>

    @DELETE("task/delete")
    fun deleteTaskSync(
        @Query("task_id") taskId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Void>

    @DELETE("task/delete")
    suspend fun deleteTaskAsync(
        @Query("task_id") taskId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>
}