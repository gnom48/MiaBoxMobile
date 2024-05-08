package com.example.pronedvizapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.pronedvizapp.databinding.ActivityKnoledgeBaseBinding

class KnowledgeBaseActivity : AppCompatActivity() {

    lateinit var binding: ActivityKnoledgeBaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKnoledgeBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goBackPanel.setOnClickListener {
            this@KnowledgeBaseActivity.finish()
        }
    }
}