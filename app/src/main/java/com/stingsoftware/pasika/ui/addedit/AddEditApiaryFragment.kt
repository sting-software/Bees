package com.stingsoftware.pasika.ui.addedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.data.ApiaryType
import com.stingsoftware.pasika.databinding.FragmentAddEditApiaryBinding
import com.stingsoftware.pasika.util.Resource
import com.stingsoftware.pasika.viewmodel.AddEditApiaryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddEditApiaryFragment : Fragment(R.layout.fragment_add_edit_apiary) {

    private val args: AddEditApiaryFragmentArgs by navArgs()
    private val addEditApiaryViewModel: AddEditApiaryViewModel by viewModels()

    private var _binding: FragmentAddEditApiaryBinding? = null
    private val binding get() = _binding!!

    private var selectedDateMillis: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditApiaryBinding.bind(view)

        val apiaryTypes = ApiaryType.entries.map {
            it.name.lowercase(Locale.getDefault())
                .replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }
        }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, apiaryTypes)
        binding.autoCompleteTextViewApiaryType.setAdapter(adapter)

        val apiaryId = args.apiaryId
        val isNewApiary = apiaryId == -1L

        setupInitialState(isNewApiary)

        if (!isNewApiary) {
            activity?.title = getString(R.string.title_edit_apiary)
            addEditApiaryViewModel.getApiary(apiaryId).observe(viewLifecycleOwner) { apiary ->
                apiary?.let {
                    binding.editTextApiaryName.setText(it.name)
                    binding.editTextApiaryLocation.setText(it.location)
                    val typeName = it.type.name.lowercase(Locale.getDefault())
                        .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
                    binding.autoCompleteTextViewApiaryType.setText(typeName, false)
                    selectedDateMillis = it.lastInspectionDate
                    updateDateEditText(it.lastInspectionDate)
                    binding.editTextNotes.setText(it.notes)
                }
            }
        } else {
            activity?.title = getString(R.string.title_add_apiary)
        }

        binding.checkboxAutoNumberApiaryHives.setOnCheckedChangeListener { _, isChecked ->
            binding.textInputLayoutStartingHiveNumberApiary.visibility =
                if (isChecked) View.VISIBLE else View.GONE
            binding.textInputLayoutEndingHiveNumberApiary.visibility =
                if (isChecked) View.VISIBLE else View.GONE
            binding.textInputLayoutNumberOfHives.visibility =
                if (isChecked) View.GONE else View.VISIBLE
            binding.editTextNumberOfHives.isEnabled = !isChecked
            updateCalculatedHiveCount()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculatedHiveCount()
            }
        }
        binding.editTextStartingHiveNumberApiary.addTextChangedListener(textWatcher)
        binding.editTextEndingHiveNumberApiary.addTextChangedListener(textWatcher)

        binding.editTextLastInspectionDate.setOnClickListener { showDatePickerDialog() }
        binding.textInputLayoutLastInspectionDate.setEndIconOnClickListener { showDatePickerDialog() }

        addEditApiaryViewModel.saveStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is Resource.Loading -> { /* Show loading indicator */
                }

                is Resource.Success -> {
                    val message = if (isNewApiary) getString(R.string.message_apiary_and_hives_added) else getString(
                        R.string.message_apiary_updated
                    )
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    if (!isNewApiary) {
                        findNavController().popBackStack()
                    }
                }

                is Resource.Error -> {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_with_message, status.message), Toast.LENGTH_LONG)
                        .show()
                }

                else -> {}
            }
        }

        addEditApiaryViewModel.navigateToDetail.observe(viewLifecycleOwner) { newApiaryId ->
            newApiaryId?.let {
                if (isAdded && findNavController().currentDestination?.id == R.id.addEditApiaryFragment) {
                    val action =
                        AddEditApiaryFragmentDirections.actionAddEditApiaryFragmentToApiaryDetailFragment(
                            it,
                            binding.editTextApiaryName.text.toString()
                        )
                    findNavController().navigate(action)
                    addEditApiaryViewModel.navigationCompleted()
                }
            }
        }

        binding.buttonSaveApiary.setOnClickListener { saveApiary(apiaryId, isNewApiary) }
        binding.buttonCancel.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupInitialState(isNewApiary: Boolean) {
        binding.cardHiveCreation.visibility = if (isNewApiary) View.VISIBLE else View.GONE
        if (isNewApiary) {
            binding.checkboxAutoNumberApiaryHives.visibility = View.VISIBLE
            binding.textInputLayoutStartingHiveNumberApiary.visibility = View.GONE
            binding.textInputLayoutEndingHiveNumberApiary.visibility = View.GONE
            binding.textInputLayoutNumberOfHives.visibility = View.VISIBLE
            binding.editTextNumberOfHives.isEnabled = true
            binding.editTextNumberOfHives.setText("1")
        }
    }

    private fun updateCalculatedHiveCount() {
        if (binding.checkboxAutoNumberApiaryHives.isChecked) {
            val startNum = binding.editTextStartingHiveNumberApiary.text.toString().toIntOrNull()
            val endNum = binding.editTextEndingHiveNumberApiary.text.toString().toIntOrNull()

            if (startNum != null && endNum != null) {
                if (endNum >= startNum) {
                    val count = endNum - startNum + 1
                    binding.editTextNumberOfHives.setText(count.toString())
                    binding.textInputLayoutEndingHiveNumberApiary.error = null
                } else {
                    binding.textInputLayoutEndingHiveNumberApiary.error =
                        getString(R.string.error_invalid_number_range)
                    binding.editTextNumberOfHives.setText("")
                }
            } else {
                binding.textInputLayoutEndingHiveNumberApiary.error = null
                binding.editTextNumberOfHives.setText("")
            }
        }
    }

    private fun saveApiary(apiaryId: Long, isNewApiary: Boolean) {
        val name = binding.editTextApiaryName.text.toString().trim()
        val location = binding.editTextApiaryLocation.text.toString().trim()
        val apiaryTypeString =
            binding.autoCompleteTextViewApiaryType.text.toString().uppercase(Locale.getDefault())
        val notes = binding.editTextNotes.text.toString().trim().ifEmpty { null }

        val apiaryType = try {
            enumValueOf<ApiaryType>(apiaryTypeString)
        } catch (_: Exception) {
            ApiaryType.STATIONARY
        }

        val autoNumberHives = binding.checkboxAutoNumberApiaryHives.isChecked
        var startingHiveNumber: Int? = null
        var endingHiveNumber: Int? = null
        var numberOfHives = 0

        if (isNewApiary) {
            if (autoNumberHives) {
                startingHiveNumber =
                    binding.editTextStartingHiveNumberApiary.text.toString().toIntOrNull()
                endingHiveNumber =
                    binding.editTextEndingHiveNumberApiary.text.toString().toIntOrNull()

                if (startingHiveNumber == null || endingHiveNumber == null || endingHiveNumber < startingHiveNumber) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_invalid_number_range),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                numberOfHives = endingHiveNumber - startingHiveNumber + 1
            } else {
                numberOfHives = binding.editTextNumberOfHives.text.toString().toIntOrNull() ?: 0
            }
        }

        val apiary = Apiary(
            id = if (isNewApiary) 0L else apiaryId,
            name = name,
            location = location,
            numberOfHives = numberOfHives,
            type = apiaryType,
            lastInspectionDate = selectedDateMillis,
            notes = notes
        )

        addEditApiaryViewModel.saveOrUpdateApiary(
            apiary = apiary,
            isNewApiary = isNewApiary,
            autoNumberHives = autoNumberHives,
            startingHiveNumber = startingHiveNumber,
            endingHiveNumber = endingHiveNumber
        )
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        selectedDateMillis?.let { calendar.timeInMillis = it }
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newCalendar = Calendar.getInstance().apply { set(year, month, day) }
                selectedDateMillis = newCalendar.timeInMillis
                updateDateEditText(selectedDateMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateEditText(dateMillis: Long?) {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.editTextLastInspectionDate.setText(
            if (dateMillis != null) formatter.format(
                Date(
                    dateMillis
                )
            ) else ""
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
