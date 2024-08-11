package com.example.pronedvizapp.main

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.IFragmentTag
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.DatesAdapter
import com.example.pronedvizapp.adapters.NotesTasksAdapter
import com.example.pronedvizapp.adapters.OnDateItemClickListener
import com.example.pronedvizapp.databases.models.INotesAdapterTemplete
import com.example.pronedvizapp.databinding.FragmentNotesBinding
import com.example.pronedvizapp.notifications.NotificationApp
import com.example.pronedvizapp.requests.ServerApiNotes
import com.example.pronedvizapp.requests.ServerApiTasks
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotesFragment(override val fragmentNavigationTag: String = "NotesFragment") : Fragment(), IFragmentTag {

    var dataSource = ArrayList<INotesAdapterTemplete>()
    var fildredDataSource = ArrayList<INotesAdapterTemplete>()
    var selectedDate: LocalDateTime = LocalDateTime.now()

    lateinit var binding: FragmentNotesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate = LocalDateTime.now()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentNotesBinding.inflate(inflater, container, false)

        fildredDataSource = dataSource
        fildredDataSource = ArrayList(fildredDataSource.filter { Instant.ofEpochSecond(it.date_time).atZone(ZoneId.systemDefault()).minusHours(3).toLocalDate() == selectedDate.toLocalDate() })
        val notesTasksAdapter = NotesTasksAdapter(this.requireActivity() as MainActivity, fildredDataSource)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                var commit: Boolean = false

                val dialog = AlertDialog.Builder(this@NotesFragment.requireContext())
                    .setTitle("Подтверждение")
                    .setMessage("Вы действительно хотите удалить элемент списка задач?")
                    .setNegativeButton("Отмена") { dialog, _ ->
                        commit = false
                    }
                    .setPositiveButton("Да") { dialog, _ ->
                        commit = true
                    }
                    .create()

                dialog.setOnDismissListener {
                    if (!commit) {
                        updateRecyclerViewAdapter()
                        return@setOnDismissListener
                    }

                    val objectToDelete = fildredDataSource[viewHolder.adapterPosition]
                    fildredDataSource.remove(fildredDataSource[viewHolder.adapterPosition])
                    val alarmManager = this@NotesFragment.requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    if (objectToDelete is Note) {
                        val alarmIntent = Intent(this@NotesFragment.requireContext(), NotificationApp::class.java).let {
                                intent ->
                            intent.putExtra("TITLE", "Напоминание")
                            intent.putExtra("CONTENT", (objectToDelete as Note).title)
                            PendingIntent.getBroadcast(this@NotesFragment.requireContext(), (objectToDelete as Note).notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        }
                        alarmManager.cancel(alarmIntent)
                        deleteNote(this@NotesFragment.requireContext(), MainStatic.currentToken!!, objectToDelete.id)
                    } else {
                        val alarmIntent = Intent(this@NotesFragment.requireContext(), NotificationApp::class.java).let {
                                intent ->
                            intent.putExtra("TITLE", "Уведомление")
                            intent.putExtra("CONTENT", "Как прошла работа над ${(objectToDelete as Task).workType}?")
                            PendingIntent.getBroadcast(this@NotesFragment.requireContext(), (objectToDelete as Task).notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        }
                        alarmManager.cancel(alarmIntent)
                        deleteTask(this@NotesFragment.requireContext(), MainStatic.currentToken!!, objectToDelete.id)
                    }

                    binding.notesRecyclerView.adapter = NotesTasksAdapter(this@NotesFragment.requireActivity() as MainActivity, fildredDataSource)
                }
                dialog.show()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.notesRecyclerView)
        binding.notesRecyclerView.adapter = notesTasksAdapter
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this.requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.datesRecyclerView.layoutManager = LinearLayoutManager(this.requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.datesRecyclerView.adapter = DatesAdapter(object : OnDateItemClickListener {
            override fun onItemClick(date: LocalDateTime) {
                selectedDate = date
                binding.selectedDateTextView.text = getFormattedDateString(date)
                updateRecyclerViewAdapter()
            }
        })

        binding.selectedDateTextView.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this.requireActivity(), DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, 0, 0, 0)
                binding.selectedDateTextView.text = getFormattedDateString(selectedDate)
                updateRecyclerViewAdapter()
            }, selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)
            datePickerDialog.setTitle("Выберите дату")
            datePickerDialog.show()
        }

        binding.rootSwipeRefreshLayout.setOnRefreshListener {
            updateRecyclerViewAdapter()
        }

        updateRecyclerViewAdapter()

        return binding.root
    }

    private fun updateRecyclerViewAdapter() {
        refreshNotesTasks { unionList ->
            this@NotesFragment.dataSource = unionList
            fildredDataSource = dataSource
            fildredDataSource = ArrayList(fildredDataSource.filter { Instant.ofEpochSecond(it.date_time).atZone(ZoneId.systemDefault()).minusHours(3).toLocalDate() == selectedDate.toLocalDate() })
            binding.notesRecyclerView.adapter = NotesTasksAdapter(this.requireActivity() as MainActivity, fildredDataSource)

            if (fildredDataSource.size == 0 || fildredDataSource.isEmpty() || fildredDataSource == null) {
                binding.noDataImageView.visibility = View.VISIBLE
            } else {
                binding.noDataImageView.visibility = View.GONE
            }
            binding.rootSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getFormattedDateString(date: LocalDateTime): String {
        val zonedDateTime = date.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("ru"))
        val formattedDateTime = zonedDateTime.format(formatter)
        return formattedDateTime
    }

    private fun refreshNotesTasks(callback: (ArrayList<INotesAdapterTemplete>) -> Unit) {
        lifecycleScope.launch {
            val resNotes = getAllNotesCurrentUser(this@NotesFragment.requireContext(), MainStatic.currentToken!!)
            var unionList = ArrayList<INotesAdapterTemplete>()

            resNotes.onSuccess {
                unionList.addAll(it as ArrayList<INotesAdapterTemplete>)
                val resTasks = getAllTasksCurrentUser(this@NotesFragment.requireContext(), MainStatic.currentToken!!)

                resTasks.onSuccess {
                    unionList.addAll(it as ArrayList<INotesAdapterTemplete>)
                    callback(unionList)
                }
                resTasks.onFailure {
                    val e = it
                }
            }
            resNotes.onFailure {
                val e = it
            }
        }
    }

    companion object {

        fun deleteNote(context: Context, token: String, noteId: Int): Boolean { //: Result<Boolean> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val notesApi = retrofit.create(ServerApiNotes::class.java)

            var resp = notesApi.deleteNote(noteId, token)
            var delRes: Boolean = false
            resp.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        delRes = true
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {

                }

            })
            return delRes
        }

        fun deleteTask(context: Context, token: String, taskId: Int): Boolean {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val tasksApi = retrofit.create(ServerApiTasks::class.java)

            var resp = tasksApi.deleteTask(taskId, token)
            var delRes: Boolean = false
            resp.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        delRes = true
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {

                }

            })
            return delRes
        }

        suspend fun getAllNotesCurrentUser(context: Context, token: String): Result<ArrayList<Note>?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val notesApi = retrofit.create(ServerApiNotes::class.java)

            return@coroutineScope try {
                val resp = notesApi.getAllNotes(token).await()
                val response = ArrayList<Note>(resp)
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Ошибка загрузки"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun getAllTasksCurrentUser(context: Context, token: String): Result<ArrayList<Task>?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val tasksApi = retrofit.create(ServerApiTasks::class.java)

            return@coroutineScope try {
                val resp = tasksApi.getAllTasks(token).await()
                val response = ArrayList<Task>(resp)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        public fun getCompletedTasksCurrentUser(context: Context, callback: (ArrayList<Task>) -> Unit) {
            var tasksList: ArrayList<Task> = arrayListOf()

            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val tasksApi = retrofit.create(ServerApiTasks::class.java)

            val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            preferences.getString("TOKEN", null)?.let {token ->
                val resp = tasksApi.getAllTasks(token)

                resp.enqueue(object : Callback<List<Task>> {
                    override fun onResponse(
                        call: Call<List<Task>>,
                        response: Response<List<Task>>
                    ) {
                        if (response.isSuccessful) {
                            val tasksList = response.body()?.filter {
                                !LocalDateTime.now().isBefore(
                                    LocalDateTime.ofEpochSecond(
                                        it.date_time + it.durationSeconds,
                                        0,
                                        ZoneOffset.UTC
                                    )
                                )
                            } as ArrayList<Task>
                            callback(tasksList)
                        }
                    }

                    override fun onFailure(call: Call<List<Task>>, t: Throwable) {
                        callback(arrayListOf<Task>())
                    }
                })
            }
        }
    }
}