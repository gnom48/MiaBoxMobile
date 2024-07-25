package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.AddresInfo
import com.example.pronedvizapp.requests.models.Note
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApiAddress {

    @GET("address/get_address_info_by_user_id")
    fun getUserAddressesByPeriod(
        @Query("user_id") userId: Int,
        @Query("date_start") dateStart: Int?,
        @Query("date_end") dateEnd: Int?,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<List<AddresInfo>>

    @GET("address/get_address_info_by_user_id")
    fun getAllUserAddresses(
        @Query("user_id") userId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<List<AddresInfo>>

    @POST("address/add_address_info")
    fun addAddressInfo(
        @Body addressInfo: AddresInfo,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Int?>

}