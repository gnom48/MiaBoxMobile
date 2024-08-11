package com.example.pronedvizapp.teams

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
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
import com.example.pronedvizapp.requests.models.AddressInfo
import com.example.pronedvizapp.requests.models.Member
import com.google.gson.Gson
import kotlinx.coroutines.launch

class FullMemberInfoActivity : AppCompatActivity() {

    private lateinit var binding: FullMemberInfoDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FullMemberInfoDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val item = Gson().fromJson<Member>(intent.getStringExtra("data"), Member::class.java)

        binding.navigationRailView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_statistics -> {
                    binding.statisticsRecyclerView.visibility = View.VISIBLE
                    binding.callsRecyclerView.visibility = View.GONE
                    binding.addressesMemberListView.visibility = View.GONE
                    true
                }
                R.id.navigation_calls -> {
                    binding.statisticsRecyclerView.visibility = View.GONE
                    binding.callsRecyclerView.visibility = View.VISIBLE
                    binding.addressesMemberListView.visibility = View.GONE
                    true
                }
                R.id.navigation_addresses -> {
                    binding.statisticsRecyclerView.visibility = View.GONE
                    binding.callsRecyclerView.visibility = View.GONE
                    binding.addressesMemberListView.visibility = View.VISIBLE
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
            val groupedCalls = CallsActivity.groupCallsByDate(item.calls)

            val allRecords = CallsAdapter.getAllUserCallsRecords(
                this@FullMemberInfoActivity,
                item.user.id,
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
            allRecords.onFailure {
                binding.callsRecyclerView.adapter = CallsAdapter(this@FullMemberInfoActivity, groupedCalls, listOf())
            }
        }

        binding.addressesMemberListView.adapter = AddressesAdapter(this, ArrayList<AddressInfo>(item.addresses.sortedByDescending { it.dateTime }))

        binding.statisticsRecyclerView.adapter = FullStatisticsAdapter(item.statistics, this)
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.statisticsRecyclerView)
        binding.statisticsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.aboutUserTextView.text = "${item.user.name} (${item.user.login})"
    }


    override fun onResume() {
        super.onResume()
        binding.navigationRailView.selectedItemId = R.id.navigation_statistics
    }
}