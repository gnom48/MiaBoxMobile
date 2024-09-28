package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.AddressInfo
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApiAddress {

    @GET("address/get_address_info_by_user_id")
    suspend fun getUserAddressesByPeriod(
        @Query("user_id") userId: String,
        @Query("date_start") dateStart: Int?,
        @Query("date_end") dateEnd: Int?,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<List<AddressInfo>>

    @GET("address/get_address_info_by_user_id")
    fun getUserAddressesByPeriodSync(
        @Query("user_id") userId: String,
        @Query("date_start") dateStart: Int?,
        @Query("date_end") dateEnd: Int?,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<List<AddressInfo>>

    @GET("address/get_address_info_by_user_id")
    suspend fun getAllUserAddresses(
        @Query("user_id") userId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<List<AddressInfo>>

    @POST("address/add_address_info")
    suspend fun addAddressInfo(
        @Body addressInfo: AddressInfo,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<String?>
}