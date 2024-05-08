package com.example.pronedvizapp.main

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.MainInfoAdapter
import com.example.pronedvizapp.databinding.FragmentProfileBinding
import com.example.pronedvizapp.adapters.MainInfoForCard
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    lateinit var binding: FragmentProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentProfileBinding.inflate(inflater, container, false)
        // TODO: Picasso
//        Picasso.get()
//            .load(MainActivity.currentUser!!.photo + "потом убрать")
//            .error(R.drawable.default_avatar)
//            .resize(110, 110)
//            .centerInside()
//            .into(binding.photoImageView)

        binding.userNameTextView.setText(MainActivity.currentUser!!.name)

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
                val userStatics = MainActivity.getUserStatistics(DAY_STATISTICS_PERIOD, this@ProfileFragment.requireContext(), MainActivity.currentToken!!)
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
}