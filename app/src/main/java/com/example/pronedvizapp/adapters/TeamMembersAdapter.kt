package com.example.pronedvizapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.TeamUserCardBinding
import com.example.pronedvizapp.requests.models.Member
import com.example.pronedvizapp.requests.models.UserStatuses
import com.example.pronedvizapp.teams.MyTeamDetailsActivity

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
                    owner.showFullMemberInfoFragment(item)
                    /*val bindingDialog = FullMemberInfoDialogBinding.inflate(LayoutInflater.from(context))

                    bindingDialog.navigationRailView.setOnItemSelectedListener { item ->
                        when (item.itemId) {
                            R.id.navigation_statistics -> {
                                bindingDialog.statisticsRecyclerView.visibility = android.view.View.VISIBLE
                                bindingDialog.callsRecyclerView.visibility = android.view.View.GONE
                                bindingDialog.addressesMemberListView.visibility = android.view.View.GONE
                                true
                            }

                            R.id.navigation_calls -> {
                                bindingDialog.statisticsRecyclerView.visibility = android.view.View.GONE
                                bindingDialog.callsRecyclerView.visibility = android.view.View.VISIBLE
                                bindingDialog.addressesMemberListView.visibility = android.view.View.GONE
                                true
                            }

                            R.id.navigation_addresses -> {
                                bindingDialog.statisticsRecyclerView.visibility = android.view.View.GONE
                                bindingDialog.callsRecyclerView.visibility = android.view.View.GONE
                                bindingDialog.addressesMemberListView.visibility = android.view.View.VISIBLE
                                true
                            }

                            else -> false
                        }
                    }

                    bindingDialog.callsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    val groupedCalls = CallsActivity.groupCallsByDate(item.calls)
                    bindingDialog.callsRecyclerView.adapter = CallsAdapter(context, groupedCalls)

                    val addresses = item.addresses.map { it.address }
                    bindingDialog.addressesMemberListView.adapter = AddressesAdapter(context, addresses)

                    bindingDialog.statisticsRecyclerView.adapter = FullStatisticsAdapter(item.statistics, context)
                    val snapHelper: SnapHelper = PagerSnapHelper()
                    snapHelper.attachToRecyclerView(bindingDialog.statisticsRecyclerView)
                    bindingDialog.statisticsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                    bindingDialog.aboutUserTextView.text = "${item.user.name} (${item.user.login})"

                    val dialog = Dialog(context)
                    dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
                    dialog.setContentView(bindingDialog.root)

                    val window = dialog.window
                    window?.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val params = window?.attributes
                    params?.gravity = Gravity.CENTER
                    params?.x = 0
                    params?.y = 0
                    params?.width = ViewGroup.LayoutParams.MATCH_PARENT
                    params?.height = ViewGroup.LayoutParams.MATCH_PARENT
                    window?.attributes = params

                    dialog.show()

                    bindingDialog.closeImageButton.setOnClickListener {
                        dialog.dismiss()
                    }*/
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