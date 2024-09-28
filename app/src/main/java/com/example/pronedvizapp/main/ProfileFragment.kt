package com.example.pronedvizapp.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.example.pronedvizapp.IFragmentTag
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.adapters.MainInfoAdapter
import com.example.pronedvizapp.adapters.MainInfoForCard
import com.example.pronedvizapp.databinding.FragmentProfileBinding
import com.example.pronedvizapp.requests.RequestsRepository.bindUserImageFileAsync
import com.example.pronedvizapp.requests.RequestsRepository.getUserStatisticsByPeriod
import com.example.pronedvizapp.requests.models.DAY_STATISTICS_PERIOD
import kotlinx.coroutines.launch

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
            bindUserImageFileAsync(this@ProfileFragment.requireContext().applicationContext, binding.photoImageView, MainStatic.currentUser.image)
        }

        binding.userNameTextView.text = MainStatic.currentUser.name

        val targetsList = arrayListOf(
            MainInfoForCard("15", "Звонков", "Нужно сделать в день"),
            MainInfoForCard("5", "Договоров", "Нужно заключить за месяц"),
            MainInfoForCard("50", "Листовок", "Нужно расклеить за день")
        )

        var achievesList = arrayListOf(MainInfoForCard("", "Здесь будут", "Ваши результаты"))

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
                val userStatics = getUserStatisticsByPeriod(DAY_STATISTICS_PERIOD, this@ProfileFragment.requireContext(), MainStatic.currentToken!!)
                userStatics.onSuccess {
                    achievesList = arrayListOf(MainInfoForCard(it.calls.toString(), "Из 15 звонков", "Совершено"),
                        MainInfoForCard((it.dealsSale + it.dealsRent).toString(), "Договоров", "Заключено"),
                        MainInfoForCard(it.flyers.toString(), "Из 50 объявлений", "Расклеено"))
                    binding.mainInfoRecyclerView.adapter = MainInfoAdapter(achievesList)
                }
                userStatics.onCached {
                    achievesList = arrayListOf(MainInfoForCard(it.calls.toString(), "Из 15 звонков", "Совершено"),
                        MainInfoForCard((it.dealsSale + it.dealsRent).toString(), "Договоров", "Заключено"),
                        MainInfoForCard(it.flyers.toString(), "Из 50 объявлений", "Расклеено"))
                    binding.mainInfoRecyclerView.adapter = MainInfoAdapter(achievesList)
                }
            }
        }

        return binding.root
    }
}