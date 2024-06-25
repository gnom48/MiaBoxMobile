package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.AddresInfo
import com.example.pronedvizapp.requests.models.Note
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ServerApiAddress {

    @GET("address/get_address_info_by_user_id")
    fun getAllUserAddresses(
        @Header("token-authorization") tokenAuthorization: String
    ): Call<List<AddresInfo>>

    @POST("address/add_address_info")
    fun addAddressInfo(
        @Body addressInfo: AddresInfo,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Int?>

}