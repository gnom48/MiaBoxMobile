package com.example.pronedvizapp.teams

import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pronedvizapp.EditProfileActivity
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.TeamsAdapter
import com.example.pronedvizapp.databinding.ActivityAllTeamsBinding
import com.example.pronedvizapp.databinding.EditProfileGenderDialogBinding
import com.example.pronedvizapp.databinding.FragmentCreateTeamBinding
import com.example.pronedvizapp.requests.ServerApiTeams
import com.example.pronedvizapp.requests.models.Team
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

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
            val res = getMyTeamsInfo(this@AllTeamsActivity, MainStatic.currentToken!!)
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

        binding.addNewTeamFloatingActionButton.setOnClickListener {
            val bindingDialog = FragmentCreateTeamBinding.inflate(LayoutInflater.from(this))

            val dialog = Dialog(this)
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(bindingDialog.root)
            dialog.show()

            bindingDialog.closeImageButton.setOnClickListener {
                dialog.dismiss()
            }

            // потом можно добавить другие параметры администрирования

            bindingDialog.completeButton.setOnClickListener {
                val teamName = bindingDialog.enterTeamNameEditText.text.toString()
                lifecycleScope.launch {
                    val res = postCreatTeam(
                        this@AllTeamsActivity,
                        MainStatic.currentToken!!,
                        Team(0, teamName, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                    )
                    res.onSuccess { }
                    res.onFailure {
                        Toast.makeText(this@AllTeamsActivity, "Не удалось создать команду! Попробуйте позже.", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
        }

        binding.joinToTeamFloatingActionButton.setOnClickListener {
            val intent = Intent(this, JoinTeamActivity::class.java)
            startActivity(intent)
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
            val res = getMyTeamsInfo(this@AllTeamsActivity, MainStatic.currentToken!!)
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

        suspend fun postCreatTeam(context: Context, token: String, team: Team): Result<Int?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val teamsApi = retrofit.create(ServerApiTeams::class.java)

            return@coroutineScope try {
                val resp = teamsApi.createTeam(team, token).await()
                Result.success(resp)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    }
}