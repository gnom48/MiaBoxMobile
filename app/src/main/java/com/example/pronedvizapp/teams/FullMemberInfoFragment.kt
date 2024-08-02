package com.example.pronedvizapp.teams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.example.pronedvizapp.CallsActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.adapters.AddressesAdapter
import com.example.pronedvizapp.adapters.CallsAdapter
import com.example.pronedvizapp.adapters.FullStatisticsAdapter
import com.example.pronedvizapp.databinding.FullMemberInfoDialogBinding
import com.example.pronedvizapp.requests.models.Member

class FullMemberInfoFragment : Fragment() {

    private lateinit var binding: FullMemberInfoDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FullMemberInfoDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item = arguments?.getSerializable("item") as Member

        binding.navigationRailView.selectedItemId = R.id.navigation_statistics
        binding.navigationRailView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_statistics -> {
                    binding.statisticsRecyclerView.visibility = View.VISIBLE
                    binding.callsRecyclerView.visibility = View.GONE
                    binding.addressesMemberListView.visibility = View.GONE
                    true
                }
                R.id.navigation_calls -> {
                    binding.statisticsRecyclerView.visibility = View.GONE
                    binding.callsRecyclerView.visibility = View.VISIBLE
                    binding.addressesMemberListView.visibility = View.GONE
                    true
                }
                R.id.navigation_addresses -> {
                    binding.statisticsRecyclerView.visibility = View.GONE
                    binding.callsRecyclerView.visibility = View.GONE
                    binding.addressesMemberListView.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }

        binding.closeImageButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        }

        binding.callsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val groupedCalls = CallsActivity.groupCallsByDate(item.calls)
        binding.callsRecyclerView.adapter = CallsAdapter(this.requireContext(), groupedCalls)

        val addresses = item.addresses.map { it.address }
        binding.addressesMemberListView.adapter = AddressesAdapter(this.requireContext(), addresses)

        binding.statisticsRecyclerView.adapter = FullStatisticsAdapter(item.statistics, this.requireContext())
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.statisticsRecyclerView)
        binding.statisticsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.aboutUserTextView.text = "${item.user.name} (${item.user.login})"

        binding.closeImageButton.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }
}
