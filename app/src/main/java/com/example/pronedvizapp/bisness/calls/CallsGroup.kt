package com.example.pronedvizapp.bisness.calls

import java.time.LocalDate

data class CallsGroup(
    var date: LocalDate,
    var calls: List<CallInfo>
)