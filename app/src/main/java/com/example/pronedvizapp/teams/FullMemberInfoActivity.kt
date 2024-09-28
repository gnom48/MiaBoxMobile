package com.example.pronedvizapp.teams

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.example.pronedvizapp.CallsActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.AddressesAdapter
import com.example.pronedvizapp.adapters.CallsAdapter
import com.example.pronedvizapp.adapters.FullStatisticsAdapter
import com.example.pronedvizapp.databinding.FullMemberInfoDialogBinding
import com.example.pronedvizapp.requests.RequestsRepository.getAllTasksCurrentUser
import com.example.pronedvizapp.requests.RequestsRepository.getAllUserCallsRecords
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.AddressInfo
import com.example.pronedvizapp.requests.models.CompletedTasks
import com.example.pronedvizapp.requests.models.Member
import com.example.pronedvizapp.requests.models.Task
import com.google.gson.Gson
import kotlinx.coroutines.launch

class FullMemberInfoActivity : AppCompatActivity() {

    private lateinit var binding: FullMemberInfoDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FullMemberInfoDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val teamMemberItem = Gson().fromJson<Member>(intent.getStringExtra("data"), Member::class.java)

        binding.navigationRailView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_statistics -> {
                    binding.statisticsRecyclerView.visibility = View.VISIBLE
                    binding.callsRecyclerView.visibility = View.GONE
                    binding.addressesMemberListView.visibility = View.GONE
                    binding.constraintLayout3.visibility = View.GONE
                    true
                }
                R.id.navigation_calls -> {
                    binding.statisticsRecyclerView.visibility = View.GONE
                    binding.callsRecyclerView.visibility = View.VISIBLE
                    binding.addressesMemberListView.visibility = View.GONE
                    binding.constraintLayout3.visibility = View.GONE
                    true
                }
                R.id.navigation_addresses -> {
                    binding.statisticsRecyclerView.visibility = View.GONE
                    binding.callsRecyclerView.visibility = View.GONE
                    binding.addressesMemberListView.visibility = View.VISIBLE
                    binding.constraintLayout3.visibility = View.GONE
                    true
                }
                R.id.navigation_kpi -> {
                    binding.statisticsRecyclerView.visibility = View.GONE
                    binding.callsRecyclerView.visibility = View.GONE
                    binding.addressesMemberListView.visibility = View.GONE
                    binding.constraintLayout3.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }

        binding.closeImageButton.setOnClickListener {
            this.finish()
        }

        lifecycleScope.launch {
            binding.callsRecyclerView.layoutManager = LinearLayoutManager(this@FullMemberInfoActivity, LinearLayoutManager.VERTICAL, false)
            val groupedCalls = CallsActivity.groupCallsByDate(teamMemberItem.calls)

            val allRecords = getAllUserCallsRecords(
                this@FullMemberInfoActivity,
                teamMemberItem.user.id,
                MainStatic.currentToken!!
            )

            allRecords.onSuccess { userRecords ->
                if (userRecords != null) {
                    groupedCalls.sortedByDescending { it.date }
                    binding.callsRecyclerView.adapter = CallsAdapter(this@FullMemberInfoActivity, groupedCalls, userRecords)
                } else {
                    binding.callsRecyclerView.adapter = CallsAdapter(this@FullMemberInfoActivity, groupedCalls, listOf())
                }
            }
            allRecords.onCached { userRecords ->
                if (userRecords != null) {
                    groupedCalls.sortedByDescending { it.date }
                    binding.callsRecyclerView.adapter = CallsAdapter(this@FullMemberInfoActivity, groupedCalls, userRecords)
                } else {
                    binding.callsRecyclerView.adapter = CallsAdapter(this@FullMemberInfoActivity, groupedCalls, listOf())
                }
            }
            allRecords.onFailure {
                binding.callsRecyclerView.adapter = CallsAdapter(this@FullMemberInfoActivity, groupedCalls, listOf())
            }
        }

        binding.currentKpiTextView.text = (teamMemberItem.kpi?.salaryPercentage ?: 0f).toString()
        binding.kpiLevelTextView.text = teamMemberItem.kpi?.userLevel ?: "Стажер"

        binding.addressesMemberListView.adapter = AddressesAdapter(this, ArrayList<AddressInfo>(teamMemberItem.addresses.sortedByDescending { it.dateTime }))

        lifecycleScope.launch {
            val userCompletedTasks = getAllTasksCurrentUser(this@FullMemberInfoActivity,  teamMemberItem.user.id, MainStatic.currentToken!!, true)
            userCompletedTasks.onSuccess {
                binding.statisticsRecyclerView.adapter = FullStatisticsAdapter(teamMemberItem.statistics, CompletedTasks(it as MutableCollection<Task>), this@FullMemberInfoActivity)
                val snapHelper: SnapHelper = PagerSnapHelper()
                snapHelper.attachToRecyclerView(binding.statisticsRecyclerView)
                binding.statisticsRecyclerView.layoutManager = LinearLayoutManager(this@FullMemberInfoActivity, LinearLayoutManager.HORIZONTAL, false)
            }
            userCompletedTasks.onCached {
                usingLocalDataToast(this@FullMemberInfoActivity)
                binding.statisticsRecyclerView.adapter = FullStatisticsAdapter(teamMemberItem.statistics, CompletedTasks(it as MutableCollection<Task>), this@FullMemberInfoActivity)
                val snapHelper: SnapHelper = PagerSnapHelper()
                snapHelper.attachToRecyclerView(binding.statisticsRecyclerView)
                binding.statisticsRecyclerView.layoutManager = LinearLayoutManager(this@FullMemberInfoActivity, LinearLayoutManager.HORIZONTAL, false)
            }
            userCompletedTasks.onFailure {
                binding.statisticsRecyclerView.adapter = FullStatisticsAdapter(teamMemberItem.statistics, CompletedTasks(mutableListOf()), this@FullMemberInfoActivity)
                val snapHelper: SnapHelper = PagerSnapHelper()
                snapHelper.attachToRecyclerView(binding.statisticsRecyclerView)
                binding.statisticsRecyclerView.layoutManager = LinearLayoutManager(this@FullMemberInfoActivity, LinearLayoutManager.HORIZONTAL, false)

                Toast.makeText(this@FullMemberInfoActivity, "Ошибка загрузки задач", Toast.LENGTH_SHORT).show()
            }
        }

        binding.aboutUserTextView.text = "${teamMemberItem.user.name} (${teamMemberItem.user.login})"
    }


    override fun onResume() {
        super.onResume()
        binding.navigationRailView.selectedItemId = R.id.navigation_statistics
    }
}