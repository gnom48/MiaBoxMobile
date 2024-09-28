package com.example.pronedvizapp.bisness.calls

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.adapters.CallsInGroupAdapter
import com.example.pronedvizapp.requests.models.UserCall

object CallsHelper {
    fun getContactName(phoneNumber: String, context: Context): String {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            var contactName: String = CallRecordingService.UNKNOWN_CALLER
            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (index < 0) {
                        return contactName
                    }
                    contactName = cursor.getString(index)
                }
                cursor.close()
            }

            return contactName
        } catch (e: Exception) {
            return CallRecordingService.UNKNOWN_CALLER
        }
    }

    @SuppressLint("Range")
    fun getLocalCalls(context: Context): List<UserCall> {
        val callLogEntries = mutableListOf<UserCall>()
        val cursor: Cursor? = context.contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC")

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val callLogEntry = UserCall(
                        userId = MainStatic.currentUser!!.id,
                        recordId = "",
                        info = LOCAL_CALL_SOURCE,
                        dateTime = it.getColumnIndex(CallLog.Calls.DATE).toInt(),
                        phoneNumber = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER)),
                        contactName = getContactName(it.getString(it.getColumnIndex(CallLog.Calls.NUMBER)), context),
                        lengthSeconds = it.getLong(it.getColumnIndex(CallLog.Calls.DURATION)).toInt(),
                        callType = 0,
                        transcription = CallsInGroupAdapter.NO_TRANSCRIPTION
                    )
                    callLogEntries.add(callLogEntry)
                } while (it.moveToNext())
            }
        }

        return callLogEntries
    }

    const val LOCAL_CALL_SOURCE = "из журнала вызовов устройства"
    const val SERVER_CALL_SOURCE = "с сервера"

}