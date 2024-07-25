package com.example.pronedvizapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.calls.CallInfo
import com.example.pronedvizapp.databinding.CallListviewItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallsInGroupAdapter(private val context: Context, private val calls: List<CallInfo>) : BaseAdapter() {

    override fun getCount(): Int = calls.size

    override fun getItem(position: Int): CallInfo = calls[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.call_listview_item, parent, false)

        val binding = CallListviewItemBinding.bind(view)
        binding.phoneNumberTextView.text = calls[position].phoneNumber
        if (calls[position].callerName != null && calls[position].callerName != "Неизвестный") {
            binding.phoneNumberTextView.text = binding.phoneNumberTextView.text.toString() + "(${calls[position].phoneNumber})"
        }
        binding.dateTimeTextView.text = unixTimeToString(calls[position].dateTime)
        binding.lengthSecondsTextView.text = getFormattedTimeLength(calls[position].lengthSeconds)
        binding.transcriptionButtonTextView.setOnClickListener {
            it.isEnabled = false
            // запрос транскрипции
        }

        return binding.root
    }

    private fun getFormattedTimeLength(timeSeconds: Int): String {
        val minutes = (timeSeconds / 60F).toInt()
        val seconds = timeSeconds - minutes * 60
        return "$minutes мин. $seconds сек."
    }

    fun unixTimeToString(unixTime: Int): String {
        val date = Date(unixTime * 1000L)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

}