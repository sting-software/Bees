package com.stingsoftware.pasika.todo

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.databinding.FragmentAddEditTaskBinding
import com.stingsoftware.pasika.viewmodel.AddEditTaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {

    private val viewModel: AddEditTaskViewModel by viewModels()
    private val args: AddEditTaskFragmentArgs by navArgs()

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!

    private var selectedDateTime = Calendar.getInstance()
    private var currentTask: Task? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notification_permission_denied), Toast.LENGTH_LONG
                ).show()
                binding.switchReminder.isChecked = false
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditTaskBinding.bind(view)

        activity?.title = args.title

        viewModel.task.observe(viewLifecycleOwner) { task ->
            task?.let {
                currentTask = it
                binding.editTextTaskTitle.setText(it.title)
                binding.editTextTaskDescription.setText(it.description)
                it.dueDate?.let { dueDate ->
                    selectedDateTime.timeInMillis = dueDate
                    updateDateTimeEditText()
                }
                binding.switchReminder.isChecked = it.reminderEnabled
            }
        }

        binding.editTextDueDate.setOnClickListener { showDateTimePicker() }
        binding.textInputLayoutDueDate.setEndIconOnClickListener { showDateTimePicker() }

        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkNotificationPermission()
            }
        }

        binding.buttonSaveTask.setOnClickListener { onSave() }
        binding.buttonCancelTask.setOnClickListener { findNavController().popBackStack() }

        viewModel.saveStatus.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    private fun onSave() {
        val title = binding.editTextTaskTitle.text?.toString()?.trim()
        if (title.isNullOrBlank()) {
            val fieldTitle = getString(R.string.hint_task_title)
            Toast.makeText(
                requireContext(),
                getString(R.string.error_field_cannot_be_empty, fieldTitle),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val description = binding.editTextTaskDescription.text?.toString()?.trim()
        val dueDate =
            if (binding.editTextDueDate.text?.isNotBlank() == true) selectedDateTime.timeInMillis else null
        val reminderEnabled = binding.switchReminder.isChecked

        // Correctly handle both creating a new task and updating an existing one.
        // Using .copy() preserves the ID and graftingBatchId for existing tasks.
        val taskToSave = currentTask?.copy(
            title = title,
            description = description,
            dueDate = dueDate,
            reminderEnabled = reminderEnabled
        ) ?: Task(
            title = title,
            description = description,
            dueDate = dueDate,
            reminderEnabled = reminderEnabled
        )

        viewModel.onSaveClick(taskToSave)
    }

    private fun showDateTimePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        updateDateTimeEditText()
                    },
                    selectedDateTime.get(Calendar.HOUR_OF_DAY),
                    selectedDateTime.get(Calendar.MINUTE),
                    true
                ).show()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeEditText() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.editTextDueDate.setText(sdf.format(selectedDateTime.time))
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
