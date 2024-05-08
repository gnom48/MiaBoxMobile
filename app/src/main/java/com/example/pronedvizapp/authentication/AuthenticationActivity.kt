package com.example.pronedvizapp.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.ActivityAuthenticationBinding

class AuthenticationActivity : AppCompatActivity() {

    lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        local_context = this

        val mIntent = Intent(this, InitialActivity::class.java)
        mIntent.putExtra("IS_OLD_ENTER", intent.getBooleanExtra("IS_OLD_ENTER", true))
        startActivity(mIntent)

        openAuthorization()
    }

    companion object {

        var local_context: AuthenticationActivity? = null

        public fun openAuthorization() {
            val fragmentTransaction = local_context!!.supportFragmentManager.beginTransaction()
            val authorizationFragment = AuthorizationFragment()
            fragmentTransaction.replace(R.id.authenticationContentFrameLayout, authorizationFragment)
            fragmentTransaction.commit()
        }

        public fun openRegistration() {
            val fragmentTransaction = local_context!!.supportFragmentManager.beginTransaction()
            val registrationFragment = RegistrationFragment()
            fragmentTransaction.replace(R.id.authenticationContentFrameLayout, registrationFragment)
            fragmentTransaction.commit()
        }

    }
}