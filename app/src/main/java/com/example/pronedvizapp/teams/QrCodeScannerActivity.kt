package com.example.pronedvizapp.teams

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.pronedvizapp.databinding.ActivityQrCodeScannerBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

class QrCodeScannerActivity: AppCompatActivity() {

    lateinit var binding: ActivityQrCodeScannerBinding

    private lateinit var barcodeView: DecoratedBarcodeView

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                barcodeView.resume()
            }
        }

    private val resText = MutableLiveData("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrCodeScannerBinding.inflate(layoutInflater)
        barcodeView = binding.barcodeScanner
        val formats = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39)
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        barcodeView.initializeFromIntent(intent)
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (result.text == null || result.text == resText.value) {
                    return
                }
                resText.value = result.text
            }
        }
        barcodeView.decodeContinuous(callback)
        setContentView(binding.root)

        resText.observe(this, Observer { str: String ->
            if (str == "") {
                return@Observer
            }
            val resultIntent = Intent()
            resultIntent.putExtra("JSON_DATA", str)
            setResult(Activity.RESULT_OK, resultIntent)
            this.finish()
        })
    }

    override fun onResume() {
        super.onResume()
        requestPermission.launch(Manifest.permission.CAMERA)
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}