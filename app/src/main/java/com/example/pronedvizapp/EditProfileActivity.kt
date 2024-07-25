package com.example.pronedvizapp

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.authentication.AuthenticationActivity
import com.example.pronedvizapp.bisness.CurrencyTextWatcher
import com.example.pronedvizapp.databinding.ActivityEditProfileBinding
import com.example.pronedvizapp.databinding.EditProfileGenderDialogBinding
import com.example.pronedvizapp.databinding.EditProfileNameDialogBinding
import com.example.pronedvizapp.databinding.EditProfilePhoneDialogBinding
import com.example.pronedvizapp.main.ProfileFragment.Companion.bindUserImageAsync
import com.example.pronedvizapp.requests.ServerApiUsers
import com.example.pronedvizapp.requests.models.Image
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityEditProfileBinding

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
                MainStatic.currentUser!!.name = name
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
//                    MainStatic.currentUser!!.birthday = selectedDate / 1000
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
                    MainStatic.currentUser!!.birthday = selectedDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
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

                MainStatic.currentUser!!.phone = phone
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
            if (MainStatic.currentUser!!.gender == "Мужской") {
                bindingDialog.male.isChecked = true
                bindingDialog.female.isChecked = false
                bindingDialog.nothing.isChecked = false
            } else if (MainStatic.currentUser!!.gender == "Женский") {
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
                        MainStatic.currentUser!!.gender = bindingDialog.male.text.toString()
                    }
                    bindingDialog.female.id -> {
                        MainStatic.currentUser!!.gender = bindingDialog.female.text.toString()
                    }
                    bindingDialog.nothing.id -> {
                        MainStatic.currentUser!!.gender = bindingDialog.nothing.text.toString()
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
        binding.nameToEditTextView.setText(MainStatic.currentUser!!.name)
        binding.userNameTextView.setText(MainStatic.currentUser!!.name)
        binding.phoneToEditTextView.setText(MainStatic.currentUser!!.phone)
        binding.genderToEditTextView.setText(MainStatic.currentUser!!.gender)
        binding.birthdayToEditTextView.setText(MainStatic.currentUser!!.birthday?.let {
            LocalDateTime.ofEpochSecond(
                it, 0, ZoneOffset.UTC).toLocalDate().toString()
        })

        lifecycleScope.launch {
            bindUserImageAsync(applicationContext, binding.avatarImageView)
        }

        binding.avatarImageView.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Сменить фото профиля")
                .setMessage("Загрузите новое фото профиля с вашего устройства")
                .setPositiveButton("Ок") { _,_ ->
                    val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
                }
                .setNegativeButton("Отмена") { dialog,_ -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    private val PICK_IMAGE_REQUEST = 48

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {

            //binding.progressBarContainerConstraintLayout.visibility = View.VISIBLE

            lifecycleScope.launch {
                val base64Bitmap = extractBitmapFromUri(data.data)
                base64Bitmap?.let {
                    val result = editAvatarImageAsync(
                        applicationContext,
                        Image(-1,  data.data.toString(), it),
                        MainStatic.currentToken!!)
                    result.onSuccess {
                        Toast.makeText(applicationContext, "Аватар обновлен", Toast.LENGTH_SHORT).show()
                        //binding.progressBarContainerConstraintLayout.visibility = View.GONE
                        bindUserImageAsync(applicationContext, binding.avatarImageView)

                    }
                    result.onFailure {
                        //binding.progressBarContainerConstraintLayout.visibility = View.GONE
                        Toast.makeText(applicationContext, "Ошибка загрузки картинки", Toast.LENGTH_SHORT).show()
                    }
                }
                //binding.progressBarContainerConstraintLayout.visibility = View.GONE
            }
        }
    }

    private fun extractBitmapFromUri(selectedImageUri: Uri?): String? {
        if (selectedImageUri != null) {
            val inputStream: InputStream? = contentResolver.openInputStream(selectedImageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            binding.avatarImageView.setImageBitmap(bitmap)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Bitmap = Base64.encodeToString(byteArray, Base64.DEFAULT)
            return base64Bitmap
        }
        return null
    }

    companion object {

        private var isProfileSynchronized: Boolean = true

        public fun syncUserProfile(context: Context) { // TODO: переписать на асинхронный
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            val resp = usersApi.editUserProfile(MainStatic.currentUser!!, MainStatic.currentToken!!)
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

        public suspend fun editAvatarImageAsync(
            context: Context,
            image: Image,
            token: String
        ): Result<Int?> = coroutineScope {
            val retrofit = Retrofit.Builder()
                .baseUrl(context.getString(R.string.server_ip_address))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val usersApi = retrofit.create(ServerApiUsers::class.java)

            return@coroutineScope try {
                val response = usersApi.setImageToUser(image, token).await()
                if (response != null) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("Ошибка получения данных"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}