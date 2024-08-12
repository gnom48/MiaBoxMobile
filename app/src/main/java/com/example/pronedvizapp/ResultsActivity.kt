package com.example.pronedvizapp

import android.content.Context
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.databinding.ActivityResultsBinding
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.IStatistic
import com.example.pronedvizapp.requests.models.Kpi
import com.example.pronedvizapp.requests.models.MONTH_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.StatisticsWithKpi
import com.example.pronedvizapp.requests.models.WEEK_STATISTICS_PERIOD
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory

class ResultsActivity : AppCompatActivity() {

    lateinit var binding: ActivityResultsBinding
    private var cachedStatistics: HashMap<String, IStatistic> = HashMap<String, IStatistic>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val userStatics = getUserStatisticsWithKpi(this@ResultsActivity, MainStatic.currentToken!!)
            userStatics.onSuccess {
                binding.kpiLevelTextView.text = it.userLevel
                binding.kpiTextView.text = it.currentMonthKpi.toString()
                cachedStatistics["KPI"] = it
            }
        }

//        binding.dayButton.isSelected = true

        binding.linearLayout6.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.dayButton.id -> {
                        updateButtonStyles(binding.dayButton, binding.weekButton, binding.monthButton)
                        showResultsByPeriod(DAY_STATISTICS_PERIOD)
                    }
                    binding.weekButton.id -> {
                        updateButtonStyles(binding.weekButton, binding.dayButton, binding.monthButton)
                        showResultsByPeriod(WEEK_STATISTICS_PERIOD)
                    }
                    binding.monthButton.id -> {
                        updateButtonStyles(binding.monthButton, binding.dayButton, binding.weekButton)
                        showResultsByPeriod(MONTH_STATISTICS_PERIOD)
                    }
                    else -> { }
                }
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
        binding.linearLayout6.check(R.id.privateButton)
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
            bindValuesToTextViews(cachedStatistics[period] as Statistics)
            return
        }
        lifecycleScope.launch {
            val userStatics = MainActivity.getUserStatistics(period, this@ResultsActivity, MainStatic.currentToken!!)
            userStatics.onSuccess {
                bindValuesToTextViews(it)
                cachedStatistics[period] = it
            }
        }
    }

    private fun bindValuesToTextViews(stat: Statistics) {
        binding.countAnalyticsTextView.text = stat.analytics.toString()
        binding.countCallsTextView.text = stat.calls.toString()
        binding.countFlyersTextView.text = stat.flyers.toString()
        binding.countDealsTextView.text = stat.deals.toString()
        binding.countMeetsTextView.text = stat.meets.toString()
        binding.countDepositsTextView.text = stat.deposits.toString()
        binding.countSearchTextView.text = stat.searches.toString()
        binding.countShowsTextView.text = stat.shows.toString()
        binding.countOthersTextView.text = stat.others.toString()
    }

    companion object {
        public suspend fun getUserStatisticsWithKpi(context: Context, token: String): Result<Kpi> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val statisticsApi = retrofit.create(ServerApiUsers::class.java)

            return@coroutineScope try {
                val response = statisticsApi.getStatisticsWithKpi(token).await()
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Ошибка получения данных"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }


    }
}