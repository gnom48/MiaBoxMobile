package com.example.pronedvizapp.main

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.IFragmentTag
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.adapters.DatesAdapter
import com.example.pronedvizapp.adapters.NotesTasksAdapter
import com.example.pronedvizapp.adapters.OnDateItemClickListener
import com.example.pronedvizapp.databases.models.INotesAdapterTemplate
import com.example.pronedvizapp.databinding.FragmentNotesBinding
import com.example.pronedvizapp.notifications.NotificationApp
import com.example.pronedvizapp.requests.RequestsRepository.deleteNoteAsync
import com.example.pronedvizapp.requests.RequestsRepository.deleteTaskAsync
import com.example.pronedvizapp.requests.RequestsRepository.getAllNotesCurrentUser
import com.example.pronedvizapp.requests.RequestsRepository.getAllTasksCurrentUser
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotesFragment(override val fragmentNavigationTag: String = "NotesFragment") : Fragment(), IFragmentTag {

    var dataSource = ArrayList<INotesAdapterTemplate>()
    var filtredDataSource = ArrayList<INotesAdapterTemplate>()
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

        filtredDataSource = dataSource
        filtredDataSource = ArrayList(filtredDataSource.filter { Instant.ofEpochSecond(it.dateTime).atZone(ZoneId.systemDefault()).minusHours(3).toLocalDate() == selectedDate.toLocalDate() })
        val notesTasksAdapter = NotesTasksAdapter(this.requireActivity() as MainActivity, filtredDataSource) {
            updateRecyclerViewAdapter()
        }
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val dialog = MaterialAlertDialogBuilder(this@NotesFragment.requireContext())
                    .setTitle("Подтверждение")
                    .setMessage("Вы действительно хотите удалить элемент списка задач?")
                    .setNegativeButton("Отмена") { mDialog, _ ->
                        mDialog.dismiss()
                    }
                    .setPositiveButton("Да") { mDialog, _ ->
                        lifecycleScope.launch {
                            val objectToDelete = filtredDataSource[viewHolder.adapterPosition]
                            filtredDataSource.remove(filtredDataSource[viewHolder.adapterPosition])

                            val alarmManager = this@NotesFragment.requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

                            when(objectToDelete) {
                                is Note -> {
                                    val alarmIntent = Intent(this@NotesFragment.requireContext(), NotificationApp::class.java).let {
                                            intent ->
                                        intent.putExtra("TITLE", "Напоминание")
                                        intent.putExtra("CONTENT", (objectToDelete as Note).title)
                                        PendingIntent.getBroadcast(this@NotesFragment.requireContext(), (objectToDelete as Note).notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                                    }
                                    alarmManager.cancel(alarmIntent)
                                    deleteNoteAsync(this@NotesFragment.requireContext(), objectToDelete.id, MainStatic.currentToken)
                                }
                                is Task -> {
                                    val alarmIntent = Intent(this@NotesFragment.requireContext(), NotificationApp::class.java).let {
                                            intent ->
                                        intent.putExtra("TITLE", "Уведомление")
                                        intent.putExtra("CONTENT", "Как прошла работа над ${(objectToDelete as Task).workType}?")
                                        PendingIntent.getBroadcast(this@NotesFragment.requireContext(), (objectToDelete as Task).notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                                    }
                                    alarmManager.cancel(alarmIntent)
                                    deleteTaskAsync(this@NotesFragment.requireContext(), objectToDelete.id, MainStatic.currentToken)
                                }
                                else -> { }
                            }

                            updateRecyclerViewAdapter()
                            // binding.notesRecyclerView.adapter = NotesTasksAdapter(this@NotesFragment.requireActivity() as MainActivity, fildredDataSource)
                            mDialog.dismiss()
                        }
                    }
                    .create()
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

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateRecyclerViewAdapter()
    }

    fun updateRecyclerViewAdapter() {
        refreshNotesTasks { unionList ->
            this@NotesFragment.dataSource = unionList
            filtredDataSource = dataSource
            filtredDataSource = ArrayList(filtredDataSource.filter { Instant.ofEpochSecond(it.dateTime).atZone(ZoneId.systemDefault()).minusHours(3).toLocalDate() == selectedDate.toLocalDate() })
            binding.notesRecyclerView.adapter = NotesTasksAdapter(this.requireActivity() as MainActivity, filtredDataSource) {
                updateRecyclerViewAdapter()
            }

            if (filtredDataSource.size == 0 || filtredDataSource.isEmpty() || filtredDataSource == null) {
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

    private fun refreshNotesTasks(callback: (ArrayList<INotesAdapterTemplate>) -> Unit) {
        lifecycleScope.launch {
            val resNotes = getAllNotesCurrentUser(this@NotesFragment.requireContext(), MainStatic.currentToken!!)
            var unionList = ArrayList<INotesAdapterTemplate>()

            resNotes.onSuccess {
                unionList.addAll(it as ArrayList<INotesAdapterTemplate>)
                val resTasks = getAllTasksCurrentUser(this@NotesFragment.requireContext(), MainStatic.currentUser!!.id, MainStatic.currentToken!!, false)

                resTasks.onSuccess {
                    unionList.addAll(it as ArrayList<INotesAdapterTemplate>)
                    callback(unionList)
                }
                resTasks.onCached {
                    unionList.addAll(it as ArrayList<INotesAdapterTemplate>)
                    callback(unionList)
                }
                resTasks.onFailure { }
            }
            resNotes.onCached {
                usingLocalDataToast(this@NotesFragment.requireContext())
                unionList.addAll(it as ArrayList<INotesAdapterTemplate>)
                val resTasks = getAllTasksCurrentUser(this@NotesFragment.requireContext(), MainStatic.currentUser!!.id, MainStatic.currentToken!!, false)

                resTasks.onSuccess {
                    unionList.addAll(it as ArrayList<INotesAdapterTemplate>)
                    callback(unionList)
                }
                resTasks.onCached {
                    unionList.addAll(it as ArrayList<INotesAdapterTemplate>)
                    callback(unionList)
                }
                resTasks.onFailure { }
            }
            resNotes.onFailure { }
        }
    }
}