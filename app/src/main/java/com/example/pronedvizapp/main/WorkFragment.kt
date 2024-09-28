package com.example.pronedvizapp.main

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.pronedvizapp.IFragmentTag
import com.example.pronedvizapp.MainStatic
import com.example.pronedvizapp.R
import com.example.pronedvizapp.databinding.FragmentAboutActivityBinding
import com.example.pronedvizapp.databinding.FragmentWorkBinding
import com.example.pronedvizapp.bisness.Analytics
import com.example.pronedvizapp.bisness.CustomeWork
import com.example.pronedvizapp.bisness.OtherWork
import com.example.pronedvizapp.bisness.Work
import com.example.pronedvizapp.databinding.FragmentAboutCustomeWorkBinding
import com.example.pronedvizapp.notifications.NotificationBroadcast
import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.WorkTasksTypes
import com.google.android.material.button.MaterialButton
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Calendar

class WorkFragment(override var fragmentNavigationTag: String = "WorkFragment") : Fragment(), IFragmentTag {

    lateinit var binding: FragmentWorkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentWorkBinding.inflate(inflater, container, false)

        binding.analyticsConstraintLayout.setOnClickListener{
            var analytics: Analytics = Analytics(this.requireContext())
            showCommitAlertDialog(analytics, R.string.analytics_desc)
        }

        binding.searchConstraintLayout.setOnClickListener{
            var search: OtherWork = OtherWork(this.requireContext(), WorkTasksTypes.SEARCH)
            showCommitAlertDialog(search, R.string.search_desc)
        }

        binding.callsConstraintLayout.setOnClickListener{
            var calls: OtherWork = OtherWork(this.requireContext(), WorkTasksTypes.CALLS)
            showCommitAlertDialog(calls, R.string.calls_desc)
        }

        binding.flyersConstraintLayout.setOnClickListener{
            var flyers: OtherWork = OtherWork(this.requireContext(), WorkTasksTypes.FLYERS)
            showCommitAlertDialog(flyers, R.string.flyer_desc)
        }

        binding.showConstraintLayout.setOnClickListener{
            var show: OtherWork = OtherWork(this.requireContext(), WorkTasksTypes.SHOW)
            showCommitAlertDialog(show, R.string.show_desc)
        }

        binding.meetConstraintLayout.setOnClickListener{
            var meet: OtherWork = OtherWork(this.requireContext(), WorkTasksTypes.MEET)
            showCommitAlertDialog(meet, R.string.meet_desc)
        }

        binding.dealConstraintLayout.setOnClickListener{
            var deal: OtherWork = OtherWork(this.requireContext(), WorkTasksTypes.DEAL)
            showCommitAlertDialog(deal, R.string.deal_desc)
        }

        binding.depositConstraintLayout.setOnClickListener{
            var deposit: OtherWork = OtherWork(this.requireContext(), WorkTasksTypes.DEPOSIT)
            showCommitAlertDialog(deposit, R.string.deposit_desc)
        }

        binding.customeWorkConstraintLayout.setOnClickListener {
            var actualWork: CustomeWork = CustomeWork(this.requireContext(), WorkTasksTypes.OTHER)

            val binding = FragmentAboutCustomeWorkBinding.inflate(LayoutInflater.from(this.requireContext()))
            var selectedDuration: Duration = Duration.ofHours((LocalTime.now().hour + 1).toLong()).plusMinutes(LocalTime.now().minute.toLong())

            binding.setHoursNumberPicker.minValue = 0
            binding.setHoursNumberPicker.maxValue = 12
            binding.setHoursNumberPicker.wrapSelectorWheel = true
            binding.setMinutesNumberPicker.minValue = 0
            binding.setMinutesNumberPicker.maxValue = 59
            binding.setMinutesNumberPicker.wrapSelectorWheel = true

            binding.setHoursNumberPicker.value = 1
            binding.setMinutesNumberPicker.value = 0

            binding.aboutActivityNameTextView.text = actualWork.workType.description

            val dialog = Dialog(this.requireContext())
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(binding.root)
            dialog.show()

            binding.closeImageButton.setOnClickListener {
                dialog.dismiss()
            }

            binding.setHoursNumberPicker.setOnValueChangedListener { _, _, _ ->
                selectedDuration = Duration.ofHours((binding.setHoursNumberPicker.value).toLong()).plusMinutes(binding.setMinutesNumberPicker.value.toLong())
            }

            binding.setMinutesNumberPicker.setOnValueChangedListener { _, _, _ ->
                selectedDuration = Duration.ofHours((binding.setHoursNumberPicker.value).toLong()).plusMinutes(binding.setMinutesNumberPicker.value.toLong())
            }

            binding.startActivityButton.setOnClickListener {
                if (selectedDuration.seconds >= 86400) {
                    selectedDuration = Duration.ofSeconds(selectedDuration.seconds - 86400)
                }
                (actualWork as CustomeWork).desc = binding.aboutActivityDescEditText.text.toString()
                (actualWork as CustomeWork).start(selectedDuration, !binding.dontNotifyCheckBox.isChecked)
                dialog.dismiss()
//            activity?.supportFragmentManager?.beginTransaction()?.remove(this@WorkFragment.requireParentFragment())?.commit()
//            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottomMenu)
//            bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
            }
        }

