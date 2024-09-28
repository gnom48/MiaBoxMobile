package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.IStatistic
import com.example.pronedvizapp.requests.models.WorkTasksTypes

fun IStatistic.editStatisticFieldByName(columnName: String, addValue: Int) {
    when(columnName) {
        WorkTasksTypes.FLYERS.description -> { this.flyers += addValue }
        WorkTasksTypes.CALLS.description -> { this.calls += addValue }
        WorkTasksTypes.SHOW.description -> { this.shows += addValue }
        WorkTasksTypes.MEET.description -> { this.meets += addValue }
        WorkTasksTypes.DEAL_RENT.description -> { this.dealsRent += addValue }
        WorkTasksTypes.DEAL_SALE.description -> { this.dealsSale += addValue }
        WorkTasksTypes.DEPOSIT.description -> { this.deposits += addValue }
        WorkTasksTypes.SEARCH.description -> { this.searches += addValue }
        WorkTasksTypes.ANALYTICS.description -> { this.analytics += addValue }
        WorkTasksTypes.REGULAR_CONTRACT.description -> { this.regularContracts += addValue }
        WorkTasksTypes.EXCLUSIVE_CONTRACT.description -> { this.exclusiveContracts += addValue }
        WorkTasksTypes.OTHER.description -> { this.others += addValue }
        else -> { }
    }
}