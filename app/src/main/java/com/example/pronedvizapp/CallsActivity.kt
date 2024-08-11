package com.example.pronedvizapp

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pronedvizapp.adapters.CallsAdapter
import com.example.pronedvizapp.adapters.CallsAdapter.Companion.getAllUserCallsRecords
import com.example.pronedvizapp.bisness.calls.CallInfo
import com.example.pronedvizapp.bisness.calls.CallsGroup
import com.example.pronedvizapp.databinding.ActivityCallsBinding
import com.example.pronedvizapp.requests.ServerApiCalls
import com.example.pronedvizapp.requests.models.CallsRecords
import com.example.pronedvizapp.requests.models.UsersCalls
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CallsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallsBinding
    var dataSource = ArrayList<UsersCalls>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.adapter = CallsAdapter(this, listOf(), listOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.rootSwipeRefreshLayout.setOnRefreshListener {
            updateRecyclerViewAdapter()
        }

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        updateRecyclerViewAdapter()
    }

    private fun updateRecyclerViewAdapter() {
        refreshCalls { calls, records ->
            this@CallsActivity.dataSource = calls
            binding.recyclerView.layoutManager = LinearLayoutManager(this@CallsActivity, LinearLayoutManager.VERTICAL, false)
            binding.recyclerView.adapter = CallsAdapter(this, groupCallsByDate(calls), records)

            if (calls.isEmpty() || calls == null) {
                binding.recyclerView.setBackgroundResource(R.drawable.no_data_img_background)
            } else {
                binding.recyclerView.setBackgroundColor(Color.TRANSPARENT)
            }
            binding.rootSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun refreshCalls(callback: (ArrayList<UsersCalls>, ArrayList<CallsRecords>) -> Unit) {
        lifecycleScope.launch {
            val allCalls = getAllCallsByUserId(
                this@CallsActivity,
                MainStatic.currentUser!!.id,
                MainStatic.currentToken!!
            )

            allCalls.onSuccess { userCalls ->
                if (userCalls != null) {

                    val allRecords = getAllUserCallsRecords(
                        this@CallsActivity,
                        MainStatic.currentUser!!.id,
                        MainStatic.currentToken!!
                    )

                    allRecords.onSuccess {  userRecords ->
                        if (userRecords != null) {
                            callback(userCalls, userRecords as ArrayList<CallsRecords>)
                        } else {
                            callback(userCalls, arrayListOf())
                        }
                    }
                    allRecords.onFailure {
                        callback(userCalls, arrayListOf())
                    }

                } else {
                    callback(arrayListOf(), arrayListOf())
                }
            }
            allCalls.onFailure {
                Toast.makeText(this@CallsActivity, "Не удалось обновить: ${it.message}", Toast.LENGTH_SHORT).show()
                callback(arrayListOf(), arrayListOf())
            }
        }
    }

    companion object {

        public fun groupCallsByDate(calls: List<UsersCalls>): List<CallsGroup> {
            val callsByDate = mutableMapOf<LocalDate, MutableList<CallInfo>>()

            for (call in calls) {
                val date = Instant.ofEpochSecond(call.dateTime.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()
                if (!callsByDate.containsKey(date)) {
                    callsByDate[date] = mutableListOf()
                }
                callsByDate[date]?.add(
                    CallInfo(
                        lengthSeconds = call.lengthSeconds,
                        callerName = call.contactName,
                        dateTime = call.dateTime,
                        transcription = call.transcription,
                        phoneNumber = call.phoneNumber,
                        userId = call.userId,
                        recordId = call.recordId,
                        callType = call.callType
                    )
                )
            }

            val res = callsByDate.map { (date, calls) -> CallsGroup(date, calls) }
            return res.sortedByDescending { it.date }
        }


        suspend fun getAllCallsByUserId(context: Context, userId: Int, token: String): Result<ArrayList<UsersCalls>?> =
            coroutineScope {
                val retrofit = Retrofit.Builder()
                    .baseUrl(context.getString(R.string.server_ip_address))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val callsApi = retrofit.create(ServerApiCalls::class.java)

                return@coroutineScope try {
                    val resp = callsApi.getAllCalls(userId, token)
                    if (resp.isSuccessful) {
                        val response = resp.body()?.let { ArrayList<UsersCalls>(it) }
                        Result.success(response)
                    } else {
                        Result.failure(Exception("Response is not successful: ${resp.code()}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    }
}