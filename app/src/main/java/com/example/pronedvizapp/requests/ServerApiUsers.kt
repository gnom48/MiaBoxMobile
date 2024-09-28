package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.AuthData
import com.example.pronedvizapp.requests.models.Kpi
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.User
import okhttp3.MultipartBody
import okhttp3.ResponseBody
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
    suspend fun registration(
        @Body user: User
    ): Response<String>

    @POST("/user/authorization_secure")
    suspend fun authorizationSecure(
        @Body authData: AuthData
    ): Response<String?>

    @GET("/user/info")
    suspend fun info(
        @Header("token-authorization") tokenAuthorization: String
    ): Response<User?>

    @PUT("/user/edit")
    suspend fun editUserProfile(
        @Body user: User,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean?>

    @PUT("/user/statistics/update")
    suspend fun updateStatisticsAsync(
        @Query("statistic") statisticName: String,
        @Query("addvalue") addValue: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>

    @GET("/user/statistics/get")
    suspend fun getStatisticsByPeriod(
        @Query("period") period: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Statistics?>

    @GET("/user/statistics/get_kpi")
    suspend fun getStatisticsWithKpi(
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Kpi?>

    @Multipart
    @POST("/user/set_image_file")
    suspend fun setImageFileToUser(
        @Part file: MultipartBody.Part,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<String?>

    @GET("/user/get_image_file")
    suspend fun getImageFileByUser(
        @Query("user_id") userId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<ResponseBody>

}