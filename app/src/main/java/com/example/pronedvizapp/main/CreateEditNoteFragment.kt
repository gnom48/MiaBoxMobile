package com.example.pronedvizapp.main

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FragmentCreateEditTaskBinding
import com.example.pronedvizapp.notifications.NotificationApp
import com.example.pronedvizapp.notifications.NotificationBroadcast
import com.example.pronedvizapp.requests.ServerApiNotes
import com.example.pronedvizapp.requests.models.Note
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.UUID


class CreateEditNoteFragment : Fragment {

    lateinit var alarmManager: AlarmManager

    lateinit var selectedLocalDateTime: LocalDateTime
    lateinit var binding: FragmentCreateEditTaskBinding

    var noteToEdit: Note? = null

    constructor(note: Note): super() {
        this.noteToEdit = note
    }

    constructor(): super() {
        this.noteToEdit = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresApi(Build.VERSION_CODES.O)
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
                binding.setDateTextView.setText(selectedDate)
            }, year, month, day)

            datePickerDialog.show()
        }

        binding.setTimeTimePicker.setOnTimeChangedListener{ _, hourOfDay: Int, minute: Int ->
            selectedLocalDateTime = LocalDateTime.of(selectedLocalDateTime.year, selectedLocalDateTime.month, selectedLocalDateTime.dayOfMonth, hourOfDay, minute, 0)
        }

        // notifications
        alarmManager = this.requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        binding.completeButton.setOnClickListener {
            if (noteToEdit == null) {
                //val dbContext = LocalDb.getDb(this.requireContext())

                if (binding.taskTitleEditText.text.toString() == "") {
                    MaterialAlertDialogBuilder(this.requireContext())
                        .setMessage("Возможно вы ввели некорректные данные, проверьте правильность заполнения всех полей и попробуйде снова (поле \"Название\" обязательно должно быть заполнено)!")
                        .setPositiveButton("Ок") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                    return@setOnClickListener
                }

                if (LocalDateTime.now().isAfter(selectedLocalDateTime)) {
                    MaterialAlertDialogBuilder(this.requireContext())
                        .setMessage("Не стоит записывать заметки за прошедшие даты, лучше думать о будущем!")
                        .setPositiveButton("Ок") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                    return@setOnClickListener
                }

                val calendar = Calendar.getInstance()
                calendar.set(selectedLocalDateTime.year, selectedLocalDateTime.monthValue-1, selectedLocalDateTime.dayOfMonth, selectedLocalDateTime.hour, selectedLocalDateTime.minute)
                val time = calendar.timeInMillis

                val newNote = Note(
                    0,
                    binding.taskTitleEditText.text.toString(),
                    binding.taskDescEditText.text.toString(),
                    selectedLocalDateTime.toEpochSecond(ZoneOffset.UTC),
                    MainActivity.currentUser!!.id,
                    System.currentTimeMillis().toInt()
                )

                // notifications
                val alarmIntent = Intent(this.requireContext(), NotificationApp::class.java).let {
                    intent ->
                        intent.putExtra("TITLE", "Напоминание")
                        intent.putExtra("CONTENT", newNote.title)
                        PendingIntent.getBroadcast(this.requireContext(), newNote.notification_id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent)

//            var tViewModel = ViewModelProvider(this).get(DbViewModel::class.java)
//            tViewModel.insertNote(newNote)

                val retrofit = Retrofit.Builder()
                    .baseUrl(this@CreateEditNoteFragment.requireContext().getString(R.string.server_ip_address))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val notesApi = retrofit.create(ServerApiNotes::class.java)

                val req = notesApi.addNote(newNote, MainActivity.currentToken!!)
                var resultAddition: Int? = null
                req.enqueue(object : Callback<Int?> {
                    override fun onResponse(call: Call<Int?>, response: Response<Int?>) {
                        if (response.isSuccessful) {
//                            Toast.makeText(this@CreateEditNoteFragment.requireContext(), "Ок", Toast.LENGTH_SHORT).show()
                            resultAddition = response.body()

                            activity?.supportFragmentManager?.beginTransaction()?.remove(this@CreateEditNoteFragment)?.commit()
                            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(
                                R.id.bottomMenu
                            )
                            bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
                            return
                        }
                        resultAddition = null
                    }

                    override fun onFailure(call: Call<Int?>, t: Throwable) {
                        resultAddition = null
                        Toast.makeText(this@CreateEditNoteFragment.requireContext(), "Ошибка записи", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                //val dbContext = LocalDb.getDb(this.requireContext())

                if (binding.taskTitleEditText.text.toString() == "") {
                    MaterialAlertDialogBuilder(this.requireContext())
                        .setMessage("Возможно вы ввели некорректные данные, проверьте правильность заполнения всех полей и попробуйде снова (поле \"Название\" обязательно должно быть заполнено)!")
                        .setPositiveButton("Ок") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                    return@setOnClickListener
                }

                val calendar = Calendar.getInstance()
                calendar.set(selectedLocalDateTime.year, selectedLocalDateTime.monthValue, selectedLocalDateTime.dayOfMonth, selectedLocalDateTime.hour, selectedLocalDateTime.minute)
                val time = calendar.timeInMillis

                var newNote = Note(
                    noteToEdit!!.id,
                    binding.taskTitleEditText.text.toString(),
                    binding.taskDescEditText.text.toString(),
                    selectedLocalDateTime.toEpochSecond(ZoneOffset.UTC),
                    noteToEdit!!.user_id,
                    noteToEdit!!.notification_id
                )

                // notifications
                val alarmIntent = Intent(context, NotificationApp::class.java).let {
                        intent ->
                    intent.putExtra("TITLE", newNote.title)
                    intent.putExtra("CONTENT", newNote.desc)
                    PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
//                alarmManager.cancel(alarmIntent) // TODO: удалять уведомление
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)

//            var tViewModel = ViewModelProvider(this).get(DbViewModel::class.java)
//            tViewModel.insertNote(newNote)

                val retrofit = Retrofit.Builder()
                    .baseUrl(this@CreateEditNoteFragment.requireContext().getString(R.string.server_ip_address))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val notesApi = retrofit.create(ServerApiNotes::class.java)

                val req = notesApi.editNote(newNote, MainActivity.currentToken!!)
                var resultAddition: Int? = null
                req.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
//                            Toast.makeText(this@CreateEditNoteFragment.requireContext(), "Ок", Toast.LENGTH_SHORT).show()

                            activity?.supportFragmentManager?.beginTransaction()?.remove(this@CreateEditNoteFragment)?.commit()
                            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(
                                R.id.bottomMenu
                            )
                            bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
                            return
                        }
                        Toast.makeText(this@CreateEditNoteFragment.requireContext(), "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        resultAddition = null
                        Toast.makeText(this@CreateEditNoteFragment.requireContext(), "Ошибка перезаписи", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        if (noteToEdit != null) {
            val dateTime = LocalDateTime.ofEpochSecond(noteToEdit!!.date_time, 0, ZoneOffset.UTC)
            binding.setTimeTimePicker.hour = dateTime.hour
            binding.setTimeTimePicker.minute = dateTime.minute

            selectedLocalDateTime = dateTime

            binding.setDateTextView.text = selectedLocalDateTime.toLocalDate().toString()

            binding.taskTitleEditText.setText(noteToEdit!!.title)
            binding.taskDescEditText.setText(noteToEdit!!.desc)
        }

        return binding.root
    }
}

fun generateNotificationId(): Int {
    val uuid = UUID.randomUUID()
    return uuid.hashCode()
}