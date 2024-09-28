package com.example.pronedvizapp.databases.models

import com.google.gson.annotations.SerializedName


data class StatisticChangeOptionInfo(
    @SerializedName("field_name")
    val fieldName: String,
    @SerializedName("add_value")
    val addValue: Int
)
