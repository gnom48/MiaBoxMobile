package com.example.pronedvizapp.adapters

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.calls.CallInfo
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.databinding.CallListviewItemBinding
import com.example.pronedvizapp.databinding.EditProfileNameDialogBinding
import com.example.pronedvizapp.requests.models.ITaskStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallsInGroupAdapter(private val context: Context, private val calls: List<CallInfo>) : BaseAdapter() {

    override fun getCount(): Int = calls.size

    override fun getItem(position: Int): CallInfo = calls[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.call_listview_item, parent, false)

        val binding = CallListviewItemBinding.bind(view)
        binding.phoneNumberTextView.text = calls[position].phoneNumber
        if (calls[position].callerName != null && calls[position].callerName != "Неизвестный") {
            binding.phoneNumberTextView.text = binding.phoneNumberTextView.text.toString() + "(${calls[position].phoneNumber})"
        }
        binding.dateTimeTextView.text = unixTimeToString(calls[position].dateTime)
        binding.lengthSecondsTextView.text = getFormattedTimeLength(calls[position].lengthSeconds)
        binding.transcriptionButtonTextView.setOnClickListener {
            if (calls[position].transcription != null && calls[position].transcription != "" && calls[position].transcription != NO_TRANSCRIPTION) {
                val bindingDialog = EditProfileNameDialogBinding.inflate(LayoutInflater.from(context))

                val dialog = Dialog(context)
                dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
                dialog.setContentView(bindingDialog.root)
                dialog.show()

                bindingDialog.textViewText.text = "Запись звонка"
                bindingDialog.cancelButton.text = "OK"
                bindingDialog.saveButton.text = "Переделать"
                bindingDialog.editText.visibility = View.GONE
                bindingDialog.textViewText.visibility = View.VISIBLE
                bindingDialog.textViewText.text = calls[position].transcription.replace(".", ".\n, ") // потом сюда встанет нормализованный текст
                bindingDialog.saveButton.setOnClickListener { view ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val taskResponse = CallRecordingService.orderCallTranscription(context, MainStatic.currentUser!!.id, calls[position].recordId, "base", MainStatic.currentToken)
                        taskResponse.onSuccess { resp ->
                            resp?.let { status ->
                                withContext(Dispatchers.Main) {
                                    showTaskStatusDialog(status)
                                    view.isEnabled = false
                                }
                            }
                        }
                        taskResponse.onFailure {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Ошибка отправки запроса", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val taskResponse = CallRecordingService.orderCallTranscription(context, MainStatic.currentUser!!.id, calls[position].recordId, "base", MainStatic.currentToken)
                    taskResponse.onSuccess { resp ->
                        resp?.let { status ->
                            withContext(Dispatchers.Main) {
                                showTaskStatusDialog(status)
                            }
                        }
                        view.isEnabled = false
                    }
                    taskResponse.onFailure {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка отправки запроса", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.playImageButton.setOnClickListener {
            // TODO: проигрывать запись (СНАЧАЛА ПРОВЕРЯТЬ ЕСТЬ ЛИ ОНА ЛОКАЛЬНО)
        }

        return binding.root
    }

    private fun showTaskStatusDialog(taskResponse: ITaskStatus) {
        MaterialAlertDialogBuilder(context)
            .setMessage("Запрос на расшифровку разговора отправлен.\nСтатус задачи: \"${taskResponse.status}\" \nВозвращайтесь позже и проверьте результат расшифровки")
            .setPositiveButton("Ок") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun getFormattedTimeLength(timeSeconds: Int): String {
        val minutes = (timeSeconds / 60F).toInt()
        val seconds = timeSeconds - minutes * 60
        return "$minutes мин. $seconds сек."
    }

    private fun unixTimeToString(unixTime: Int): String {
        val date = Date(unixTime * 1000L)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    companion object {
        const val NO_TRANSCRIPTION = "no transcription"
    }
}