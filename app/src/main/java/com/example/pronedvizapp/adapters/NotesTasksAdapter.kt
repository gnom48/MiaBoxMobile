package com.example.pronedvizapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databases.models.INotesAdapterTemplete
import com.example.pronedvizapp.databinding.NoteCardBinding
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Task
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import com.example.pronedvizapp.main.CreateEditNoteFragment
import com.example.pronedvizapp.MainActivity

class NotesTasksAdapter(val context: MainActivity, var dataSource: ArrayList<INotesAdapterTemplete>): //, val actionListener: INotesTasksActionListener):
    RecyclerView.Adapter<NotesTasksAdapter.NoteViewHolder>() {

    init {
        lastDataSource = dataSource
    }

    inner class NoteViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        fun bind(mItem: INotesAdapterTemplete) {
            val binding: NoteCardBinding = NoteCardBinding.bind(view)

            if (mItem is Note) {
                val item = mItem as Note

                binding.detailsButton.setOnClickListener {
                    val fragment = CreateEditNoteFragment(item)
                    val transaction = context.supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.mainContentFrame, fragment)
                    transaction.commit()
                }

                val noteDatetime = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.date_time), ZoneOffset.systemDefault()).minusHours(3).toLocalDate()
                if (ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), noteDatetime) < 0.toLong()) {
                    binding.iconImageView.setImageResource(R.drawable.task_complete_icon)
                    binding.card.setBackgroundResource(R.drawable.note_card_others_res)
                    binding.indicatorPanel.setBackgroundResource(R.drawable.card_indicator_other_res)
                } else if (ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), noteDatetime) == 0.toLong()) {
                    binding.iconImageView.setImageResource(R.drawable.task_progress_icon)
                    binding.card.setBackgroundResource(R.drawable.notes_card_selected_res)
                    binding.indicatorPanel.setBackgroundResource(R.drawable.card_indicator_selected_res)
                } else if (ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), noteDatetime) > 0.toLong()) {
                    binding.iconImageView.setImageResource(R.drawable.task_planned_icon)
                    binding.card.setBackgroundResource(R.drawable.note_card_others_res)
                    binding.indicatorPanel.setBackgroundResource(R.drawable.card_indicator_other_res)
                }

                binding.titleTextView.setText(item.title)
                item.desc?.let {
                    if (item.desc.length > 70) {
                        binding.contentTextView.setText(it.substring(0, 70) + "...")
                        return@let
                    }
                    binding.contentTextView.setText(it)
                }
                binding.timeTextView.setText(
                    LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(item.date_time),
                        ZoneOffset.UTC
                    ).toLocalTime().toString()
                )

            } else if (mItem is Task) {
                val item = mItem as Task

                binding.detailsButton.setOnClickListener {
                    // TODO: новый фрагмент с подробностями задачи
                }
                binding.detailsButton.visibility = View.INVISIBLE

                val workDatetime = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.date_time + item.durationSeconds), ZoneOffset.systemDefault()).minusHours(3).toLocalDate()
                if (ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), workDatetime) < 0.toLong()) {
                    binding.card.setBackgroundResource(R.drawable.note_card_others_res)
                    binding.indicatorPanel.setBackgroundResource(R.drawable.card_indicator_other_res)
                } else if (ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), workDatetime) == 0.toLong()) {
                    binding.card.setBackgroundResource(R.drawable.notes_card_selected_res)
                    binding.indicatorPanel.setBackgroundResource(R.drawable.card_indicator_selected_res)
                } else if (ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), workDatetime) > 0.toLong()) {
                    binding.card.setBackgroundResource(R.drawable.note_card_others_res)
                    binding.indicatorPanel.setBackgroundResource(R.drawable.card_indicator_other_res)
                }

                binding.iconImageView.setImageResource(R.drawable.on_work_task_icon)
                binding.titleTextView.setText(item.workType)
                binding.contentTextView.setText("Текущая рабочая задача")
                val endTime = LocalDateTime.ofEpochSecond(item.date_time + item.durationSeconds, 0, ZoneOffset.UTC)
                if (LocalDateTime.now().isBefore(endTime)) {
                    val remainingDuration = Duration.between(LocalDateTime.now(), endTime)

                    binding.timeTextView.setText("Ещё ${String.format("%02d:%02d", remainingDuration.toHours(), remainingDuration.toMinutesPart())}")
                } else {
                    binding.timeTextView.setText("Завершено")
                    binding.detailsButton.visibility = View.GONE
                    binding.completeTaskButton.visibility = View.VISIBLE
                    binding.completeTaskButton.setOnClickListener {
                        MainActivity.showResultDialog(item, context)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_card, parent, false)
        return NoteViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(dataSource[position])
    }

    companion object {

        var lastDataSource = arrayListOf<INotesAdapterTemplete>()

    }
}