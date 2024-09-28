package com.example.pronedvizapp.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pronedvizapp.InitialActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.ActivityAuthenticationBinding

class AuthenticationActivity : AppCompatActivity() {

    lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localContext = this

        val mIntent = Intent(this, InitialActivity::class.java)
        mIntent.putExtra("IS_OLD_ENTER", intent.getBooleanExtra("IS_OLD_ENTER", true))
        startActivity(mIntent)

        openAuthorization()
    }

    companion object {

        var localContext: AuthenticationActivity? = null

        const val OFFLINE_TOKEN = "OFFLINE_TOKEN"

        public fun openAuthorization() {
            val fragmentTransaction = localContext!!.supportFragmentManager.beginTransaction()
            val authorizationFragment = AuthorizationFragment()
            fragmentTransaction.replace(R.id.authenticationContentFrameLayout, authorizationFragment)
            fragmentTransaction.commit()
        }

        public fun openRegistration() {
            val fragmentTransaction = localContext!!.supportFragmentManager.beginTransaction()
            val registrationFragment = RegistrationFragment()
            fragmentTransaction.replace(R.id.authenticationContentFrameLayout, registrationFragment)
            fragmentTransaction.commit()
        }

    }
}