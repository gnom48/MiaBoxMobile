package com.example.pronedvizapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.TeamUserCardBinding
import com.example.pronedvizapp.requests.RequestsRepository.bindUserImageFileAsync
import com.example.pronedvizapp.requests.RequestsRepository.moveTeamMemberRole
import com.example.pronedvizapp.requests.models.Member
import com.example.pronedvizapp.requests.models.Team
import com.example.pronedvizapp.requests.models.UserStatuses
import com.example.pronedvizapp.teams.FullMemberInfoActivity
import com.example.pronedvizapp.teams.MyTeamDetailsActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ConnectException

class TeamMembersAdapter(val dataSource: List<Member>, val context: Context, private val owner: MyTeamDetailsActivity, private val isOwner: Boolean, val team: Team): RecyclerView.Adapter<TeamMembersAdapter.InfoViewHolder>() {

    inner class InfoViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: TeamUserCardBinding = TeamUserCardBinding.bind(view)

        fun bind(item: Member) {
            CoroutineScope(Dispatchers.IO).launch {
                bindUserImageFileAsync(context,
                    imageView = binding.teamMemberAvatarImageView,
                    imageId = item.user.image,
                    userId = item.user.id
                )
            }

            binding.userNameTextView.text = item.user.name
            binding.statusTextView.text = item.role.description
            binding.analyticsPreTextView.text = item.statistics?.day?.analytics.toString()
            binding.flyersPreTextView.text = item.statistics?.day?.flyers.toString()
            binding.dealssPreTextView.text = ((item.statistics?.day?.dealsSale ?: 0) + (item.statistics?.day?.dealsRent ?: 0)).toString()
            binding.callsPreTextView.text = item.statistics?.day?.calls.toString()

            if (!isOwner) {
                binding.optionImageButton.visibility = View.GONE
            }

            binding.optionImageButton.setOnClickListener {
                val popupMenu = PopupMenu(context, it)
                popupMenu.menuInflater.inflate(R.menu.teammember_optional_menu, popupMenu.menu)
                if (item.role == UserStatuses.USER) {
                    popupMenu.menu.findItem(R.id.move_role_to_user).isVisible = false
                }
                if (item.role == UserStatuses.OWNER) {
                    popupMenu.menu.findItem(R.id.move_role_to_owner).isVisible = false
                }
                popupMenu.menu.findItem(R.id.remove_from_team).isVisible = false

                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.move_role_to_owner -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                val res = moveTeamMemberRole(context, item.user.id, team.id, UserStatuses.OWNER, MainStatic.currentToken!!)
                                res.onSuccess {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, "Статус изменен", Toast.LENGTH_SHORT).show()
                                        item.role = UserStatuses.OWNER
                                        notifyItemChanged(adapterPosition)
                                    }
                                }
                                res.onFailure {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка смены статуса", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            true
                        }
                        R.id.move_role_to_user -> {
                            CoroutineScope(Dispatchers.IO).launch {
                                val res = moveTeamMemberRole(context, item.user.id, team.id, UserStatuses.USER, MainStatic.currentToken!!)
                                res.onSuccess {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, "Статус изменен", Toast.LENGTH_SHORT).show()
                                        item.role = UserStatuses.USER
                                        notifyItemChanged(adapterPosition)
                                    }
                                }
                                res.onFailure {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка смены статуса", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            true
                        }
                        R.id.remove_from_team -> {
                            true
                        }
                        else -> false
                    }
                }

                popupMenu.show()
            }

            binding.root.setOnClickListener {
                if (dataSource.find { member -> member.user.id == MainStatic.currentUser.id }?.role?.name == UserStatuses.OWNER.name) {
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