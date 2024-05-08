package com.example.pronedvizapp.authentication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databases.DbViewModel
import com.example.pronedvizapp.databinding.FragmentAuthorizationBinding
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory

class AuthorizationFragment: Fragment() {

    lateinit var binding: FragmentAuthorizationBinding

    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAuthorizationBinding.inflate(inflater, container, false)
        preferences = this.requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        binding.gradientView.animateGradientColors()

        binding.goToRegistrationTextView.setOnClickListener {
            AuthenticationActivity.Companion.openRegistration()
        }

        if (preferences.contains("LAST_LOGIN")) {
            binding.enterLoginEditText.setText(preferences.getString("LAST_LOGIN", ""))
        }
        if (preferences.contains("LAST_PASSWORD")) {
            binding.enterPasswordEditText.setText(preferences.getString("LAST_PASSWORD", ""))
        }

//        if (binding.enterLoginEditText.text != null && binding.enterPasswordEditText.text != null) {
//            binding.completeButton.performClick() // не работает -> теперь в InitialActivity
//        }

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
                val res = RegistrationFragment.Companion.authForToken(this@AuthorizationFragment.requireContext(), login, password)
                res.onSuccess {
                    token = it
                    if (token == null) {
                        Toast.makeText(this@AuthorizationFragment.requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val resUser = getUserInfo(this@AuthorizationFragment.requireContext(), token!!)
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

                        //var tViewModel = ViewModelProvider(this@AuthorizationFragment).get(DbViewModel::class.java)

                        //            if (tViewModel.getUserByLogin(binding.enterLoginEditText.text.toString())?.value == null) {
                        //                try {
                        //                    tViewModel.insertUser(userFromServer!!)
                        //                } catch (e: Exception) {
                        //                    Toast.makeText(this@AuthorizationFragment.requireContext(), "Ошибка записи в бд", Toast.LENGTH_SHORT).show()
                        //                }
                        //            }
                        MainActivity.currentUser = userFromServer
                        MainActivity.currentToken = token

                        val editor = preferences.edit()
                        editor.putString("LAST_LOGIN", binding.enterLoginEditText.text.toString()).apply()
                        editor.putString("LAST_PASSWORD", binding.enterPasswordEditText.text.toString()).apply()
                        editor.putString("TOKEN", token).apply()

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

    companion object {

        suspend fun getUserInfo(context: Context, token: String): Result<User?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            return@coroutineScope try {
                val response = usersApi.info(token).await()
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Ошибка получения токена"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    }
}