package com.example.pronedvizapp.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.calls.CallsGroup
import com.example.pronedvizapp.databinding.CallGroupItemBinding

class CallsAdapter(private val context: Context, private val callsGroups: List<CallsGroup>): RecyclerView.Adapter<CallsAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: CallGroupItemBinding = CallGroupItemBinding.bind(view)

        fun bind(item: CallsGroup) {
            binding.listView.adapter = CallsInGroupAdapter(context, item.calls)
            var params = binding.listView.layoutParams
            params.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120F * item.calls.size, context.resources.displayMetrics).toInt()
            binding.listView.layoutParams = params
            binding.listView.requestLayout()
            binding.dateTextView.text = item.date.toString()
            binding.countTextView.text = "Звонков: ${item.calls.size}"
            binding.showDetailsImageView.setOnClickListener {
                if (binding.listView.visibility == View.GONE) {
                    binding.listView.visibility = View.VISIBLE
                } else {
                    binding.listView.visibility = View.GONE
                }
                binding.showDetailsImageView.rotation += 180F
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.call_group_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = callsGroups.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(callsGroups[position])
    }
}