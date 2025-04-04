package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.AddressResponse
import com.example.pronedvizapp.requests.models.Coordinates
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DadataApi {
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
        "Authorization: Token 63fc0220f7a8411af1ec225a49ee35103be869c5"
    )
    @POST("4_1/rs/geolocate/address")
    suspend fun getAddressByCoordinates(@Body coordinates: Coordinates): Response<AddressResponse>
}