package com.example.pronedvizapp.authentication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FragmentRegistrationBinding
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.User
import com.example.pronedvizapp.requests.models.UserTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory

class RegistrationFragment: Fragment() {

    lateinit var binding: FragmentRegistrationBinding

    private lateinit var preferences: SharedPreferences

    public val userPhotoLinks: Array<String> = arrayOf(
        "https://png.pngtree.com/png-clipart/20190421/ourmid/pngtree-creative-memphis-geometric-abstract-patterns-background-png-image_961379.jpg",
        "https://fons.pibig.info/uploads/posts/2023-05/thumbs/1685404903_fons-pibig-info-p-graficheskie-figuri-fon-pinterest-40.jpg",
        "https://fons.pibig.info/uploads/posts/2023-05/thumbs/1685404959_fons-pibig-info-p-graficheskie-figuri-fon-pinterest-52.jpg",
        "https://fons.pibig.info/uploads/posts/2023-05/thumbs/1685404910_fons-pibig-info-p-graficheskie-figuri-fon-pinterest-31.jpg",
        "https://fons.pibig.info/uploads/posts/2023-05/thumbs/1685404965_fons-pibig-info-p-graficheskie-figuri-fon-pinterest-77.jpg"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentRegistrationBinding.inflate(inflater, container, false)

        preferences = this.requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        binding.gradientView.animateGradientColors()

        binding.completeButton.setOnClickListener {
            if (binding.enterUserLoginEditText.text.toString() == "" || binding.enterPasswordEditText.text.toString() == "" || binding.enterNameEditText.text.toString() == "" || binding.enterSurnameEditText.text.toString() == "") {
                MaterialAlertDialogBuilder(this.requireContext())
                    .setMessage("Возможно вы ввели некорректные данные, проверьте правильность заполнения всех полей и попробуйде снова!")
                    .setPositiveButton("Ок") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                return@setOnClickListener
            }

            var newUser = User(0,
                binding.enterUserLoginEditText.text.toString(),
                binding.enterPasswordEditText.text.toString(),
                UserTypes.PRIVATE.description,
                userPhotoLinks.random().toString(),
                binding.enterNameEditText.text.toString() + " " + binding.enterSurnameEditText.text.toString(),
                "",
                0,
                "",
                0
            )

            var token: String? = ""
            var regResult: Int = -1
            lifecycleScope.launch {
                val resUser = regNewUser(this@RegistrationFragment.requireContext(), newUser)
                resUser.onSuccess {
                    regResult = it
                    newUser.id = it

                    val res = authForToken(this@RegistrationFragment.requireContext(), newUser.login, newUser.password)
                    res.onSuccess {
                        token = it
                        if (token == null) {
                            Toast.makeText(this@RegistrationFragment.requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        //            var tViewModel = ViewModelProvider(this@RegistrationFragment).get(DbViewModel::class.java)
                        //
                        //            if (tViewModel.getUserByLogin(binding.enterUserLoginEditText.text.toString()) == null) {
                        //                tViewModel.insertUser(newUser)
                        //            } else {
                        //                Toast.makeText(this@RegistrationFragment.requireContext(), "Ошибка записи в бд", Toast.LENGTH_SHORT).show()
                        //            }

                        MainActivity.currentToken = token
                        MainActivity.currentUser = newUser

                        val editor = preferences.edit()
                        editor.putString("LAST_LOGIN", newUser.login).apply()
                        editor.putString("LAST_PASSWORD", newUser.password).apply()
                        editor.putString("TOKEN", token).apply()

                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        this@RegistrationFragment.requireActivity().finish()
                    }
                    res.onFailure {
                        token = null
                        Toast.makeText(this@RegistrationFragment.requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }
                resUser.onFailure {
                    regResult = -1
                    Toast.makeText(this@RegistrationFragment.requireContext(), "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }
        }

        binding.goBackImageButton.setOnClickListener {
            AuthenticationActivity.Companion.openAuthorization()
        }

        binding.goToAuthorizationTextView.setOnClickListener {
            AuthenticationActivity.Companion.openAuthorization()
        }

        return binding.root
    }

    companion object {

        suspend fun authForToken(context: Context, login: String, password: String): Result<String?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            return@coroutineScope try {
                val response = usersApi.authorization(login, password).await()
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Ошибка авторизации"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun regNewUser(context: Context, newUser: User): Result<Int> = coroutineScope  {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            return@coroutineScope try {
                val response = usersApi.registration(newUser).await()
                if (response != -1) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Ошибка авторизации"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    }
}