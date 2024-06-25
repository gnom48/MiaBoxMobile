package com.example.pronedvizapp.bisness.calls

import android.content.Context
import android.media.MediaRecorder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.example.pronedvizapp.bisness.calls.CallRecordingService.Companion.DEBUG_TAG
import java.io.File
import java.io.IOException
import java.lang.RuntimeException

class CallRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null

    public fun startRecording() {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        val outputDir = File(context.filesDir, "Recordings")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, "call_recording_${System.currentTimeMillis()}.mp4")
        mediaRecorder?.setOutputFile(outputFile.absolutePath)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            Log.d(DEBUG_TAG, "Start recording ok")
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "Failed to start recording", e)
            mediaRecorder = null
        }
    }

    public fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            Log.d(DEBUG_TAG, "Stop recording")
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "Stop recording error")
            mediaRecorder = null
        }
    }
}
