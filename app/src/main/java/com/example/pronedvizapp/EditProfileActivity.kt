package com.example.pronedvizapp

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.pronedvizapp.authentication.AuthenticationActivity
import com.example.pronedvizapp.bisness.CurrencyTextWatcher
import com.example.pronedvizapp.databinding.ActivityEditProfileBinding
import com.example.pronedvizapp.databinding.EditProfileGenderDialogBinding
import com.example.pronedvizapp.databinding.EditProfileNameDialogBinding
import com.example.pronedvizapp.databinding.EditProfilePhoneDialogBinding
import com.example.pronedvizapp.requests.ServerApiUsers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityEditProfileBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showUserDataInFields()

        binding.goBackPanel.setOnClickListener {
            this.finish()
        }

        binding.signOutAccountConstraintLayout.setOnClickListener {
            val intent = Intent(this, AuthenticationActivity::class.java)
            intent.putExtra("IS_OLD_ENTER", false)
            startActivity(intent)
            this.finish()
        }

        binding.editNameConstraintLayout.setOnClickListener {
            val bindingDialog = EditProfileNameDialogBinding.inflate(LayoutInflater.from(this))

            val dialog = Dialog(this)
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(bindingDialog.root)
            dialog.show()

            bindingDialog.cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            bindingDialog.saveButton.setOnClickListener {
                val name = bindingDialog.editText.text.toString()
                if (name.length > 50) {
                    Toast.makeText(this, "Некорректное имя!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                MainActivity.currentUser!!.name = name
                EditProfileActivity.syncUserProfile(this)
                dialog.dismiss()
                showUserDataInFields()
            }
        }

        binding.editBirthdayConstraintLayout.setOnClickListener {
//            val datePicker = MaterialDatePicker.Builder.datePicker()
//                .setTitleText("Дата рождения:")
//                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
//                .build()
//            datePicker.show(supportFragmentManager, "DATE_PICKER")
//
//            datePicker.addOnPositiveButtonClickListener { selectedDate ->
//                val selectedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(selectedDate), ZoneOffset.UTC)
//                val currentDateTime = LocalDateTime.now()
//                val sixteenYearsAgo = currentDateTime.minusYears(16)
//
//                if (selectedDateTime.isBefore(sixteenYearsAgo)) {
//                    MainActivity.currentUser!!.birthday = selectedDate / 1000
//                    EditProfileActivity.syncUserPrifile(this)
//                    showUserDataInFields()
//                } else {
//                    Toast.makeText(this, "Выбранная дата была менее чем 16 лет назад. Выберите другую дату.", Toast.LENGTH_SHORT).show()
//                }
//            }

            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                val currentDate = LocalDate.now()
                val sixteenYearsAgo = currentDate.minusYears(16)

                if (selectedDate.isBefore(sixteenYearsAgo)) {
                    MainActivity.currentUser!!.birthday = selectedDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                    syncUserProfile(this)
                    showUserDataInFields()
                } else {
                    Toast.makeText(this, "Выбранная дата была менее чем 16 лет назад. Выберите другую дату.", Toast.LENGTH_SHORT).show()
                }
            }, year, month, day)
            datePickerDialog.setTitle("Дата рождения:")
            datePickerDialog.show()
        }

        binding.editPhoneConstraintLayout.setOnClickListener {
            val bindingDialog = EditProfilePhoneDialogBinding.inflate(LayoutInflater.from(this))

            val dialog = Dialog(this)
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(bindingDialog.root)
            dialog.show()

            bindingDialog.editText.addTextChangedListener(CurrencyTextWatcher())

            bindingDialog.cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            bindingDialog.saveButton.setOnClickListener {
                val phone = bindingDialog.editText.text.toString()
//                val regex = Regex("""^8\(\d{3}\)\d{3}-\d{2}-\d{2}$""")
//                if (!regex.matches(phone)) {
//                    Toast.makeText(this, "Некорректный формат номера!", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }

                MainActivity.currentUser!!.phone = phone
                dialog.dismiss()
                EditProfileActivity.syncUserProfile(this)
                showUserDataInFields()
            }
        }

        binding.editGenderConstraintLayout.setOnClickListener {
            val bindingDialog = EditProfileGenderDialogBinding.inflate(LayoutInflater.from(this))

            val dialog = Dialog(this)
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(bindingDialog.root)
            dialog.show()
            if (MainActivity.currentUser!!.gender == "Мужской") {
                bindingDialog.male.isChecked = true
                bindingDialog.female.isChecked = false
                bindingDialog.nothing.isChecked = false
            } else if (MainActivity.currentUser!!.gender == "Женский") {
                bindingDialog.female.isChecked = true
                bindingDialog.male.isChecked = false
                bindingDialog.nothing.isChecked = false
            }
            else {
                bindingDialog.nothing.isChecked = true
                bindingDialog.male.isChecked = false
                bindingDialog.female.isChecked = false
            }

            bindingDialog.cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            bindingDialog.radioGroup.setOnCheckedChangeListener { radioGroup, checkId ->
                when(checkId) {
                    bindingDialog.male.id -> {
                        MainActivity.currentUser!!.gender = bindingDialog.male.text.toString()
                    }
                    bindingDialog.female.id -> {
                        MainActivity.currentUser!!.gender = bindingDialog.female.text.toString()
                    }
                    bindingDialog.nothing.id -> {
                        MainActivity.currentUser!!.gender = bindingDialog.nothing.text.toString()
                    }
                }
            }

            bindingDialog.saveButton.setOnClickListener {
                EditProfileActivity.syncUserProfile(this)
                showUserDataInFields()
                dialog.dismiss()
            }
        }
    }

    private fun showUserDataInFields() {
        binding.nameToEditTextView.setText(MainActivity.currentUser!!.name)
        binding.userNameTextView.setText(MainActivity.currentUser!!.name)
        binding.phoneToEditTextView.setText(MainActivity.currentUser!!.phone)
        binding.genderToEditTextView.setText(MainActivity.currentUser!!.gender)
        binding.birthdayToEditTextView.setText(MainActivity.currentUser!!.birthday?.let {
            LocalDateTime.ofEpochSecond(
                it, 0, ZoneOffset.UTC).toLocalDate().toString()
        })

        // TODO: Picasso
//        Picasso.get()
//            .load(MainActivity.currentUser!!.photo + "потом убрать")
//            .error(R.drawable.default_avatar)
//            .resize(110, 110)
//            .centerInside()
//            .into(binding.avatarImageView)
    }

    companion object {

        private var isProfileSynchronized: Boolean = true

        public fun syncUserProfile(context: Context) {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            val resp = usersApi.editUserProfile(MainActivity.currentUser!!, MainActivity.currentToken!!)
            resp.enqueue(object : Callback<Boolean?> {
                override fun onResponse(call: Call<Boolean?>, response: Response<Boolean?>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                        isProfileSynchronized = true
                        return
                    }
                    Toast.makeText(context, "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                    isProfileSynchronized = false
                }

                override fun onFailure(call: Call<Boolean?>, t: Throwable) {
                    Toast.makeText(context, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                    isProfileSynchronized = false
                }
            })
        }

    }
}