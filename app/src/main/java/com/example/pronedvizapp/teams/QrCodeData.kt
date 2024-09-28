package com.example.pronedvizapp.teams

import android.content.Context
import android.util.Base64
import com.example.pronedvizapp.R
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class QrCodeData(
    @SerializedName("team_id")
    var teamId: String,
    @SerializedName("author_id")
    var authorId: String,
    @SerializedName("qr_code_datetime")
    var qrCodeDatetime: Long,
    @SerializedName("secret")
    val secret: String
) : Serializable

public fun encodeSecret(context: Context): String {
    return Base64.encodeToString(
        context.getString(R.string.secret).toByteArray(),
        Base64.DEFAULT
    ).trim()
}