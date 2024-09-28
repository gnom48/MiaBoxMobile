package com.example.pronedvizapp.bisness

class DateTimeDuration(
    private var _years: Int = 0,
    private var _months: Int = 0,
    private var _days: Int = 0,
    private var _hours: Int = 0,
    private var _minutes: Int = 0,
    private var _seconds: Int = 0
) {
    val years: Int
        get() = _years

    val months: Int
        get() = _months

    val days: Int
        get() = _days

    val hours: Int
        get() = _hours

    val minutes: Int
        get() = _minutes

    val seconds: Int
        get() = _seconds

    companion object {
        fun ofHours(hours: Int): DateTimeDuration {
            val days = hours / 24
            val remainingHours = hours % 24
            return DateTimeDuration(_days = days, _hours = remainingHours)
        }

        fun ofSeconds(seconds: Long): DateTimeDuration {
            val years = seconds / (365L * 24 * 60 * 60)
            val remainingSecondsAfterYears = seconds % (365L * 24 * 60 * 60)
            val months = remainingSecondsAfterYears / (30L * 24 * 60 * 60)
            val remainingSecondsAfterMonths = remainingSecondsAfterYears % (30L * 24 * 60 * 60)
            val days = remainingSecondsAfterMonths / (24L * 60 * 60)
            val remainingSecondsAfterDays = remainingSecondsAfterMonths % (24L * 60 * 60)
            val hours = remainingSecondsAfterDays / (60L * 60)
            val remainingSecondsAfterHours = remainingSecondsAfterDays % (60L * 60)
            val minutes = remainingSecondsAfterHours / 60
            val remainingSecondsAfterMinutes = remainingSecondsAfterHours % 60

            return DateTimeDuration(
                _years = years.toInt(),
                _months = months.toInt(),
                _days = days.toInt(),
                _hours = hours.toInt(),
                _minutes = minutes.toInt(),
                _seconds = remainingSecondsAfterMinutes.toInt()
            )
        }
    }

    fun plusMinutes(minutes: Int): DateTimeDuration {
        val totalMinutes = this._minutes + minutes
        val hours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        val days = hours / 24
        val remainingHours = hours % 24
        return DateTimeDuration(
            _days = this._days + days,
            _hours = this._hours + remainingHours,
            _minutes = remainingMinutes
        )
    }

    fun toSeconds(): Long = _years * 365L * 24 * 60 * 60 +
                _months * 30L * 24 * 60 * 60 +
                _days * 24L * 60 * 60 +
                _hours * 60L * 60 +
                _minutes * 60L +
                _seconds

    fun toMillis(): Long = toSeconds() * 1000
}