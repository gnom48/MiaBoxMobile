package com.example.pronedvizapp.adapters

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FullMemberInfoDialogBinding
import com.example.pronedvizapp.databinding.TeamUserCardBinding
import com.example.pronedvizapp.requests.models.Member
import com.example.pronedvizapp.requests.models.UserStatuses

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
                if (dataSource.find { member -> member.user.id == MainActivity.currentUser!!.id }?.role?.name == UserStatuses.OWNER.name) {
                    val bindingDialog = FullMemberInfoDialogBinding.inflate(LayoutInflater.from(context))

                    bindingDialog.recyclerStatistics.adapter = FullStatisticsAdapter(item.statistics, context)
                    val snapHelper: SnapHelper = PagerSnapHelper()
                    snapHelper.attachToRecyclerView(bindingDialog.recyclerStatistics)
                    bindingDialog.recyclerStatistics.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                    bindingDialog.aboutUserTextView.text = "${item.user.name} (${item.user.login})"

                    val dialog = Dialog(context)
                    dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
                    dialog.setContentView(bindingDialog.root)
                    dialog.show()

                    bindingDialog.closeImageButton.setOnClickListener {
                        dialog.dismiss()
                    }
                } else {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Нет доступа!")
                        .setMessage("Для просмотра всей информации об участнике необходимо являться администратором!")
                        .setIcon(R.drawable.baseline_error_outline_24)
                        .setPositiveButton("ОК") { dialog, id ->
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