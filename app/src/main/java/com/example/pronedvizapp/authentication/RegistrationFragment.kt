package com.example.pronedvizapp.authentication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FragmentRegistrationBinding
import com.example.pronedvizapp.requests.RequestsRepository.authForToken
import com.example.pronedvizapp.requests.RequestsRepository.regNewUser
import com.example.pronedvizapp.requests.models.User
import com.example.pronedvizapp.requests.models.UserTypes
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class RegistrationFragment: Fragment() {

    lateinit var binding: FragmentRegistrationBinding
    private lateinit var preferences: SharedPreferences
    private var newUser: User = User("0", "", "", "", "", "", "", 0, "", 0, "0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentRegistrationBinding.inflate(inflater, container, false)

        binding.toggleButtonGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val selectedButton = group.findViewById<MaterialButton>(checkedId)
                val unselectedButton: MaterialButton
                if (checkedId == R.id.commercialButton) {
                    unselectedButton = binding.privateButton
                    newUser.type = UserTypes.COMMERCIAL.description
                } else {
                    unselectedButton = binding.commercialButton
                    newUser.type = UserTypes.PRIVATE.description
                }

                updateButtonStyles(selectedButton, unselectedButton)
            }
        }

        preferences = this.requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        binding.gradientView.animateGradientColors()

        binding.completeButton.setOnClickListener {
            if (binding.enterUserLoginEditText.text.toString() == "" || binding.enterPasswordEditText.text.toString() == "" || binding.enterNameEditText.text.toString() == "") {
                MaterialAlertDialogBuilder(this.requireContext())
                    .setMessage("Возможно вы ввели некорректные данные, проверьте правильность заполнения всех полей и попробуйте снова!")
                    .setPositiveButton("Ок") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                return@setOnClickListener
            }

            newUser = User(
                id = "",
                login = binding.enterUserLoginEditText.text.toString(),
                password = binding.enterPasswordEditText.text.toString(),
                type = newUser.type,
                email = binding.enterUserEmailEditText.text.toString(),
                name = binding.enterNameEditText.text.toString(),
                gender = "",
                birthday = 0,
                phone = "",
                regDate = System.currentTimeMillis() / 1000,
                image = "1"
            )

            var token: String? = ""
            var regResult: String = ""
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
                        MainStatic.currentToken = token as String
                        MainStatic.currentUser = newUser

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
                    regResult = ""
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

    override fun onResume() {
        super.onResume()
        binding.toggleButtonGroup.check(R.id.privateButton)
        updateButtonStyles(binding.privateButton, binding.commercialButton)
    }

    private fun updateButtonStyles(selectedButton: MaterialButton, unselectedButton: MaterialButton) {
        selectedButton.setTextColor(resources.getColor(android.R.color.black))
        selectedButton.backgroundTintList = resources.getColorStateList(android.R.color.white)

        unselectedButton.setTextColor(resources.getColor(android.R.color.white))
        unselectedButton.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.transparent0))
        unselectedButton.strokeColor = resources.getColorStateList(android.R.color.white)
        unselectedButton.strokeWidth = 2
    }
}