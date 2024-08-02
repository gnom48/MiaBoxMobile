package com.example.pronedvizapp.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.teams.AllTeamsActivity
import com.example.pronedvizapp.teams.MyTeamDetailsActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.TeamCardBinding
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import com.example.pronedvizapp.requests.models.UserTeamsWithInfoItem
import com.google.gson.Gson

class TeamsAdapter(val dataSource: UserTeamsWithInfo, val context: AllTeamsActivity): RecyclerView.Adapter<TeamsAdapter.InfoViewHolder>() {

    inner class InfoViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: TeamCardBinding = TeamCardBinding.bind(view)

        fun bind(item: UserTeamsWithInfoItem) {
            binding.titleTextView.text = item.team.name

            binding.detailsButton.setOnClickListener {
                val intent = Intent(context, MyTeamDetailsActivity::class.java)
                val serialized = Gson().toJson(item)
                intent.putExtra("data", serialized)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.team_card, parent, false)
        return InfoViewHolder(view)
    }

    override fun getItemCount(): Int = dataSource.size

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.bind(dataSource[position])
    }
}