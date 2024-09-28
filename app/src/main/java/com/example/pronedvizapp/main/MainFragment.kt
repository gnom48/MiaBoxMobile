package com.example.pronedvizapp.main

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.CallsActivity
import com.example.pronedvizapp.EditProfileActivity
import com.example.pronedvizapp.IFragmentTag
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.MapActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.ResultsActivity
import com.example.pronedvizapp.WebViewActivity
import com.example.pronedvizapp.WebViewActivity.Companion.SOURCE
import com.example.pronedvizapp.bisness.calls.CallRecordingService
import com.example.pronedvizapp.bisness.geo.GeoPositionService
import com.example.pronedvizapp.bisness.isServiceRunning
import com.example.pronedvizapp.databinding.FragmentMainBinding
import com.example.pronedvizapp.requests.RequestsRepository.bindUserImageFileAsync
import com.example.pronedvizapp.teams.AllTeamsActivity
import kotlinx.coroutines.launch

class MainFragment(override val fragmentNavigationTag: String = "MainFragment") : Fragment(), IFragmentTag {

    lateinit var binding: FragmentMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentMainBinding.inflate(inflater, container, false)

        binding.userNameTextView.setText(MainStatic.currentUser!!.name)
        binding.avatarImageView.setImageDrawable(this.requireContext().getDrawable(R.drawable.avatar))
        binding.avatarImageView.setForeground(getResources().getDrawable(R.drawable.image_view_rounded_corners, null))

        lifecycleScope.launch {
            bindUserImageFileAsync(this@MainFragment.requireContext().applicationContext, binding.avatarImageView, MainStatic.currentUser.image)
        }
        
        binding.personalDataConstraintLayout.setOnClickListener {
            val intent = Intent(this.requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.takeConsultationConstraintLayout.setOnClickListener {
            try {
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "egorchima@gmail.com", null))
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject")
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Body")
                startActivity(Intent.createChooser(emailIntent, "Send email..."))
            } catch (e: Exception) {}
            catch (e: Error) {}
        }

        binding.myAddressesConstraintLayout.setOnClickListener {
            val intent = Intent(this.requireContext(), MapActivity::class.java)
            startActivity(intent)
        }

        binding.knowledgeBaseConstraintLayout.setOnClickListener {
            val intent = Intent(this.requireContext(), WebViewActivity::class.java)
            intent.putExtra(SOURCE, "baza.html")
            startActivity(intent)
        }

        binding.resultsConstraintLayout.setOnClickListener {
            val intent = Intent(this.requireContext(), ResultsActivity::class.java)
            startActivity(intent)
        }

        binding.myTeamConstraintLayout.setOnClickListener {
            val intent = Intent(this.requireContext(), AllTeamsActivity::class.java)
            startActivity(intent)
        }

        if (isServiceRunning(this.requireContext(), CallRecordingService::class.java)) {
            binding.recordMyCallsSwitch.isChecked = true
            binding.recordMyCallsSwitch.isEnabled = false
        }
        binding.recordMyCallsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val context = this.requireContext()
            if (isChecked) {
                if (!isServiceRunning(context, CallRecordingService::class.java)) {
                    ContextCompat.startForegroundService(context, Intent(context, CallRecordingService::class.java))
                    binding.recordMyCallsSwitch.isEnabled = false
                }
            } else {
                if (isServiceRunning(context, CallRecordingService::class.java)) {
                    context.stopService(Intent(context, CallRecordingService::class.java))
                }
            }
        }

        if (isServiceRunning(this.requireContext(), GeoPositionService::class.java)) {
            binding.recordMyCoordsSwitch.isChecked = true
            binding.recordMyCoordsSwitch.isEnabled = false
        }
        binding.recordMyCoordsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val context = this.requireContext()
            if (isChecked) {
                if (!isServiceRunning(context, GeoPositionService::class.java)) {
                    ContextCompat.startForegroundService(context, Intent(context, GeoPositionService::class.java))
                    binding.recordMyCoordsSwitch.isEnabled = false
                }
            } else {
                if (isServiceRunning(context, GeoPositionService::class.java)) {
                    context.stopService(Intent(context, GeoPositionService::class.java))
                }
            }
        }

        binding.recordMyCallsConstraintLayout.setOnClickListener {
            val intent = Intent(this.requireContext(), CallsActivity::class.java)
            startActivity(intent)
        }

        binding.aboutAppTextView.setOnClickListener {
            val intent = Intent(this.requireContext(), WebViewActivity::class.java)
            intent.putExtra(SOURCE, "about.html")
            startActivity(intent)
        }

        return binding.root
    }
}