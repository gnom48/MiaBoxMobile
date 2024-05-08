package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ServerApiUsers {

    @POST("user/registration")
    fun registration(
        @Body user: User
    ): Call<Int>

    @GET("/user/authorization")
    fun authorization(
        @Query("login") login: String,
        @Query("password") password: String
    ): Call<String?>

    @GET("/user/info")
    fun info(
        @Header("token-authorization") tokenAuthorization: String
    ): Call<User?>

    @PUT("/user/edit")
    fun editUserProfile(
        @Body user: User,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Boolean?>

    @GET("/user/statistics/update")
    fun updateStatistics(
        @Query("statistic") statisticName: String,
        @Query("addvalue") addValue: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Boolean>

    @GET("/user/statistics/get")
    fun getStatisticsByPeriod(
        @Query("period") period: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Statistics?>

}