        binding.unableToWorkTextView.setOnClickListener {
            val binding = FragmentAboutCustomeWorkBinding.inflate(LayoutInflater.from(this.requireContext()))
            var selectedDuration: Duration = Duration.ofHours((LocalTime.now().hour + 1).toLong()).plusMinutes(LocalTime.now().minute.toLong())

            binding.setHoursNumberPicker.minValue = 0
            binding.setHoursNumberPicker.maxValue = 12
            binding.setHoursNumberPicker.wrapSelectorWheel = true
            binding.setMinutesNumberPicker.minValue = 0
            binding.setMinutesNumberPicker.maxValue = 59
            binding.setMinutesNumberPicker.wrapSelectorWheel = true

            binding.setHoursNumberPicker.value = 1
            binding.setMinutesNumberPicker.value = 0

            binding.aboutActivityNameTextView.text = "Отдых"
            binding.aboutActivityDescEditText.hint = "Укажите почему вы пока не можете работать и выберите время для отдыха"
            binding.startActivityButton.text = "Отдыхать"
            binding.dontNotifyCheckBox.visibility = View.INVISIBLE

            val dialog = Dialog(this.requireContext())
            dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
            dialog.setContentView(binding.root)
            dialog.show()

            binding.closeImageButton.setOnClickListener {
                dialog.dismiss()
            }

            binding.setHoursNumberPicker.setOnValueChangedListener { _, _, _ ->
                selectedDuration = Duration.ofHours((binding.setHoursNumberPicker.value).toLong()).plusMinutes(binding.setMinutesNumberPicker.value.toLong())
            }

            binding.setMinutesNumberPicker.setOnValueChangedListener { _, _, _ ->
                selectedDuration = Duration.ofHours((binding.setHoursNumberPicker.value).toLong()).plusMinutes(binding.setMinutesNumberPicker.value.toLong())
            }

            binding.startActivityButton.setOnClickListener {
                if (selectedDuration.seconds >= 86400) {
                    selectedDuration = Duration.ofSeconds(selectedDuration.seconds - 86400)
                }

                val workStartTime = LocalDateTime.now()
                val calendar = Calendar.getInstance()
                calendar.set(workStartTime.year, workStartTime.monthValue-1, workStartTime.dayOfMonth, workStartTime.hour, workStartTime.minute, workStartTime.second)

                val alarmManager = this.requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(context, NotificationBroadcast::class.java).let {
                        intent ->
                    intent.putExtra("TITLE", "Уведомление")
                    intent.putExtra("CONTENT", "Пришло время поработать?")
                    PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
                if (selectedDuration.seconds >= 86400) {
                    selectedDuration = Duration.ofSeconds(selectedDuration.seconds - 86400)
                }
                val time = selectedDuration.toMillis() + calendar.timeInMillis
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarmIntent)

                dialog.dismiss()
//            activity?.supportFragmentManager?.beginTransaction()?.remove(this@WorkFragment.requireParentFragment())?.commit()
//            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottomMenu)
//            bottomNavigationView?.selectedItemId = R.id.bottomMenuItemNotes
            }
        }

