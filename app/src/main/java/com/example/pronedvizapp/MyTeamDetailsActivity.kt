package com.example.pronedvizapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pronedvizapp.adapters.TeamMembersAdapter
import com.example.pronedvizapp.adapters.TeamsAdapter
import com.example.pronedvizapp.databinding.ActivityAllTeamsBinding
import com.example.pronedvizapp.databinding.ActivityMyTeamDetailsBinding
import com.example.pronedvizapp.requests.ServerApiTeams
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import com.example.pronedvizapp.requests.models.UserTeamsWithInfoItem
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.reflect.typeOf

class MyTeamDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityMyTeamDetailsBinding
    lateinit var teamInfo: UserTeamsWithInfoItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTeamDetailsBinding.inflate(layoutInflater)
        setSupportActionBar(binding.constraintLayout)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setContentView(binding.root)

        teamInfo = Gson().fromJson<UserTeamsWithInfoItem>(intent.getStringExtra("data"), UserTeamsWithInfoItem::class.java)

        binding.recyclerView.adapter = TeamMembersAdapter(teamInfo.members, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        val t: MenuItem = findViewById(R.id.leaveTeamMenuItem)
//        t.isVisible = true
        // TODO: сюда припихнуть администрирование по правам
        menuInflater.inflate(R.menu.team_optional_menu_res, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return  when (item.itemId) {
            R.id.leaveTeamMenuItem -> {
                val retrofit = Retrofit.Builder()
                    .baseUrl(getString(R.string.server_ip_address))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val teamsApi = retrofit.create(ServerApiTeams::class.java)

                val resp = teamsApi.leaveTeam(teamInfo.team.id, MainActivity.currentToken!!)
                resp.enqueue(object : Callback<Boolean> {
                    override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                        if (response.isSuccessful) {
                            this@MyTeamDetailsActivity.finish()
                            Toast.makeText(this@MyTeamDetailsActivity, "Вы покинули команду", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Boolean>, t: Throwable) {
                        Toast.makeText(this@MyTeamDetailsActivity, t.message, Toast.LENGTH_SHORT).show()
                    }
                })


                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}