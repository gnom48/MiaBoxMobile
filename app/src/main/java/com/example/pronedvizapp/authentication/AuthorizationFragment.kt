package com.example.pronedvizapp.authentication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.bisness.SharedPreferencesHelper
import com.example.pronedvizapp.databinding.FragmentAuthorizationBinding
import com.example.pronedvizapp.requests.RequestsRepository.authForToken
import com.example.pronedvizapp.requests.RequestsRepository.getUserInfo
import com.example.pronedvizapp.requests.RequestsRepository.usingLocalDataToast
import com.example.pronedvizapp.requests.models.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AuthorizationFragment: Fragment() {

    lateinit var binding: FragmentAuthorizationBinding

    private val preferences: SharedPreferences by lazy { this@AuthorizationFragment.requireContext().getSharedPreferences(SharedPreferencesHelper.SETTINGS_PREFS_KEY, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAuthorizationBinding.inflate(inflater, container, false)

        binding.gradientView.animateGradientColors()

        binding.goToRegistrationTextView.setOnClickListener {
            AuthenticationActivity.Companion.openRegistration()
        }

        if (preferences.contains(SharedPreferencesHelper.LAST_LOGIN_TAG)) {
            binding.enterLoginEditText.setText(preferences.getString(SharedPreferencesHelper.LAST_LOGIN_TAG, ""))
        }
        if (preferences.contains(SharedPreferencesHelper.LAST_PASSWORD_TAG)) {
            binding.enterPasswordEditText.setText(preferences.getString(SharedPreferencesHelper.LAST_PASSWORD_TAG, ""))
        }

        binding.completeButton.setOnClickListener {

            if (binding.enterLoginEditText.text.toString() == "" || binding.enterPasswordEditText.text.toString() == "") {
                MaterialAlertDialogBuilder(this.requireContext())
                    .setMessage("Возможно вы ввели некорректные данные, проверьте правильность заполнения всех полей и попробуйде снова!")
                    .setPositiveButton("Ок") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                return@setOnClickListener
            }

            val login = binding.enterLoginEditText.text.toString()
            val password = binding.enterPasswordEditText.text.toString()

            var userFromServer: User? = null
            var token: String? = null
            lifecycleScope.launch {
                val res = authForToken(this@AuthorizationFragment.requireContext(), login, password)
                res.onSuccess {
                    token = it
                    if (token == null) {
                        Toast.makeText(this@AuthorizationFragment.requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val resUser = getUserInfo(this@AuthorizationFragment.requireContext(), login, password, token!!)
                    resUser.onSuccess {
                        userFromServer = it

                        if (userFromServer == null) {
                            Toast.makeText(this@AuthorizationFragment.requireContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (!(userFromServer!!.login == binding.enterLoginEditText.text.toString() &&
                                    userFromServer!!.password == binding.enterPasswordEditText.text.toString())) {
                            Toast.makeText(this@AuthorizationFragment.requireContext(), "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        MainStatic.currentUser = userFromServer as User
                        MainStatic.currentToken = token as String

                        val editor = preferences.edit()
                        editor.putString(SharedPreferencesHelper.LAST_LOGIN_TAG, binding.enterLoginEditText.text.toString()).apply()
                        editor.putString(SharedPreferencesHelper.LAST_PASSWORD_TAG, binding.enterPasswordEditText.text.toString()).apply()
                        editor.putString(SharedPreferencesHelper.TOKEN_TAG, token).apply()

                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        this@AuthorizationFragment.requireActivity().finish()
                    }
                    resUser.onCached {
                        userFromServer = it

                        if (userFromServer == null) {
                            Toast.makeText(this@AuthorizationFragment.requireContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        usingLocalDataToast(this@AuthorizationFragment.requireContext())

                        if (!(userFromServer!!.login == binding.enterLoginEditText.text.toString() &&
                                    userFromServer!!.password == binding.enterPasswordEditText.text.toString())) {
                            Toast.makeText(this@AuthorizationFragment.requireContext(), "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        MainStatic.currentUser = userFromServer as User
                        MainStatic.currentToken = token as String

                        val editor = preferences.edit()
                        editor.putString(SharedPreferencesHelper.LAST_LOGIN_TAG, binding.enterLoginEditText.text.toString()).apply()
                        editor.putString(SharedPreferencesHelper.LAST_PASSWORD_TAG, binding.enterPasswordEditText.text.toString()).apply()
                        editor.putString(SharedPreferencesHelper.TOKEN_TAG, token).apply()

                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        this@AuthorizationFragment.requireActivity().finish()
                    }
                    resUser.onFailure {
                        userFromServer = null
                        Toast.makeText(this@AuthorizationFragment.requireContext(), "Ошибка авторизации с токеном", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }
                res.onFailure {
                    token = null
                    Toast.makeText(this@AuthorizationFragment.requireContext(), "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }
        }

        return binding.root
    }
}