        return binding.root
    }

    private fun showCommitAlertDialog(actualWork: Work, desc: Int?) {
        val dialogBinding = FragmentAboutActivityBinding.inflate(LayoutInflater.from(this.requireContext()))
        var selectedDuration: Duration = Duration.ofHours((LocalTime.now().hour + 1).toLong()).plusMinutes(LocalTime.now().minute.toLong())

        if (actualWork.workType != WorkTasksTypes.DEAL) {
            dialogBinding.dealTypesMaterialButtonToggleGroup.visibility = View.GONE
        }
        dialogBinding.dealTypesMaterialButtonToggleGroup.check(R.id.dealSaleButton)
        updateButtonStyles(dialogBinding.dealSaleButton, dialogBinding.dealRentButton)

        dialogBinding.dealTypesMaterialButtonToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    dialogBinding.dealRentButton.id -> {
                        actualWork.workType = WorkTasksTypes.DEAL_RENT
                        updateButtonStyles(dialogBinding.dealRentButton, dialogBinding.dealSaleButton)
                    }
                    dialogBinding.dealSaleButton.id -> {
                        actualWork.workType = WorkTasksTypes.DEAL_SALE
                        updateButtonStyles(dialogBinding.dealSaleButton, dialogBinding.dealRentButton)
                    }
                    else -> { }
                }
            }
        }

        dialogBinding.setHoursNumberPicker.minValue = 0
        dialogBinding.setHoursNumberPicker.maxValue = 12
        dialogBinding.setHoursNumberPicker.wrapSelectorWheel = true
        dialogBinding.setMinutesNumberPicker.minValue = 0
        dialogBinding.setMinutesNumberPicker.maxValue = 59
        dialogBinding.setMinutesNumberPicker.wrapSelectorWheel = true

        dialogBinding.setHoursNumberPicker.value = 1
        dialogBinding.setMinutesNumberPicker.value = 0

        dialogBinding.aboutActivityDescTextView.text = getText(desc!!)
        dialogBinding.aboutActivityNameTextView.text = actualWork.workType.description

        if (actualWork.workType == WorkTasksTypes.DEAL) {
            actualWork.workType = WorkTasksTypes.DEAL_SALE
        }

        val dialog = Dialog(this.requireContext())
        dialog.window?.setBackgroundDrawableResource(R.color.transparent0)
        dialog.setContentView(dialogBinding.root)
        dialog.show()

        dialogBinding.closeImageButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.setHoursNumberPicker.setOnValueChangedListener { _, _, _ ->
            selectedDuration = Duration.ofHours((dialogBinding.setHoursNumberPicker.value).toLong()).plusMinutes(dialogBinding.setMinutesNumberPicker.value.toLong())
        }

        dialogBinding.setMinutesNumberPicker.setOnValueChangedListener { _, _, _ ->
            selectedDuration = Duration.ofHours((dialogBinding.setHoursNumberPicker.value).toLong()).plusMinutes(dialogBinding.setMinutesNumberPicker.value.toLong())
        }

        dialogBinding.startActivityButton.setOnClickListener {
            if (selectedDuration.seconds >= 86400) {
                selectedDuration = Duration.ofSeconds(selectedDuration.seconds - 86400)
            }
            actualWork.start(selectedDuration, !dialogBinding.dontNotifyCheckBox.isChecked)
            dialog.dismiss()

            val fragment = NotesFragment()
            val transaction = this.requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.mainContentFrame, fragment)
            transaction.commit()
        }
    }

    private fun updateButtonStyles(selectedButton: MaterialButton, unselectedButton2: MaterialButton) {
        selectedButton.setTextColor(resources.getColor(android.R.color.black))
        selectedButton.backgroundTintList = resources.getColorStateList(android.R.color.white)

        unselectedButton2.setTextColor(resources.getColor(android.R.color.white))
        unselectedButton2.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.transparent0))
        unselectedButton2.strokeColor = resources.getColorStateList(android.R.color.white)
        unselectedButton2.strokeWidth = 2
    }

    companion object {
        const val WORK_TYPE_DEAL = "Сделка"
    }
}