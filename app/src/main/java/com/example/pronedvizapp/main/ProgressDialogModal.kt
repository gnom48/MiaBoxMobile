package com.example.pronedvizapp.main

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.example.pronedvizapp.R

class ProgressDialogModal(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.progress_dialog_modal)
        setCancelable(false)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}