package com.example.pronedvizapp

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pronedvizapp.authentication.AuthenticationActivity
import com.example.pronedvizapp.bisness.CurrencyTextWatcher
import com.example.pronedvizapp.databinding.ActivityEditProfileBinding
import com.example.pronedvizapp.databinding.EditProfileGenderDialogBinding
import com.example.pronedvizapp.databinding.EditProfileNameDialogBinding
import com.example.pronedvizapp.databinding.EditProfilePhoneDialogBinding
import com.example.pronedvizapp.main.ProgressDialogModal
import com.example.pronedvizapp.requests.RequestsRepository.bindUserImageFileAsync
import com.example.pronedvizapp.requests.RequestsRepository.editAvatarImageFileAsync
import com.example.pronedvizapp.requests.RequestsRepository.synchroniseUserProfileAsync
import com.example.pronedvizapp.requests.models.UserTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.ConnectException
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
                val lastFieldValue = MainStatic.currentUser.name
                val name = bindingDialog.editText.text.toString()
                if (name.length > 50) {
                    Toast.makeText(this@EditProfileActivity, "Некорректное имя!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                MainStatic.currentUser.name = name
                lifecycleScope.launch {
                    synchroniseUserProfileAsync(this@EditProfileActivity).also {
                        it.onSuccess {
                            Toast.makeText(this@EditProfileActivity, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                        }
                        it.onFailure {
                            MainStatic.currentUser.name = lastFieldValue
                            Toast.makeText(this@EditProfileActivity, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.dismiss()
                    showUserDataInFields()
                }
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
            val lastFieldValue = MainStatic.currentUser.birthday

            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                val currentDate = LocalDate.now()
                val sixteenYearsAgo = currentDate.minusYears(16)

                if (selectedDate.isBefore(sixteenYearsAgo)) {
                    MainStatic.currentUser.birthday = selectedDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                    lifecycleScope.launch {
                        synchroniseUserProfileAsync(this@EditProfileActivity).also {
                            it.onSuccess {
                                Toast.makeText(this@EditProfileActivity, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                            }
                            it.onFailure {
                                MainStatic.currentUser.birthday = lastFieldValue
                                Toast.makeText(this@EditProfileActivity, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showUserDataInFields()
                    }
                } else {
                    Toast.makeText(this, "Выбранная дата была менее чем 16 лет назад. Выберите другую дату.", Toast.LENGTH_SHORT).show()
                }
            }, year, month, day)
            datePickerDialog.setTitle("Дата рождения:")
            datePickerDialog.show()
        }

        binding.editPhoneConstraintLayout.setOnClickListener {
            val lastFieldValue = MainStatic.currentUser.phone

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
                MainStatic.currentUser.phone = phone
                lifecycleScope.launch {
                    synchroniseUserProfileAsync(this@EditProfileActivity).also {
                        it.onSuccess {
                            Toast.makeText(this@EditProfileActivity, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                        }
                        it.onFailure {
                            MainStatic.currentUser.phone = lastFieldValue
                            Toast.makeText(this@EditProfileActivity, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.dismiss()
                    showUserDataInFields()
                }
            }
        }

        binding.editRielterTypeConstraintLayout.setOnClickListener {
            val lastFieldValue = MainStatic.currentUser.type

            val bindingDialog = EditProfileGenderDialogBinding.inflate(LayoutInflater.from(this))

            bindingDialog.male.text = "Коммерческий"
            bindingDialog.female.text = "Частный"
            bindingDialog.nothing.visibility = View.GONE
            bindingDialog.textView4.text = "Выбрать профиль риелтора"

            val dialog = Dialog(this)
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(bindingDialog.root)
            dialog.show()
            if (MainStatic.currentUser!!.type == UserTypes.COMMERCIAL.description) {
                bindingDialog.male.isChecked = true
                bindingDialog.female.isChecked = false
            } else if (MainStatic.currentUser!!.type == UserTypes.PRIVATE.description) {
                bindingDialog.female.isChecked = true
                bindingDialog.male.isChecked = false
            } else {
                bindingDialog.nothing.isChecked = false
                bindingDialog.male.isChecked = false
            }

            bindingDialog.cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            bindingDialog.radioGroup.setOnCheckedChangeListener { radioGroup, checkId ->
                when(checkId) {
                    bindingDialog.male.id -> {
                        MainStatic.currentUser!!.type = UserTypes.COMMERCIAL.description
                    }
                    bindingDialog.female.id -> {
                        MainStatic.currentUser!!.type = UserTypes.PRIVATE.description
                    }
                }
            }

            bindingDialog.saveButton.setOnClickListener {
                lifecycleScope.launch {
                    synchroniseUserProfileAsync(this@EditProfileActivity).also {
                        it.onSuccess {
                            Toast.makeText(this@EditProfileActivity, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                        }
                        it.onFailure {
                            MainStatic.currentUser.type = lastFieldValue
                            Toast.makeText(this@EditProfileActivity, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.dismiss()
                    showUserDataInFields()
                }
            }
        }

        binding.editGenderConstraintLayout.setOnClickListener {
            val lastFieldValue = MainStatic.currentUser.gender

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
                        MainStatic.currentUser.gender = bindingDialog.male.text.toString()
                    }
                    bindingDialog.female.id -> {
                        MainStatic.currentUser.gender = bindingDialog.female.text.toString()
                    }
                    bindingDialog.nothing.id -> {
                        MainStatic.currentUser.gender = bindingDialog.nothing.text.toString()
                    }
                }
            }

            bindingDialog.saveButton.setOnClickListener {
                lifecycleScope.launch {
                    synchroniseUserProfileAsync(this@EditProfileActivity).also {
                        it.onSuccess {
                            Toast.makeText(this@EditProfileActivity, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                        }
                        it.onFailure {
                            MainStatic.currentUser.gender = lastFieldValue
                            Toast.makeText(this@EditProfileActivity, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.dismiss()
                    showUserDataInFields()
                }
            }
        }

        val showEditEmailImage = {
            if (MainStatic.currentUser.email == "") {
                binding.editEmailImageView.visibility = View.VISIBLE
            } else {
                binding.editEmailImageView.visibility = View.GONE
            }
        }
        showEditEmailImage()
        binding.emailConstraintLayout.setOnClickListener {
            if (MainStatic.currentUser!!.email == "") {
                val bindingDialog = EditProfileNameDialogBinding.inflate(LayoutInflater.from(this))

                val dialog = Dialog(this)
                dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
                dialog.setContentView(bindingDialog.root)
                dialog.show()

                bindingDialog.textView4.text = "Ввести почту"
                bindingDialog.editText.hint = "example@email.com"

                bindingDialog.cancelButton.setOnClickListener {
                    dialog.dismiss()
                }

                bindingDialog.saveButton.setOnClickListener {
                    val email = bindingDialog.editText.text.toString()
                    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                    if(!email.matches(emailRegex)) {
                        Toast.makeText(this, "Не корректная эл. почта!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    MainStatic.currentUser.email = email
                    lifecycleScope.launch {
                        synchroniseUserProfileAsync(this@EditProfileActivity).also {
                            it.onSuccess {
                                Toast.makeText(this@EditProfileActivity, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                            }
                            it.onFailure {
                                Toast.makeText(this@EditProfileActivity, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showEditEmailImage()
                        dialog.dismiss()
                        showUserDataInFields()
                    }
                }

            }
        }
    }

    private fun showUserDataInFields() {
        binding.nameToEditTextView.text = MainStatic.currentUser!!.name
        binding.userNameTextView.text = MainStatic.currentUser!!.name
        binding.phoneToEditTextView.text = MainStatic.currentUser!!.phone
        binding.genderToEditTextView.text = MainStatic.currentUser!!.gender
        binding.birthdayToEditTextView.text = MainStatic.currentUser!!.birthday?.let {
            LocalDateTime.ofEpochSecond(
                it, 0, ZoneOffset.UTC).toLocalDate().toString()
        }
        binding.emailTextView.text = if (MainStatic.currentUser!!.email == "") "Ваша почта" else MainStatic.currentUser!!.email
        binding.rielterTypeToEditTextView.text = MainStatic.currentUser!!.type

        lifecycleScope.launch {
            bindUserImageFileAsync(applicationContext, binding.avatarImageView, MainStatic.currentUser.image)
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
        val progressDialog = ProgressDialogModal(this)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            progressDialog.show()
            lifecycleScope.launch {
                val file = extractFileFormUri(applicationContext, data.data!!)
                file?.let {
                    val result = editAvatarImageFileAsync(
                        this@EditProfileActivity.applicationContext,
                        it,
                        MainStatic.currentToken
                    )
                    result.onSuccess { img ->
                        Toast.makeText(applicationContext, "Аватар обновлен (перезагрузите приложение)", Toast.LENGTH_SHORT).show()
                        bindUserImageFileAsync(applicationContext, binding.avatarImageView, MainStatic.currentUser.image)
                        progressDialog.dismiss()
                        MainStatic.currentUser.image = img
                    }
                    result.onFailure {
                        Toast.makeText(applicationContext, if (it is ConnectException) "Нет подключения к интернету" else "Ошибка загрузки картинки", Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    private fun extractFileFormUri(context: Context, uri: Uri): File? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return inputStream?.let { input ->
            val filename = getFileNameFromUri(context, uri)
            val file = File(context.filesDir, "images/${MainStatic.currentUser!!.id}-$filename")
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytes -> bytesRead = bytes } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.close()
            input.close()
            return@let file
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }
        return fileName
    }

    companion object {

        var isProfileSynchronized: Boolean = true
    }
}