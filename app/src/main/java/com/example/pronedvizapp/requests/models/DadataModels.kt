package com.example.pronedvizapp.requests.models

import com.google.gson.annotations.SerializedName

data class Coordinates(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)

data class AddressResponse(
    @SerializedName("suggestions") val suggestions: List<AddressSuggestion>
)

data class AddressSuggestion(
    @SerializedName("value") val value: String,
    @SerializedName("unrestricted_value") val unrestrictedValue: String,
    @SerializedName("data") val data: AddressData
)

data class AddressData(
    val value: String?
)