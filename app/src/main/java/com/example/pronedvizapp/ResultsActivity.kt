package com.example.pronedvizapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.bisness.calls.CallRecordingService.Companion.isServiceRunning
import com.example.pronedvizapp.databinding.ActivityResultsBinding
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.MONTH_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.WEEK_STATISTICS_PERIOD
import kotlinx.coroutines.launch

class ResultsActivity : AppCompatActivity() {

    lateinit var binding: ActivityResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val userStatics = MainActivity.getUserStatistics(period, this@ResultsActivity, MainActivity.currentToken!!)
            userStatics.onSuccess {
                binding.countAnalyticsTextView.setText(it.analytics.toString())
                binding.countCallsTextView.setText(it.calls.toString())
                binding.countFlyersTextView.setText(it.flyers.toString())
                binding.countDealsTextView.setText(it.deals.toString())

                val callsKpi = it.calls / 20 * 100
                val anakyticsKpi = it.analytics / 5 * 100
                val dealsKpi = it.deals / 2 * 100
                val flyersKpi = it.flyers / 20 * 100

                val kpi = 0.2 * callsKpi + 0.3 * flyersKpi + 0.1 * anakyticsKpi + 0.4 * dealsKpi

                binding.kpiTextView.setText(kpi.toString())
            }
        }
    }
}