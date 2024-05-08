package com.example.pronedvizapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.MainInfoCardBinding

class MainInfoAdapter(val dataSource: ArrayList<MainInfoForCard>): RecyclerView.Adapter<MainInfoAdapter.InfoViewHolder>() {

    inner class InfoViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: MainInfoCardBinding = MainInfoCardBinding.bind(view)

        fun bind(item: MainInfoForCard) {
            binding.countTextView.text = item.count
            binding.toDoTextView.text = item.toDo
            binding.descTextView.text = item.desc
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.main_info_card, parent, false)
        return InfoViewHolder(view)
    }

    override fun getItemCount(): Int = dataSource.size

    override fun onBindViewHolder(holder: InfoViewHolder, position: Int) {
        holder.bind(dataSource[position])
    }
}