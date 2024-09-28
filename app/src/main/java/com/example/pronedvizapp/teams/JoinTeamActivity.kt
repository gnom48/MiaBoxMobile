package com.example.pronedvizapp.teams

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.databinding.ActivityJoinTeamBinding
import com.example.pronedvizapp.requests.RequestsRepository.joinToTeam
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class JoinTeamActivity : AppCompatActivity() {

    lateinit var binding: ActivityJoinTeamBinding
    var joinResult: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinTeamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanQrCodeImageButton.setOnClickListener {
            val intent = Intent(this, QrCodeScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val jsonData = data?.getStringExtra(RETURN_KEY)
                try {
                    val inviteData: QrCodeData = Gson().fromJson(jsonData, QrCodeData::class.java)
                    val secret = encodeSecret(this@JoinTeamActivity)

                    if (inviteData.secret != secret) {
                        binding.scanResultTextView.text = "Не верный QR-код, попробуйте запросить новый  повторить попытку!"
                        return
                    }
                    val isWithinTwoDaysPast = run { val targetUnixTime = inviteData.qrCodeDatetime; LocalDateTime.ofEpochSecond(targetUnixTime, 0, ZoneOffset.UTC) }.run { LocalDateTime.now().minusDays(2).isBefore(this) && this.isBefore(LocalDateTime.now()) }
                    if (!isWithinTwoDaysPast) {
                        binding.scanResultTextView.text = "Ваш QR-код приглашение устарел, попробуйте запросить новый у администратора!"
                        return
                    }
                    binding.scanResultTextView.text = "Ваша новая команда: \"${inviteData.teamId}\""
                    lifecycleScope.launch {
                        val res = joinToTeam(this@JoinTeamActivity, MainStatic.currentToken!!, inviteData.teamId, inviteData.authorId)
                        res.onSuccess {
                            binding.scanResultTextView.text = "Вы успешно присоединились к команде!\nПерейдите к списку команд и посмотрите."
                            CoroutineScope(Dispatchers.IO).launch {
                                Thread.sleep(5)
                            }
                            this@JoinTeamActivity.finish()
                        }
                        res.onFailure {
                            Toast.makeText(this@JoinTeamActivity, it.message, Toast.LENGTH_SHORT).show()
                            binding.scanResultTextView.text = "Ошибка ${it.message}"
                        }
                    }

                } catch (e: JsonSyntaxException) {
                    Toast.makeText(this@JoinTeamActivity, "Не корректный QR-код!", Toast.LENGTH_SHORT).show()
                    binding.scanResultTextView.text = "Не корректный QR-код!"
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 48
        const val RETURN_KEY: String = "JSON_DATA"
    }
}