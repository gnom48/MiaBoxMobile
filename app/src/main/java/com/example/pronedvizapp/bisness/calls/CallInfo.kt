package com.example.pronedvizapp.bisness.calls

data class CallInfo(
    var lengthSeconds: Int,
    var callerName: String,
    var dateTime: Int,
    var transcription: String,
    var phoneNumber: String,
    var userId: String,
    var recordId: String,
    var callType: Int
)