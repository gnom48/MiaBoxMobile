package com.example.pronedvizapp

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.adapters.CompletedTasksAdapter
import com.example.pronedvizapp.databinding.ActivityResultsBinding
import com.example.pronedvizapp.requests.RequestsRepository.getAllTasksCurrentUser
import com.example.pronedvizapp.requests.RequestsRepository.getUserStatisticsByPeriod
import com.example.pronedvizapp.requests.RequestsRepository.getUserStatisticsWithKpi
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.CompletedTasks
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.MONTH_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.WEEK_STATISTICS_PERIOD
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ResultsActivity : AppCompatActivity() {

    lateinit var binding: ActivityResultsBinding
    private var cachedStatistics: HashMap<String, Any> = HashMap<String, Any>()
    private var completedTasks: CompletedTasks = CompletedTasks(mutableListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val userStatics = getUserStatisticsWithKpi(this@ResultsActivity, MainStatic.currentToken!!)
            userStatics.onSuccess { kpi ->
                binding.kpiLevelTextView.text = kpi.userLevel
                kpi.currentMonthKpi?.let {
                    binding.currentKpiTextView.text = it.toString()
                }
                kpi.lastMonthKpi?.let {
                    binding.lastMonthKpiTextView.text = it.salaryPercentage.toString()
                    binding.countContrTextView.text = (it.regularContracts + it.exclusiveContracts).toString()
                    binding.subCountContrRegularTextView.text = it.regularContracts.toString()
                    binding.subCountContrExcTextView.text = it.exclusiveContracts.toString()
                }
                cachedStatistics["KPI"] = kpi

                val userCompletedTasks = getAllTasksCurrentUser(this@ResultsActivity,  MainStatic.currentUser!!.id, MainStatic.currentToken!!, true)
                userCompletedTasks.onSuccess { tasks ->
                    completedTasks = CompletedTasks(tasks as MutableCollection<Task>)
                    binding.completedTasksListView.adapter = CompletedTasksAdapter(this@ResultsActivity, CompletedTasksAdapter.filterTasksForToday(completedTasks, DAY_STATISTICS_PERIOD))
                }
                userCompletedTasks.onCached { tasks ->
                    completedTasks = CompletedTasks(tasks as MutableCollection<Task>)
                    binding.completedTasksListView.adapter = CompletedTasksAdapter(this@ResultsActivity, CompletedTasksAdapter.filterTasksForToday(completedTasks, DAY_STATISTICS_PERIOD))
                }
                userCompletedTasks.onFailure {
                    completedTasks = CompletedTasks(mutableListOf())
                    Toast.makeText(this@ResultsActivity, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.linearLayout6.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.dayButton.id -> {
                        updateButtonStyles(binding.dayButton, binding.weekButton, binding.monthButton)
                        showResultsByPeriod(DAY_STATISTICS_PERIOD)
                        binding.completedTasksListView.adapter = CompletedTasksAdapter(this, CompletedTasksAdapter.filterTasksForToday(completedTasks, DAY_STATISTICS_PERIOD))

                        val params = binding.completedTasksListView.layoutParams
                        params.height = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            CompletedTasksAdapter.ITEM_HEIGHT * binding.completedTasksListView.adapter.count, resources.displayMetrics).toInt()
                        binding.completedTasksListView.layoutParams = params
                    }
                    binding.weekButton.id -> {
                        updateButtonStyles(binding.weekButton, binding.dayButton, binding.monthButton)
                        showResultsByPeriod(WEEK_STATISTICS_PERIOD)
                        binding.completedTasksListView.adapter = CompletedTasksAdapter(this, CompletedTasksAdapter.filterTasksForToday(completedTasks, WEEK_STATISTICS_PERIOD))

                        val params = binding.completedTasksListView.layoutParams
                        params.height = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            CompletedTasksAdapter.ITEM_HEIGHT * binding.completedTasksListView.adapter.count, resources.displayMetrics).toInt()
                        binding.completedTasksListView.layoutParams = params
                    }
                    binding.monthButton.id -> {
                        updateButtonStyles(binding.monthButton, binding.dayButton, binding.weekButton)
                        showResultsByPeriod(MONTH_STATISTICS_PERIOD)
                        binding.completedTasksListView.adapter = CompletedTasksAdapter(this, CompletedTasksAdapter.filterTasksForToday(completedTasks, MONTH_STATISTICS_PERIOD))

                        val params = binding.completedTasksListView.layoutParams
                        params.height = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            CompletedTasksAdapter.ITEM_HEIGHT * binding.completedTasksListView.adapter.count, resources.displayMetrics).toInt()
                        binding.completedTasksListView.layoutParams = params
                    }
                    else -> { }
                }
            }
        }

        binding.showCompletedCustomeTasksImageButton.setOnClickListener {
            binding.showCompletedCustomeTasksImageButton.rotation += 180F
            if (binding.completedTasksListView.visibility == View.GONE) {
                binding.completedTasksListView.visibility = View.VISIBLE
                binding.linearLayout6.check(binding.linearLayout6.checkedButtonId)
            } else {
                binding.completedTasksListView.visibility = View.GONE
            }
        }

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        binding.showAllLinearLayout.setOnClickListener {
            binding.showAllLinearLayout.visibility = View.GONE
            binding.dopStatisticsLinearLayout.visibility = View.VISIBLE
        }

        binding.hideDopLinearLayout.setOnClickListener {
            binding.showAllLinearLayout.visibility = View.VISIBLE
            binding.dopStatisticsLinearLayout.visibility = View.GONE
        }

        showResultsByPeriod(DAY_STATISTICS_PERIOD)
    }

    override fun onResume() {
        super.onResume()
        binding.linearLayout6.check(R.id.dayButton)
        updateButtonStyles(binding.dayButton, binding.weekButton, binding.monthButton)
    }

    private fun updateButtonStyles(selectedButton: MaterialButton, unselectedButton1: MaterialButton, unselectedButton2: MaterialButton) {
        selectedButton.setTextColor(resources.getColor(android.R.color.black))
        selectedButton.backgroundTintList = resources.getColorStateList(android.R.color.white)

        unselectedButton1.setTextColor(resources.getColor(android.R.color.white))
        unselectedButton1.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.transparent0))
        unselectedButton1.strokeColor = resources.getColorStateList(android.R.color.white)
        unselectedButton1.strokeWidth = 2
        unselectedButton2.setTextColor(resources.getColor(android.R.color.white))
        unselectedButton2.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.transparent0))
        unselectedButton2.strokeColor = resources.getColorStateList(android.R.color.white)
        unselectedButton2.strokeWidth = 2
    }

    private fun showResultsByPeriod(period: String) {
        if (cachedStatistics[period] != null) {
            bindValuesToTextViews(cachedStatistics[period] as Statistics, period)
            return
        }
        lifecycleScope.launch {
            val userStatics = getUserStatisticsByPeriod(period, this@ResultsActivity, MainStatic.currentToken!!)
            userStatics.onSuccess { stats ->
                bindValuesToTextViews(stats, period)
                cachedStatistics[period] = stats
            }
            userStatics.onCached { stats ->
                usingLocalDataToast(this@ResultsActivity)
                bindValuesToTextViews(stats, period)
                cachedStatistics[period] = stats
            }
        }
    }

    private fun bindValuesToTextViews(stat: Statistics, period: String) {
        binding.countAnalyticsTextView.text = stat.analytics.toString()
        binding.countCallsTextView.text = stat.calls.toString()
        binding.countFlyersTextView.text = stat.flyers.toString()
        binding.countDealsTextView.text = (stat.dealsSale + stat.dealsRent).toString()
        binding.subCountDealsSaleTextView.text = stat.dealsSale.toString()
        binding.subCountDealsRentTextView.text = stat.dealsRent.toString()
        binding.countContrTextView.text = (stat.regularContracts + stat.exclusiveContracts).toString()
        binding.subCountContrRegularTextView.text = stat.regularContracts.toString()
        binding.subCountContrExcTextView.text = stat.exclusiveContracts.toString()
        binding.countMeetsTextView.text = stat.meets.toString()
        binding.countDepositsTextView.text = stat.deposits.toString()
        binding.countSearchTextView.text = stat.searches.toString()
        binding.countShowsTextView.text = stat.shows.toString()
        binding.countOthersTextView.text = stat.others.toString()

        binding.completedTasksListView.adapter = CompletedTasksAdapter(this@ResultsActivity, CompletedTasksAdapter.filterTasksForToday(completedTasks, period))
    }
}