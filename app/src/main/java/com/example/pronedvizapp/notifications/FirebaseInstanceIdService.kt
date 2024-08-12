package com.example.pronedvizapp.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService



class FirebaseInstanceIdService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(DEBUG_TAG, "Refreshed token: $token")
    }

    companion object {
        const val DEBUG_TAG = "FirebaseMessagingService"
    }
}