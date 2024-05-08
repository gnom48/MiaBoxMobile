package com.example.pronedvizapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.MyTeamDetailsActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.TeamCardBinding
import com.example.pronedvizapp.databinding.TeamUserCardBinding
import com.example.pronedvizapp.requests.models.Member
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import com.example.pronedvizapp.requests.models.UserTeamsWithInfoItem
import com.google.gson.Gson

class TeamMembersAdapter(val dataSource: List<Member>, val context: Context): RecyclerView.Adapter<TeamMembersAdapter.InfoViewHolder>() {

    inner class InfoViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: TeamUserCardBinding = TeamUserCardBinding.bind(view)

        fun bind(item: Member) {
            binding.userNameTextView.setText(item.user.name)
            binding.statusTextView.setText(item.role.description)
            binding.analyticsPreTextView.setText(item.statistics.day.analytics.toString())
            binding.flyersPreTextView.setText(item.statistics.day.flyers.toString())
            binding.dealssPreTextView.setText(item.statistics.day.deals.toString())
            binding.callsPreTextView.setText(item.statistics.day.calls.toString())

            binding.root.setOnClickListener {
                // TODO: подробнее вся статистика для администратора
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.team_user_card, parent, false)
        return InfoViewHolder(view)
    }

    override fun getItemCount(): Int = dataSource.size

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.bind(dataSource[position])
    }
}