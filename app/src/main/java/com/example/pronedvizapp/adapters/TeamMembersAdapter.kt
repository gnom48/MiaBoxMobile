package com.example.pronedvizapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.TeamUserCardBinding
import com.example.pronedvizapp.requests.models.Member
import com.example.pronedvizapp.requests.models.UserStatuses
import com.example.pronedvizapp.teams.FullMemberInfoActivity
import com.example.pronedvizapp.teams.MyTeamDetailsActivity
import com.google.gson.Gson

class TeamMembersAdapter(val dataSource: List<Member>, val context: Context, private val owner: MyTeamDetailsActivity): RecyclerView.Adapter<TeamMembersAdapter.InfoViewHolder>() {

    inner class InfoViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: TeamUserCardBinding = TeamUserCardBinding.bind(view)

        fun bind(item: Member) {
            binding.userNameTextView.text = item.user.name
            binding.statusTextView.text = item.role.description
            binding.analyticsPreTextView.text = item.statistics.day.analytics.toString()
            binding.flyersPreTextView.text = item.statistics.day.flyers.toString()
            binding.dealssPreTextView.text = item.statistics.day.deals.toString()
            binding.callsPreTextView.text = item.statistics.day.calls.toString()

            binding.root.setOnClickListener {
                if (dataSource.find { member -> member.user.id == MainStatic.currentUser!!.id }?.role?.name == UserStatuses.OWNER.name) {
                    val intent = Intent(owner.applicationContext, FullMemberInfoActivity::class.java)
                    val serialized = Gson().toJson(item)
                    intent.putExtra("data", serialized)
                    owner.startActivity(intent)
                } else {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Нет доступа!")
                        .setMessage("Для просмотра всей информации об участнике необходимо являться администратором!")
                        .setIcon(R.drawable.baseline_error_outline_24)
                        .setPositiveButton("ОК") { dialog, _ ->
                            dialog.dismiss()
                        }
                    val dialog = builder.create()
                    dialog.show()
                }
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