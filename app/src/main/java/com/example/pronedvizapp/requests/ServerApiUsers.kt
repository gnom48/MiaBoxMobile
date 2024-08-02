package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Image
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.StatisticsWithKpi
import com.example.pronedvizapp.requests.models.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    @PUT("/user/statistics/update")
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

    @GET("/user/statistics/get_kpi")
    fun getStatisticsWithKpi(
        @Header("token-authorization") tokenAuthorization: String
    ): Call<StatisticsWithKpi?>

    @Multipart
    @POST("/user/set_image_file")
    suspend fun setImageFileToUser(
        @Part file: MultipartBody.Part,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Int?>

    @GET("/user/get_image_file")
    suspend fun getImageFileByUser(
        @Header("token-authorization") tokenAuthorization: String
    ): Response<ResponseBody>

}