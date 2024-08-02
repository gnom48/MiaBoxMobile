package com.example.pronedvizapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.databinding.ActivityResultsBinding
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.MONTH_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.StatisticsWithKpi
import com.example.pronedvizapp.requests.models.WEEK_STATISTICS_PERIOD
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory

class ResultsActivity : AppCompatActivity() {

    lateinit var binding: ActivityResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val userStatics = getUserStatisticsWithKpi(this@ResultsActivity, MainStatic.currentToken!!)
            userStatics.onSuccess {
                binding.kpiLevelTextView.text = it.userLevel
                binding.kpiTextView.text = it.salaryPercentage.toString()
            }
        }

        binding.dayButton.isSelected = true

        binding.dayButton.setOnClickListener {
            binding.dayButton.isSelected = true
            binding.weekButton.isSelected = false
            binding.monthButton.isSelected = false
            showResultsByPeriod(DAY_STATISTICS_PERIOD)
        }
        binding.weekButton.setOnClickListener {
            binding.dayButton.isSelected = false
            binding.weekButton.isSelected = true
            binding.monthButton.isSelected = false
            showResultsByPeriod(WEEK_STATISTICS_PERIOD)
        }
        binding.monthButton.setOnClickListener {
            binding.dayButton.isSelected = false
            binding.weekButton.isSelected = false
            binding.monthButton.isSelected = true
            showResultsByPeriod(MONTH_STATISTICS_PERIOD)
        }

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        showResultsByPeriod(DAY_STATISTICS_PERIOD)
    }

    private fun showResultsByPeriod(period: String) {
        lifecycleScope.launch {
            val userStatics = MainActivity.getUserStatistics(period, this@ResultsActivity, MainStatic.currentToken!!)
            userStatics.onSuccess {
                binding.countAnalyticsTextView.setText(it.analytics.toString())
                binding.countCallsTextView.setText(it.calls.toString())
                binding.countFlyersTextView.setText(it.flyers.toString())
                binding.countDealsTextView.setText(it.deals.toString())
            }
        }
    }

    companion object {
        public suspend fun getUserStatisticsWithKpi(context: Context, token: String): Result<StatisticsWithKpi> = coroutineScope {
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