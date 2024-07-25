package com.example.pronedvizapp.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import androidx.work.ListenableWorker.Result.Failure
import com.example.pronedvizapp.EditProfileActivity
import com.example.pronedvizapp.IFragmentTag
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.MainInfoAdapter
import com.example.pronedvizapp.databinding.FragmentProfileBinding
import com.example.pronedvizapp.adapters.MainInfoForCard
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.example.pronedvizapp.requests.models.Image
import com.example.pronedvizapp.requests.models.Statistics
import com.example.pronedvizapp.requests.models.StatisticsWithKpi
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class ProfileFragment(override val fragmentNavigationTag: String = "ProfileFragment") : Fragment(), IFragmentTag {

    lateinit var binding: FragmentProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentProfileBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            bindUserImageAsync(this@ProfileFragment.requireContext().applicationContext, binding.photoImageView)
        }

        binding.userNameTextView.setText(MainStatic.currentUser!!.name)

        val targetsList = arrayListOf(
            MainInfoForCard("15", "Звонков", "Нужно сделать в день"),
            MainInfoForCard("5", "Договоров", "Нужно заключить за месяц"),
            MainInfoForCard("50", "Листовок", "Нужно расклеить за день")
        )

        var achivesList = arrayListOf(
            MainInfoForCard("", "Здесь будут", "Ваши результаты")
        )

        binding.mainInfoRecyclerView.layoutManager = LinearLayoutManager(this.requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.mainInfoRecyclerView)
        binding.mainInfoRecyclerView.adapter = MainInfoAdapter(targetsList)

        binding.targetsButton.isSelected = true

        binding.targetsButton.setOnClickListener {
            binding.targetsButton.isSelected = true
            binding.achivesButton.isSelected = false
            binding.mainInfoRecyclerView.adapter = MainInfoAdapter(targetsList)
        }

        binding.achivesButton.setOnClickListener {
            binding.targetsButton.isSelected = false
            binding.achivesButton.isSelected = true
            lifecycleScope.launch {
                val userStatics = MainActivity.getUserStatistics(DAY_STATISTICS_PERIOD, this@ProfileFragment.requireContext(), MainStatic.currentToken!!)
                userStatics.onSuccess {
                    achivesList = arrayListOf(MainInfoForCard(it.calls.toString(), "Из 15 звонков", "Совершено"),
                        MainInfoForCard(it.deals.toString(), "Из 5 договоров", "Заключено"),
                        MainInfoForCard(it.flyers.toString(), "Из 50 объявлений", "Расклеено"))
                    binding.mainInfoRecyclerView.adapter = MainInfoAdapter(achivesList)
                }
            }
        }

        return binding.root
    }

    companion object {

        public suspend fun bindUserImageAsync(context: Context, imageView: ImageView) = coroutineScope {
            val outputDir = File(context.filesDir, "images")
            if (outputDir.exists()) {
                val file = File(outputDir, "img-${MainStatic.currentUser!!.id}-${MainStatic.currentUser!!.image}.jpg")
                if (file.exists()) {
                    val inputStream = file.inputStream()
                    val buffer = inputStream.readBytes()
                    inputStream.close()

                    val bitmap =  BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

                    imageView.setImageBitmap(bitmap)
                    return@coroutineScope
                }
            } else {
                outputDir.mkdirs()
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            val resp = usersApi.getImageByUser(MainStatic.currentToken!!)
            resp.enqueue(object : Callback<Image?> {
                override fun onResponse(call: Call<Image?>, response: Response<Image?>) {
                    if (response.isSuccessful && response.body() != null) {
                        val image = response.body()!!
                        val separatedData = image.data.split(",")
                        val imageData: ByteArray
                        if (separatedData.size > 1) {
                            imageData = Base64.decode(separatedData[1], Base64.DEFAULT)
                        } else if (separatedData.size == 1) {
                            imageData = Base64.decode(separatedData[0], Base64.DEFAULT)
                        } else {
                            onFailure(call, Throwable("base64 error"))
                            return
                        }
                        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                        imageView.setImageBitmap(bitmap)

                        val file = File(context.filesDir, "images/img-${MainStatic.currentUser!!.id}-${MainStatic.currentUser!!.image}.jpg")
                        try {
                            val outputStream = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            outputStream.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return
                    }
                    imageView.setImageResource(R.drawable.avatar)
                    Toast.makeText(context, "Ошибка получения картинки", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<Image?>, t: Throwable) {
                    imageView.setImageResource(R.drawable.avatar)
                    Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }
}