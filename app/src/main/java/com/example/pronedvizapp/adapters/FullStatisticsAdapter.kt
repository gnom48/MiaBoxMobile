package com.example.pronedvizapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FullStatisticsCardBinding
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.StatisticsPeriods
import com.example.pronedvizapp.requests.models.UserTeamsWithInfoItem

class FullStatisticsAdapter(val dataSource: StatisticsPeriods, val context: Context): RecyclerView.Adapter<FullStatisticsAdapter.InfoViewHolder>() {

    lateinit var dataArray:  ArrayList<Statistics>
    val assotiativeMap: Map<Int, String> = mapOf(0 to "День", 1 to "Неделя", 2 to "Месяц")

    inner class InfoViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: FullStatisticsCardBinding = FullStatisticsCardBinding.bind(view)

        fun bind(item: Statistics, title: String) {
            binding.periodTextView.text = title

            binding.analyticsCountTextView.text = item.analytics.toString()
            binding.callsCountTextView.text = item.calls.toString()
            binding.showCountTextView.text = item.shows.toString()
            binding.flyersCountTextView.text = item.flyers.toString()
            binding.searchCountTextView.text = item.searches.toString()
            binding.dealsCountTextView.text = item.deals.toString()
            binding.meetsCountTextView.text = item.meets.toString()
            binding.depositsCountTextView.text = item.deposits.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.full_statistics_card, parent, false)
        dataArray = arrayListOf<Statistics>(dataSource.day, dataSource.week, dataSource.month)
        return InfoViewHolder(view)
    }

    override fun getItemCount(): Int = 3

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        assotiativeMap[position].let { holder.bind(dataArray[position], it!!) }
    }
}