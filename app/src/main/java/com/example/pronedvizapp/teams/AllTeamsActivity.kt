package com.example.pronedvizapp.teams

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.TeamsAdapter
import com.example.pronedvizapp.databinding.ActivityAllTeamsBinding
import com.example.pronedvizapp.databinding.FragmentCreateTeamBinding
import com.example.pronedvizapp.requests.RequestsRepository.getMyTeamsInfo
import com.example.pronedvizapp.requests.RequestsRepository.postCreateTeam
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.Team
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import kotlinx.coroutines.launch
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
            val res = getMyTeamsInfo(this@AllTeamsActivity, MainStatic.currentToken)
            res.onSuccess {
                it.sortBy { userTeamsWithInfoItem -> userTeamsWithInfoItem.team.name }
                teamsInfo = it
                binding.recyclerView.adapter = teamsInfo?.let { TeamsAdapter(teamsInfo!!, this@AllTeamsActivity) }
                binding.recyclerView.layoutManager = LinearLayoutManager(this@AllTeamsActivity, LinearLayoutManager.VERTICAL, false)
            }
            res.onCached {
                usingLocalDataToast(this@AllTeamsActivity)
                it.sortBy { userTeamsWithInfoItem -> userTeamsWithInfoItem.team.name }
                teamsInfo = it
                binding.recyclerView.adapter = teamsInfo?.let { TeamsAdapter(teamsInfo!!, this@AllTeamsActivity) }
                binding.recyclerView.layoutManager = LinearLayoutManager(this@AllTeamsActivity, LinearLayoutManager.VERTICAL, false)
            }
            res.onFailure {
                Toast.makeText(this@AllTeamsActivity, "Ошибка загрузки: $it.message", Toast.LENGTH_SHORT).show()
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

            bindingDialog.completeButton.setOnClickListener {
                val teamName = bindingDialog.enterTeamNameEditText.text.toString()
                lifecycleScope.launch {
                    val res = postCreateTeam(
                        this@AllTeamsActivity,
                        MainStatic.currentToken,
                        Team(
                            name = teamName,
                            createdDateTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                        )
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
            res.onCached {
                usingLocalDataToast(this@AllTeamsActivity)
                it.sortBy { userTeamsWithInfoItem -> userTeamsWithInfoItem.team.name }
                teamsInfo = it
            }
            res.onFailure {
                Toast.makeText(this@AllTeamsActivity, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}