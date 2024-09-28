package com.example.pronedvizapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebSettings
import com.example.pronedvizapp.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        binding.mainWebView.settings.javaScriptEnabled = true
        val src = intent.getStringExtra(SOURCE)
        if (src != null) {
            loadHtmlFromAssets(src)
        } else {
            loadHtmlFromAssets("not_found.html")
        }
    }

    private fun loadHtmlFromAssets(fileName: String) {
        val inputStream = assets.open(fileName)
        val htmlContent = inputStream.bufferedReader().use { it.readText() }
        binding.mainWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    companion object {
        const val SOURCE = "SOURCE"
    }
}