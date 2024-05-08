package com.example.pronedvizapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pronedvizapp.adapters.TeamsAdapter
import com.example.pronedvizapp.databinding.ActivityAllTeamsBinding
import com.example.pronedvizapp.requests.ServerApiTeams
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory

class AllTeamsActivity : AppCompatActivity() {

    lateinit var binding: ActivityAllTeamsBinding
    var teamsInfo: UserTeamsWithInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllTeamsBinding.inflate(layoutInflater)
        setSupportActionBar(binding.constraintLayout)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setContentView(binding.root)

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        lifecycleScope.launch {
            val res = getMyTeamsInfo(this@AllTeamsActivity, MainActivity.currentToken!!)
            res.onSuccess {
                it.sortBy { userTeamsWithInfoItem -> userTeamsWithInfoItem.team.name }
                teamsInfo = it
                binding.recyclerView.adapter = teamsInfo?.let { TeamsAdapter(teamsInfo!!, this@AllTeamsActivity) }
                binding.recyclerView.layoutManager = LinearLayoutManager(this@AllTeamsActivity, LinearLayoutManager.VERTICAL, false)
            }
            res.onFailure {
                Toast.makeText(this@AllTeamsActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.rootSwipeRefreshLayout.setOnRefreshListener {
            updateTeams { data ->
                teamsInfo = data
            }
            binding.recyclerView.adapter = teamsInfo?.let { TeamsAdapter(it, this) }
            binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            binding.rootSwipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        updateTeams { data ->
            teamsInfo = data
        }
        binding.recyclerView.adapter = teamsInfo?.let { TeamsAdapter(it, this) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun updateTeams(callback: (UserTeamsWithInfo) -> Unit) {
        lifecycleScope.launch {
            val res = getMyTeamsInfo(this@AllTeamsActivity, MainActivity.currentToken!!)
            res.onSuccess {
                it.sortBy { userTeamsWithInfoItem -> userTeamsWithInfoItem.team.name }
                teamsInfo = it

            }
            res.onFailure {
                Toast.makeText(this@AllTeamsActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {

        suspend fun getMyTeamsInfo(context: Context, token: String): Result<UserTeamsWithInfo> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val teamsApi = retrofit.create(ServerApiTeams::class.java)

            return@coroutineScope try {
                val resp = teamsApi.getMyTeams(token).await()
                Result.success(resp)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    }
}