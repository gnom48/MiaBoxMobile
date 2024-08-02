package com.example.pronedvizapp.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.example.pronedvizapp.IFragmentTag
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.MainInfoAdapter
import com.example.pronedvizapp.databinding.FragmentProfileBinding
import com.example.pronedvizapp.adapters.MainInfoForCard
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
            bindUserImageFileAsync(this@ProfileFragment.requireContext().applicationContext, binding.photoImageView)
//            bindUserImageAsync(this@ProfileFragment.requireContext().applicationContext, binding.photoImageView)
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

        public suspend fun bindUserImageFileAsync(context: Context, imageView: ImageView) = coroutineScope {
            val outputDir = File(context.filesDir, "images")
            if (outputDir.exists()) {
                val file = File(outputDir, "img-${MainStatic.currentUser!!.id}-${MainStatic.currentUser!!.image}.jpg")
                if (file.exists()) {
                    val inputStream = file.inputStream()
                    val buffer = inputStream.readBytes()
                    inputStream.close()

                    val bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

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

            try {
                val response = usersApi.getImageFileByUser(MainStatic.currentToken!!)
                if (response.isSuccessful && response.body() != null) {
                    val inputStream: InputStream = response.body()!!.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                    }

                    imageView.setImageBitmap(bitmap)

                    val file = File(context.filesDir, "images/img-${MainStatic.currentUser!!.id}-${MainStatic.currentUser!!.image}.jpg")
                    try {
                        val outputStream = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        outputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    imageView.setImageResource(R.drawable.avatar)
                    Toast.makeText(context, "Ошибка получения картинки", Toast.LENGTH_SHORT).show()
                }
            } catch (e: HttpException) {
                imageView.setImageResource(R.drawable.avatar)
                Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
            }
        }
    }
}