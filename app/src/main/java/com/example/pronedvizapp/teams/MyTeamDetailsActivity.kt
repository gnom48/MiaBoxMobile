package com.example.pronedvizapp.teams

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.TeamMembersAdapter
import com.example.pronedvizapp.databinding.ActivityMyTeamDetailsBinding
import com.example.pronedvizapp.databinding.FragmentInviteToTeamBinding
import com.example.pronedvizapp.requests.ServerApiTeams
import com.example.pronedvizapp.requests.models.UserStatuses
import com.example.pronedvizapp.requests.models.UserTeamsWithInfoItem
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.EnumMap


class MyTeamDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityMyTeamDetailsBinding
    lateinit var teamInfo: UserTeamsWithInfoItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTeamDetailsBinding.inflate(layoutInflater)
        setSupportActionBar(binding.constraintLayout)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setContentView(binding.root)

        teamInfo = Gson().fromJson<UserTeamsWithInfoItem>(intent.getStringExtra("data"), UserTeamsWithInfoItem::class.java)
        binding.teamNameTextView.text = teamInfo.team.name

        val aboutMeInTeam = teamInfo.members.find { it.user.id == MainStatic.currentUser!!.id }
        binding.recyclerView.adapter = TeamMembersAdapter(teamInfo.members, this, this, aboutMeInTeam!!.role == UserStatuses.OWNER, teamInfo.team)
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.team_optional_menu_res, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return  when (item.itemId) {
            R.id.leaveTeamMenuItem -> {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Подтверждение")
                    .setMessage("Вы действительно хотите покинуть командное пространство?\nВ случае необходимости, Вы всегда сможете обратиться к администратору и вернуться в команду.")
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Да") { dialog, _ ->
                        val retrofit = Retrofit.Builder()
                            .baseUrl(getString(R.string.server_ip_address))
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        val teamsApi = retrofit.create(ServerApiTeams::class.java)

                        val resp = teamsApi.leaveTeam(teamInfo.team.id, MainStatic.currentToken!!)
                        resp.enqueue(object : Callback<Boolean> {
                            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                                if (response.isSuccessful) {
                                    this@MyTeamDetailsActivity.finish()
                                    if (response.body() == true) {
                                        Toast.makeText(this@MyTeamDetailsActivity, "Вы покинули команду", Toast.LENGTH_SHORT).show()
                                        this@MyTeamDetailsActivity.finish()
                                        return
                                    }
                                    Toast.makeText(this@MyTeamDetailsActivity, "Ошибка! Повторите попытку позже.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                                Toast.makeText(this@MyTeamDetailsActivity, t.message, Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    .create()
                dialog.show()

                true
            }
            R.id.inviteTeamMenuItem -> {
                val jsonData = Gson().toJson(QrCodeData(
                    teamInfo.team.id,
                    MainStatic.currentUser!!.id,
                    LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                    encodeSecret(this@MyTeamDetailsActivity)))
                val bitmap = generateQRCode(jsonData)
                bitmap?.let { showQRCodeAlertDialog(it) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun generateQRCode(input: String): Bitmap? {
        val size = 500
        val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 0
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(input, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            return bitmap
        } catch (e: WriterException) {
            Log.e("QRCode", "Failed to encode QR Code", e)
            return null
        }
    }

    private fun showQRCodeAlertDialog(bitmap: Bitmap) {
        val bindingDialog = FragmentInviteToTeamBinding.inflate(LayoutInflater.from(this))

        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
        dialog.setContentView(bindingDialog.root)
        dialog.show()

        bindingDialog.qrCodeImageView.setImageBitmap(bitmap)

        bindingDialog.closeImageButton.setOnClickListener {
            dialog.dismiss()
        }
    }
}