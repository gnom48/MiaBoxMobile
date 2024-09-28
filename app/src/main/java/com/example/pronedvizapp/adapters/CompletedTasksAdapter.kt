package com.example.pronedvizapp.adapters

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.AddressListviewItemBinding
import com.example.pronedvizapp.databinding.CallListviewItemBinding
import com.example.pronedvizapp.requests.models.CompletedTasks
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.MONTH_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.WEEK_STATISTICS_PERIOD
import java.util.Calendar
import java.util.TimeZone

class CompletedTasksAdapter(private val context: Context, private val tasks: ArrayList<Task>) : BaseAdapter() {

    override fun getCount(): Int = tasks.size

    override fun getItem(position: Int): Task = tasks[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.address_listview_item, parent, false)
        val binding = AddressListviewItemBinding.bind(view)

        binding.imageView3.setImageResource(R.drawable.work_icon)

        binding.contentTextView.text = tasks[position].desc
        binding.dateTimeTextView.text = CallsInGroupAdapter.unixDateTimeToString(tasks[position].dateTime.toInt())

        val params = binding.root.layoutParams
        params.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ITEM_HEIGHT, context.resources.displayMetrics).toInt()
        binding.root.layoutParams = params

        return binding.root
    }

    companion object {

        const val ITEM_HEIGHT: Float = 100.0f

        fun filterTasksForToday(tasks: CompletedTasks, filterType: String): CompletedTasks {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

            return when(filterType) {
                DAY_STATISTICS_PERIOD -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfDay = calendar.timeInMillis / 1000

                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endOfDay = calendar.timeInMillis / 1000

                    CompletedTasks(tasks.filter { task ->
                        task.dateTime in startOfDay..endOfDay
                    }.toMutableList())
                }
                WEEK_STATISTICS_PERIOD -> {
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfWeek = calendar.timeInMillis / 1000

                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endOfWeek = calendar.timeInMillis / 1000

                    CompletedTasks(tasks.filter { task ->
                        task.dateTime in startOfWeek..endOfWeek
                    }.toMutableList())
                }
                MONTH_STATISTICS_PERIOD -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfMonth = calendar.timeInMillis / 1000

                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endOfMonth = calendar.timeInMillis / 1000

                    CompletedTasks(tasks.filter { task ->
                        task.dateTime in startOfMonth..endOfMonth
                    }.toMutableList())
                }

                else -> { CompletedTasks(mutableListOf()) }
            }

        }
    }
}