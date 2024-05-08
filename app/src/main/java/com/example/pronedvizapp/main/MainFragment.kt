package com.example.pronedvizapp.main

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.pronedvizapp.AllTeamsActivity
import com.example.pronedvizapp.EditProfileActivity
import com.example.pronedvizapp.KnowledgeBaseActivity
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MapActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.ResultsActivity
import com.example.pronedvizapp.WebViewActivity
import com.example.pronedvizapp.databinding.FragmentMainBinding
import com.squareup.picasso.Picasso

class MainFragment : Fragment() {

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

        binding.userNameTextView.setText(MainActivity.currentUser!!.name)
        binding.avatarImageView.setImageDrawable(this.requireContext().getDrawable(R.drawable.avatar))
        binding.avatarImageView.setForeground(getResources().getDrawable(R.drawable.image_view_rounded_corners, null))

        // TODO: Picasso
//        Picasso.get()
//            .load(MainActivity.currentUser!!.photo + "потом убрать")
//            .error(R.drawable.default_avatar)
//            .resize(110, 110)
//            .centerInside()
//            .into(binding.avatarImageView)

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
            startActivity(intent)
        }

        binding.resultsConstraintLayout.setOnClickListener {
            val intent = Intent(this.requireContext(), ResultsActivity::class.java)
            startActivity(intent)
        }

        binding.myTeamConstraintLayout.setOnClickListener {
//            val intent = Intent(this.requireContext(), AllTeamsActivity::class.java)
//            startActivity(intent)
        }

        return binding.root
    }
}