package com.example.pronedvizapp.main

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.example.pronedvizapp.MainActivity
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.WebViewActivity
import com.example.pronedvizapp.databinding.FragmentActionResultBinding
import com.example.pronedvizapp.requests.RequestsRepository.deleteTaskAsync
import com.example.pronedvizapp.requests.RequestsRepository.editUserStatisticsAsync
import com.example.pronedvizapp.requests.models.ContractTypes
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Task
import com.example.pronedvizapp.requests.models.WorkTasksTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset

fun showResultDialog(task: Task, context: Context, owner: MainActivity, onUpdateAdapter: (() -> Unit)? = null) {
    val dialogBinding = FragmentActionResultBinding.inflate(LayoutInflater.from(context))

    dialogBinding.aboutActivityDescTextView1.text = "Как вы оцениваете результат своей работы в области \"${task.workType}\" (${task.desc})?"

    if (task.workType == WorkTasksTypes.FLYERS.description || task.workType == WorkTasksTypes.CALLS.description) {
        dialogBinding.aboutActivityDescTextView2.text = "Введите количественный показатель:"
        dialogBinding.countEditText.visibility = View.VISIBLE
//                dialogBinding.countNumberPicker.visibility = View.VISIBLE
    }
    if (task.workType == WorkTasksTypes.ANALYTICS.description || task.workType == WorkTasksTypes.SEARCH.description) {
        dialogBinding.aboutActivityDescTextView2.text = "Дайте свою оценку:"
        dialogBinding.postpondActivityButton.visibility = View.GONE
//                dialogBinding.countNumberPicker.visibility = View.VISIBLE
    }
    if (task.workType == WorkTasksTypes.DEPOSIT.description || task.workType == WorkTasksTypes.MEET.description || task.workType == WorkTasksTypes.SHOW.description) {
        dialogBinding.aboutActivityDescTextView2.text = "Как прошло общение с клиентом:"
        dialogBinding.contractStatusRadioGroup.visibility = View.VISIBLE
    }
    if (task.workType == WorkTasksTypes.SHOW.description) {
        dialogBinding.aboutActivityDescTextView2.text = "Как прошло общение с клиентом:"
        dialogBinding.contractStatusRadioGroup.visibility = View.GONE
    }
    if (task.workType == WorkTasksTypes.DEAL.description || task.workType == WorkTasksTypes.DEAL_SALE.description || task.workType == WorkTasksTypes.DEAL_RENT.description) {
        dialogBinding.aboutActivityDescTextView2.text = "Каков результат сделки:"
//                dialogBinding.resultImageView.visibility = View.VISIBLE
    }
    if (task.workType == WorkTasksTypes.OTHER.description) {
        dialogBinding.aboutActivityDescTextView2.text = "Каков результат работы:"
//                dialogBinding.resultImageView.visibility = View.VISIBLE
    }

    var contract: ContractTypes = ContractTypes.NO

    dialogBinding.contractStatusRadioGroup.setOnCheckedChangeListener { _, checkedId ->
        when (checkedId) {
            R.id.noContractRadioButton -> contract = ContractTypes.NO
            R.id.regularContractRadioButton -> contract = ContractTypes.REGULAR
            R.id.exclusiveContractRadioButton -> contract = ContractTypes.EXCLUSIVE
        }
    }

    val dialog = Dialog(context)
    dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
    dialog.setContentView(dialogBinding.root)

    var addValue = 1

    dialogBinding.goodActivityButton.setOnClickListener {
        if (task.workType == WorkTasksTypes.FLYERS.description || task.workType == WorkTasksTypes.CALLS.description) {
            addValue = try {
                dialogBinding.countEditText.text.toString().toInt()  // dialogBinding.countNumberPicker.value
            } catch (e: Exception) {
                Toast.makeText(context, "Введите корректное число!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
        if (task.workType == WorkTasksTypes.DEPOSIT.description || task.workType == WorkTasksTypes.MEET.description || task.workType == WorkTasksTypes.SHOW.description) {
            addValue = 1
        }
        CoroutineScope(Dispatchers.IO).launch {
            editUserStatisticsAsync(context, task.workType, addValue, MainStatic.currentToken)
            when(contract) {
                ContractTypes.NO -> { }
                ContractTypes.REGULAR -> {
                    editUserStatisticsAsync(context, WorkTasksTypes.REGULAR_CONTRACT.description, addValue, MainStatic.currentToken)
                }
                ContractTypes.EXCLUSIVE -> {
                    editUserStatisticsAsync(context, WorkTasksTypes.EXCLUSIVE_CONTRACT.description, addValue, MainStatic.currentToken)
                }
            }
            deleteTaskAsync(context, task.id, MainStatic.currentToken)
            withContext(Dispatchers.Main) {
                onUpdateAdapter?.invoke()
                dialog.dismiss()
            }
        }
    }

    dialogBinding.badActivityButton.setOnClickListener {
        dialog.dismiss()
        CoroutineScope(Dispatchers.Main).launch {
            deleteTaskAsync(context, task.id, MainStatic.currentToken)
            onUpdateAdapter?.invoke()

            MaterialAlertDialogBuilder(context)
                .setMessage("Рекомендуется почитать обучающие материалы для того, чтобы в следующий раз достигнуть успеха.")
                .setPositiveButton("Да, спасибо") { materialAlertDialog, _ ->
                    materialAlertDialog.dismiss()
                    val intent = Intent(context, WebViewActivity::class.java)
                    intent.putExtra(WebViewActivity.SOURCE, "baza.html")
                    context.startActivity(intent)
                }
                .create()
                .show()
        }
    }

    dialogBinding.postpondActivityButton.setOnClickListener {
        dialog.dismiss()
        CoroutineScope(Dispatchers.Main).launch {
            deleteTaskAsync(context, task.id, MainStatic.currentToken)
            onUpdateAdapter?.invoke()

            MaterialAlertDialogBuilder(context)
                .setMessage("Рекомендуется сделать заметку, чтобы не забыть о том, что встреча перенесена.")
                .setPositiveButton("Да") { materialAlertDialog, _ ->
                    materialAlertDialog.dismiss()
                    owner.showFragment("CreateEditNoteFragment")
                    val fragment = CreateEditNoteFragment(
                        Note("", "${task.workType} с ...", "По предмету ...", LocalDateTime.now().toEpochSecond(
                            ZoneOffset.UTC), MainStatic.currentUser!!.id, 0), true)
                    val transaction = owner.supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.mainContentFrame, fragment)
                    transaction.commit()
                }
                .setNegativeButton("Нет, спасибо") { materialAlertDialog, _ ->
                    materialAlertDialog.dismiss()
                }
                .create()
                .show()
        }
    }

    dialog.show()
}
