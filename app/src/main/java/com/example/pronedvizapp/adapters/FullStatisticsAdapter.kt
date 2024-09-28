package com.example.pronedvizapp.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FullStatisticsCardBinding
import com.example.pronedvizapp.requests.models.CompletedTasks
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.MONTH_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.StatisticsPeriods
import com.example.pronedvizapp.requests.models.WEEK_STATISTICS_PERIOD

class FullStatisticsAdapter(private val dataSource: StatisticsPeriods?, private val completedTasks: CompletedTasks, val context: Context): RecyclerView.Adapter<FullStatisticsAdapter.InfoViewHolder>() {

    private lateinit var dataArray:  ArrayList<Statistics>
    private val associativeMap: Map<Int, String> = mapOf(0 to "День", 1 to "Неделя", 2 to "Месяц")
    private val periodsMap: Map<Int, String> = mapOf(0 to DAY_STATISTICS_PERIOD, 1 to WEEK_STATISTICS_PERIOD, 2 to MONTH_STATISTICS_PERIOD)

    inner class InfoViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: FullStatisticsCardBinding = FullStatisticsCardBinding.bind(view)

        fun bind(item: Statistics, title: String, filterPeriod: String) {
            binding.periodTextView.text = title

            binding.analyticsCountTextView.text = item.analytics.toString()
            binding.callsCountTextView.text = item.calls.toString()
            binding.showCountTextView.text = item.shows.toString()
            binding.flyersCountTextView.text = item.flyers.toString()
            binding.searchCountTextView.text = item.searches.toString()
            binding.dealsCountTextView.text = (item.dealsRent + item.dealsSale).toString()
            binding.subCountDealsRentTextView.text = item.dealsRent.toString()
            binding.subCountDealsSaleTextView.text = item.dealsSale.toString()
            binding.meetsCountTextView.text = item.meets.toString()
            binding.depositsCountTextView.text = item.deposits.toString()
            binding.contrCountTextView.text = (item.regularContracts + item.exclusiveContracts).toString()
            binding.subCountContrRegularTextView.text = item.regularContracts.toString()
            binding.subCountContrExcTextView.text = item.exclusiveContracts.toString()
            binding.othersCountTextView.text = item.others.toString()

            binding.showCompletedCustomeTasksImageButton.setOnClickListener {
                if (binding.completedTasksListView.visibility == View.GONE) {
                    binding.completedTasksListView.visibility = View.VISIBLE
                    binding.completedTasksListView.adapter = CompletedTasksAdapter(context, CompletedTasksAdapter.filterTasksForToday(completedTasks, filterPeriod))

                    val params = binding.completedTasksListView.layoutParams
                    params.height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        CompletedTasksAdapter.ITEM_HEIGHT * binding.completedTasksListView.adapter.count, context.resources.displayMetrics).toInt()
                    binding.completedTasksListView.layoutParams = params
                } else {
                    binding.completedTasksListView.visibility = View.GONE
                }
                binding.showCompletedCustomeTasksImageButton.rotation += 180F
            }

            binding.completedTasksListView.adapter = CompletedTasksAdapter(context, CompletedTasksAdapter.filterTasksForToday(completedTasks, filterPeriod))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.full_statistics_card, parent, false)
        dataArray = dataSource?.let { arrayListOf<Statistics>(it.day, dataSource.week, dataSource.month) } ?: arrayListOf()
        return InfoViewHolder(view)
    }

    override fun getItemCount(): Int = 3

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        associativeMap[position].let { holder.bind(dataArray[position], it!!, periodsMap[position]!!) }
    }
}