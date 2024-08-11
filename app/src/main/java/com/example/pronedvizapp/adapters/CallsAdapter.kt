package com.example.pronedvizapp.adapters

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.calls.CallsGroup
import com.example.pronedvizapp.databinding.CallGroupItemBinding
import com.example.pronedvizapp.requests.ServerApiCalls
import com.example.pronedvizapp.requests.models.CallsRecords
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime
import java.time.ZoneOffset

class CallsAdapter(private val context: Context, private val callsGroups: List<CallsGroup>, private val callsRecords: List<CallsRecords>): RecyclerView.Adapter<CallsAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val binding: CallGroupItemBinding = CallGroupItemBinding.bind(view)

        fun bind(item: CallsGroup) {
            binding.listView.adapter = CallsInGroupAdapter(context, item.calls.sortedByDescending { it.dateTime }, callsRecords)
            var params = binding.listView.layoutParams
            params.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120F * item.calls.size, context.resources.displayMetrics).toInt()
            binding.listView.layoutParams = params
            binding.listView.requestLayout()
            binding.dateTextView.text = "${item.date.dayOfMonth}.${item.date.monthValue}.${item.date.year}"
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

    companion object {

        public suspend fun getAllUserCallsRecords(context: Context, userId: Int, token: String): Result<List<CallsRecords>?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ServerApiCalls::class.java)

            return@coroutineScope try {
                val resp = api.getAllRecordsInfo(userId, token)
                if (resp.isSuccessful) {
                    val response = resp.body()
                    Result.success(response)
                } else {
                    Result.failure(Exception("Response is not successful: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    }
}