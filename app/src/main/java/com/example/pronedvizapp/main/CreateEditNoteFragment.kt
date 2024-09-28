package com.example.pronedvizapp.main

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FragmentCreateEditTaskBinding
import com.example.pronedvizapp.notifications.NotificationApp
import com.example.pronedvizapp.requests.RequestsRepository.addNewNote
import com.example.pronedvizapp.requests.RequestsRepository.editNote
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.Note
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale


class CreateEditNoteFragment: Fragment {

    lateinit var alarmManager: AlarmManager
    lateinit var selectedLocalDateTime: LocalDateTime
    lateinit var binding: FragmentCreateEditTaskBinding
    private var isNewTask: Boolean = false
    private var noteToEdit: Note? = null

    constructor(note: Note, isNew: Boolean = false): super() {
        this.noteToEdit = note
        isNewTask = isNew
    }

    constructor(): super() {
        this.noteToEdit = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentCreateEditTaskBinding.inflate(inflater, container, false)

        selectedLocalDateTime = LocalDateTime.now()
        selectedLocalDateTime.plusHours(1)

        binding.setTimeTimePicker.setIs24HourView(true)
        binding.setDateTextView.text = selectedLocalDateTime.toLocalDate().toString()
        binding.setTimeTimePicker.hour = selectedLocalDateTime.hour
        binding.setTimeTimePicker.minute = selectedLocalDateTime.minute

        binding.setDateTextView.setOnClickListener {
            val calendar = Calendar.getInstance()

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this.requireContext(), DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format(Locale.US, "%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
                selectedLocalDateTime = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, selectedLocalDateTime.hour, selectedLocalDateTime.minute, 0)
                binding.setDateTextView.text = selectedDate
            }, year, month, day)

            datePickerDialog.show()
        }

        binding.setTimeTimePicker.setOnTimeChangedListener{ _, hourOfDay: Int, minute: Int ->
            selectedLocalDateTime = LocalDateTime.of(selectedLocalDateTime.year, selectedLocalDateTime.month, selectedLocalDateTime.dayOfMonth, hourOfDay, minute, 0)
        }

        alarmManager = this.requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        binding.completeButton.setOnClickListener {
            makeTaskWork()
        }

        binding.completeButtonDoubler.setOnClickListener {
            makeTaskWork()
        }

        if (noteToEdit != null) {
            val dateTime = LocalDateTime.ofEpochSecond(noteToEdit!!.dateTime, 0, ZoneOffset.UTC)
            binding.setTimeTimePicker.hour = dateTime.hour
            binding.setTimeTimePicker.minute = dateTime.minute

            selectedLocalDateTime = dateTime

            binding.setDateTextView.text = selectedLocalDateTime.toLocalDate().toString()

            binding.taskTitleEditText.setText(noteToEdit!!.title)
            binding.taskDescEditText.setText(noteToEdit!!.desc)
        }

        return binding.root
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun makeTaskWork() {
        if (noteToEdit == null) {
            if (binding.taskTitleEditText.text.toString() == "") {
                MaterialAlertDialogBuilder(this.requireContext())
                    .setMessage("Возможно вы ввели некорректные данные, проверьте правильность заполнения всех полей и попробуйде снова (поле \"Название\" обязательно должно быть заполнено)!")
                    .setPositiveButton("Ок") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                return
            }
            if (LocalDateTime.now().isAfter(selectedLocalDateTime)) {
                MaterialAlertDialogBuilder(this.requireContext())
                    .setMessage("Не стоит записывать заметки за прошедшие даты, лучше думать о будущем!")
                    .setPositiveButton("Ок") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                return
            }

            val calendar = Calendar.getInstance()
            calendar.set(selectedLocalDateTime.year, selectedLocalDateTime.monthValue-1, selectedLocalDateTime.dayOfMonth, selectedLocalDateTime.hour, selectedLocalDateTime.minute)
            val time = calendar.timeInMillis

            val newNote = Note(
                title = binding.taskTitleEditText.text.toString(),
                desc = binding.taskDescEditText.text.toString(),
                dateTime = selectedLocalDateTime.toEpochSecond(ZoneOffset.UTC),
                userId = MainStatic.currentUser.id,
                notificationId = System.currentTimeMillis().toInt()
            )

            val alarmIntent = Intent(this.requireContext(), NotificationApp::class.java).let {
                    intent ->
                intent.putExtra("TITLE", "Напоминание")
                intent.putExtra("CONTENT", newNote.title)
                PendingIntent.getBroadcast(this.requireContext(), newNote.notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent)

            lifecycleScope.launch {
                val additionResult = addNewNote(this@CreateEditNoteFragment.requireContext(), newNote, MainStatic.currentToken)
                additionResult.onSuccess {
                    activity?.supportFragmentManager?.beginTransaction()?.remove(this@CreateEditNoteFragment)?.commit()
                    val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottomMenu)
                    bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
                }
                additionResult.onFailure {
                    Toast.makeText(this@CreateEditNoteFragment.requireContext(), "Ошибка записи", Toast.LENGTH_SHORT).show()
                }
                additionResult.onCached {
                    activity?.supportFragmentManager?.beginTransaction()?.remove(this@CreateEditNoteFragment)?.commit()
                    val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottomMenu)
                    bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
                    usingLocalDataToast(this@CreateEditNoteFragment.requireContext())
                }
            }
        } else {
            if (binding.taskTitleEditText.text.toString() == "") {
                MaterialAlertDialogBuilder(this.requireContext())
                    .setMessage("Возможно вы ввели некорректные данные, проверьте правильность заполнения всех полей и попробуйде снова (поле \"Название\" обязательно должно быть заполнено)!")
                    .setPositiveButton("Ок") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                return
            }

            val calendar = Calendar.getInstance()
            calendar.set(selectedLocalDateTime.year, selectedLocalDateTime.monthValue, selectedLocalDateTime.dayOfMonth, selectedLocalDateTime.hour, selectedLocalDateTime.minute)

            val newNote = Note(
                noteToEdit!!.id,
                binding.taskTitleEditText.text.toString(),
                binding.taskDescEditText.text.toString(),
                selectedLocalDateTime.toEpochSecond(ZoneOffset.UTC),
                noteToEdit!!.userId,
                noteToEdit!!.notificationId
            )

            val alarmIntent = Intent(context, NotificationApp::class.java).let {
                    intent ->
                intent.putExtra("TITLE", newNote.title)
                intent.putExtra("CONTENT", newNote.desc)
                PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            //alarmManager.cancel(alarmIntent) // FIXME: удалять уведомление
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)

            lifecycleScope.launch {
                val additionResult = editNote(this@CreateEditNoteFragment.requireContext(), noteToEdit!!, MainStatic.currentToken)
                additionResult.onSuccess {
                    activity?.supportFragmentManager?.beginTransaction()?.remove(this@CreateEditNoteFragment)?.commit()
                    val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottomMenu)
                    bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
                }
                additionResult.onFailure {
                    Toast.makeText(this@CreateEditNoteFragment.requireContext(), "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                }
                additionResult.onCached {
                    activity?.supportFragmentManager?.beginTransaction()?.remove(this@CreateEditNoteFragment)?.commit()
                    val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottomMenu)
                    bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
                    usingLocalDataToast(this@CreateEditNoteFragment.requireContext())
                }
            }
        }
    }
}