package com.example.pronedvizapp.adapters

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.bisness.calls.CallInfo
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.databinding.CallListviewItemBinding
import com.example.pronedvizapp.databinding.EditProfileNameDialogBinding
import com.example.pronedvizapp.requests.models.CallsRecords
import com.example.pronedvizapp.requests.models.ITaskStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class CallsInGroupAdapter(private val context: Context, private val callsInfo: List<CallInfo>, private val callsRecords: List<CallsRecords>) : BaseAdapter() {

    override fun getCount(): Int = callsInfo.size

    override fun getItem(position: Int): CallInfo = callsInfo[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.call_listview_item, parent, false)

        val binding = CallListviewItemBinding.bind(view)
        binding.phoneNumberTextView.text = callsInfo[position].phoneNumber
        if (callsInfo[position].callerName != null && callsInfo[position].callerName != "Неизвестный") {
            binding.phoneNumberTextView.text = binding.phoneNumberTextView.text.toString() + "(${callsInfo[position].callerName})"
        }
        binding.dateTimeTextView.text = unixDateTimeToString(callsInfo[position].dateTime)
        binding.lengthSecondsTextView.text = getFormattedTimeLength(callsInfo[position].lengthSeconds)
        binding.transcriptionButtonTextView.setOnClickListener {
            if (callsInfo[position].transcription != null && callsInfo[position].transcription != "" && callsInfo[position].transcription != NO_TRANSCRIPTION) {
                val bindingDialog = EditProfileNameDialogBinding.inflate(LayoutInflater.from(context))

                val dialog = Dialog(context)
                dialog.window?.setBackgroundDrawableResource(R.color.transparent0)

                bindingDialog.textView4.text = "Запись звонка"
                bindingDialog.cancelButton.text = "OK"
                bindingDialog.cancelButton.setOnClickListener {
                    dialog.dismiss()
                }
                bindingDialog.saveButton.text = "Переделать"
                bindingDialog.editText.visibility = View.INVISIBLE
                bindingDialog.textViewText.visibility = View.VISIBLE
                val text = callsInfo[position].transcription.replace(".", ".\n")
                bindingDialog.textViewText.text = text // потом сюда встанет нормализованный текст
                bindingDialog.saveButton.setOnClickListener { view ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val taskResponse = CallRecordingService.orderCallTranscription(context, MainStatic.currentUser!!.id, callsInfo[position].recordId, "base", MainStatic.currentToken)
                        taskResponse.onSuccess { resp ->
                            resp?.let { status ->
                                withContext(Dispatchers.Main) {
                                    showTaskStatusDialog(status)
                                    withContext(Dispatchers.Main) {
                                        binding.transcriptionButtonTextView.isEnabled = false
                                    }
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
                dialog.setContentView(bindingDialog.root)
                dialog.show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val taskResponse = CallRecordingService.orderCallTranscription(context, MainStatic.currentUser!!.id, callsInfo[position].recordId, "base", MainStatic.currentToken)
                    taskResponse.onSuccess { resp ->
                        resp?.let { status ->
                            withContext(Dispatchers.Main) {
                                showTaskStatusDialog(status)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            binding.transcriptionButtonTextView.isEnabled = false
                        }
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
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    val callRecordInfo = callsRecords.find { it.id == callsInfo[position].recordId }
                    if (callRecordInfo != null) {
                        val recordingsDir = File(Environment.getExternalStorageDirectory(), "Recordings/Call")
                        if (!recordingsDir.exists()) {
                            recordingsDir.mkdirs()
                        }
                        var audioFile = File(recordingsDir, callRecordInfo.name)
                        if (audioFile.exists()) {
                            openAudioFile(audioFile)
                        } else {
                            val tmpDir = File(Environment.getExternalStorageDirectory(), "Recordings/MiaBoxTmp")
                            if (!tmpDir.exists()) {
                                tmpDir.mkdirs()
                            }
                            audioFile = File(tmpDir, callRecordInfo.name)
                            if (audioFile.exists()) {
                                openAudioFile(audioFile)
                            } else {
                                downloadAndPlayAudioFile(callRecordInfo.name, callRecordInfo.id)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Информация о записи не найдена!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка при работе с файлом", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun openAudioFile(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = "audio/*"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        intent.setDataAndType(uri, mimeType)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Не удалось открыть аудиофайл", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Нет приложений для открытия аудиофайла", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun downloadAndPlayAudioFile(fileName: String, recordId: Int) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${context.getString(R.string.server_ip_address)}calls/get_call_record_file?user_id=${MainStatic.currentUser!!.id}&record_id=$recordId")
            .addHeader("token-authorization", MainStatic.currentToken!!)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Ошибка загрузки аудио", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val audioFile = File(Environment.getExternalStorageDirectory(), "Recordings/MiaBoxTmp/$fileName")
                        if (audioFile.exists()) {
                            openAudioFile(audioFile)
                            return@launch
                        }
                        response.body()?.let { responseBody ->
                            try {
                                val inputStream = responseBody.byteStream()
                                val outputStream = FileOutputStream(audioFile)
                                val buffer = ByteArray(4096)
                                var bytesRead: Int

                                try {
                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                        outputStream.write(buffer, 0, bytesRead)
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(context, "Ошибка записи файла: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    outputStream.close()
                                    inputStream.close()
                                }

                                CoroutineScope(Dispatchers.Main).launch {
                                    openAudioFile(audioFile)
                                }
                            } catch (e: Exception) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Ошибка при работе с файлом", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Ошибка загрузки аудиофайла", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showTaskStatusDialog(taskResponse: ITaskStatus) {
        MaterialAlertDialogBuilder(context)
            .setMessage("Запрос на расшифровку разговора отправлен.\nСтатус задачи: \"${taskResponse.status}\" \nВозвращайтесь позже и проверьте результат расшифровки (операция занимает 1 - 3 минуты)")
            .setPositiveButton("Ок") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    companion object {

        const val NO_TRANSCRIPTION = "no transcription"

        public fun getFormattedTimeLength(timeSeconds: Int): String {
            val minutes = (timeSeconds / 60F).toInt()
            val seconds = timeSeconds - minutes * 60
            return "$minutes мин. $seconds сек."
        }

        public fun unixDateTimeToString(unixDateTime: Int): String {
            val dateTime = LocalDateTime.ofEpochSecond(unixDateTime.toLong(), 0, ZoneOffset.UTC)
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            return dateTime.format(formatter)
        }

    }

}