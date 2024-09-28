package com.example.pronedvizapp.databases

import com.example.pronedvizapp.databases.models.ICombinedPrimaryKey
import com.example.pronedvizapp.databases.models.IOrmVersion
import com.example.pronedvizapp.databases.models.getCurrentUnixSeconds
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

abstract class ModelsCaster {
    fun <T : Any> castByJsonTo(target: Class<T>): T {

        val gson: Gson = GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.IDENTITY)
            .create()

        val json = gson.toJson(this)
        val obj = gson.fromJson(json, target)

        if (obj is IOrmVersion) {
            obj.version = 1
            obj.whenLastUpdated = getCurrentUnixSeconds()
        }

        if (obj is ICombinedPrimaryKey) {
            obj.combinedKey = obj.getCombinedKeyValue()
        }

        return obj
    }
}