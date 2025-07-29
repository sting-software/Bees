package com.stingsoftware.pasika.ui.bulkedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.databinding.FragmentBulkEditHiveBinding
import com.stingsoftware.pasika.viewmodel.BulkEditHiveViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BulkEditHiveFragment : Fragment() {

    private val bulkEditHiveViewModel: BulkEditHiveViewModel by viewModels()
    private var _binding: FragmentBulkEditHiveBinding? = null
    private val binding get() = _binding!!

    private var selectedIsolationFromDateMillis: Long? = null
    private var selectedIsolationToDateMillis: Long? = null

    private data class BulkEditFormState(
        val material: String,
        val hiveType: String,
        val frameType: String,
        val breed: String,
        val role: String,
        val queenTagColor: String,
        val queenNumber: String,
        val queenYear: String,
        val queenLine: String,
        val isolationFromDate: Long?,
        val isolationToDate: Long?,
        val notes: String,
        val autoNumber: Boolean,
        val startingHiveNumber: String
    )

    private var initialState: BulkEditFormState? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBulkEditHiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_bulk_edit_hives)

        setupMenu()
        setupBackButtonHandler()
        setupSpinners()
        setupDatePickers()
        setupListeners()

        bulkEditHiveViewModel.selectedHives.observe(viewLifecycleOwner) { hives ->
            if (hives.isNullOrEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_no_hives_selected_for_editing), Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                return@observe
            }
            binding.textViewSelectedHivesCount.text = getString(R.string.bulk_edit_hives_count, hives.size)
            prefillCommonValues(hives)
            initialState = captureCurrentState()
        }
    }

    private fun setupMenu() {
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_save, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save_hive -> {
                        applyBulkEdit()
                        true
                    }
                    android.R.id.home -> {
                        handleAutoSaveOnExit()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupBackButtonHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleAutoSaveOnExit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun captureCurrentState(): BulkEditFormState {
        return BulkEditFormState(
            material = binding.autoCompleteTextViewMaterial.text.toString(),
            hiveType = binding.autoCompleteTextViewHiveType.text.toString(),
            frameType = binding.autoCompleteTextViewFrameType.text.toString(),
            breed = binding.autoCompleteTextViewBreed.text.toString(),
            role = binding.autoCompleteTextViewRole.text.toString(),
            queenTagColor = binding.autoCompleteTextViewQueenTagColor.text.toString(),
            queenNumber = binding.editTextQueenNumber.text.toString(),
            queenYear = binding.editTextQueenYear.text.toString(),
            queenLine = binding.editTextQueenLine.text.toString(),
            isolationFromDate = selectedIsolationFromDateMillis,
            isolationToDate = selectedIsolationToDateMillis,
            notes = binding.editTextHiveNotes.text.toString(),
            autoNumber = binding.checkboxAutoNumberHivesBulk.isChecked,
            startingHiveNumber = binding.editTextStartingHiveNumberBulk.text.toString()
        )
    }

    private fun hasChanges(): Boolean {
        return initialState != null && initialState != captureCurrentState()
    }

    private fun handleAutoSaveOnExit() {
        if (hasChanges()) {
            applyBulkEdit()
        } else {
            findNavController().popBackStack()
        }
    }

    private fun setupListeners() {
        binding.checkboxAutoNumberHivesBulk.setOnCheckedChangeListener { _, isChecked ->
            binding.textInputLayoutStartingHiveNumberBulk.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.textViewAutoNumberingWarning.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.editTextStartingHiveNumberBulk.text?.clear()
            }
        }
        binding.buttonSaveBulkEdit.setOnClickListener { applyBulkEdit() }
        binding.buttonCancelBulkEdit.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupSpinners() {
        val materialChoices = resources.getStringArray(R.array.material_choices)
        val materialAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, materialChoices)
        binding.autoCompleteTextViewMaterial.setAdapter(materialAdapter)

        val hiveTypes = resources.getStringArray(R.array.hive_types)
        val hiveTypeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, hiveTypes)
        binding.autoCompleteTextViewHiveType.setAdapter(hiveTypeAdapter)

        val frameTypes = resources.getStringArray(R.array.frame_types)
        val frameTypeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, frameTypes)
        binding.autoCompleteTextViewFrameType.setAdapter(frameTypeAdapter)

        val breedChoices = resources.getStringArray(R.array.breed_choices)
        val breedAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, breedChoices)
        binding.autoCompleteTextViewBreed.setAdapter(breedAdapter)

        val queenTagColors = resources.getStringArray(R.array.queen_tag_colors)
        val queenTagColorAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, queenTagColors)
        binding.autoCompleteTextViewQueenTagColor.setAdapter(queenTagColorAdapter)

        val roleLabels = HiveRole.entries.map { it.getLabel(requireContext()) }
        val roleAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, roleLabels)
        binding.autoCompleteTextViewRole.setAdapter(roleAdapter)
    }

    private fun setupDatePickers() {
        val isolationFromListener = { showDatePickerDialog(::selectedIsolationFromDateMillis, binding.editTextIsolationFromDate) }
        binding.editTextIsolationFromDate.setOnClickListener { isolationFromListener() }
        binding.textInputLayoutIsolationFromDate.setEndIconOnClickListener { isolationFromListener() }

        val isolationToListener = { showDatePickerDialog(::selectedIsolationToDateMillis, binding.editTextIsolationToDate) }
        binding.editTextIsolationToDate.setOnClickListener { isolationToListener() }
        binding.textInputLayoutIsolationToDate.setEndIconOnClickListener { isolationToListener() }
    }

    private fun prefillCommonValues(hives: List<Hive>) {
        if (hives.isEmpty()) return
        val firstHive = hives[0]

        fun <T> allShareSameValue(property: (Hive) -> T): Boolean {
            val firstValue = property(firstHive)
            return hives.all { property(it) == firstValue }
        }

        if (allShareSameValue { it.material }) binding.autoCompleteTextViewMaterial.setText(firstHive.material, false)
        if (allShareSameValue { it.hiveType }) binding.autoCompleteTextViewHiveType.setText(firstHive.hiveType, false)
        if (allShareSameValue { it.frameType }) binding.autoCompleteTextViewFrameType.setText(firstHive.frameType, false)
        if (allShareSameValue { it.breed }) binding.autoCompleteTextViewBreed.setText(firstHive.breed, false)
        if (allShareSameValue { it.queenTagColor }) binding.autoCompleteTextViewQueenTagColor.setText(firstHive.queenTagColor, false)
        if (allShareSameValue { it.queenNumber }) binding.editTextQueenNumber.setText(firstHive.queenNumber)
        if (allShareSameValue { it.queenYear }) binding.editTextQueenYear.setText(firstHive.queenYear)
        if (allShareSameValue { it.queenLine }) binding.editTextQueenLine.setText(firstHive.queenLine)
        if (allShareSameValue { it.notes }) binding.editTextHiveNotes.setText(firstHive.notes)

        if (allShareSameValue { it.role }) {
            binding.autoCompleteTextViewRole.setText(firstHive.role.getLabel(requireContext()), false)
        }

        if (allShareSameValue { it.isolationFromDate }) {
            selectedIsolationFromDateMillis = firstHive.isolationFromDate
            updateDateEditText(selectedIsolationFromDateMillis, binding.editTextIsolationFromDate)
        }
        if (allShareSameValue { it.isolationToDate }) {
            selectedIsolationToDateMillis = firstHive.isolationToDate
            updateDateEditText(selectedIsolationToDateMillis, binding.editTextIsolationToDate)
        }
    }


    private fun applyBulkEdit() {
        val material = binding.autoCompleteTextViewMaterial.text.toString().trim().ifEmpty { null }
        val hiveType = binding.autoCompleteTextViewHiveType.text.toString().trim().ifEmpty { null }
        val frameType = binding.autoCompleteTextViewFrameType.text.toString().trim().ifEmpty { null }
        val breed = binding.autoCompleteTextViewBreed.text.toString().trim().ifEmpty { null }
        val selectedRoleLabel = binding.autoCompleteTextViewRole.text.toString()
        val role = HiveRole.entries.find { it.getLabel(requireContext()) == selectedRoleLabel }
        val queenTagColor = binding.autoCompleteTextViewQueenTagColor.text.toString().trim().ifEmpty { null }
        val queenNumber = binding.editTextQueenNumber.text.toString().trim().ifEmpty { null }
        val queenYear = binding.editTextQueenYear.text.toString().trim().ifEmpty { null }
        val queenLine = binding.editTextQueenLine.text.toString().trim().ifEmpty { null }
        val notes = binding.editTextHiveNotes.text.toString().trim().ifEmpty { null }
        val autoNumber = binding.checkboxAutoNumberHivesBulk.isChecked
        val startingHiveNumber = binding.editTextStartingHiveNumberBulk.text.toString().trim().toIntOrNull()

        if (autoNumber && (startingHiveNumber == null || startingHiveNumber <= 0)) {
            binding.textInputLayoutStartingHiveNumberBulk.error = getString(R.string.error_must_be_positive_number, getString(R.string.label_starting_hive_number))
            return
        } else {
            binding.textInputLayoutStartingHiveNumberBulk.error = null
        }

        bulkEditHiveViewModel.updateSelectedHives(
            material = material,
            hiveType = hiveType,
            frameType = frameType,
            breed = breed,
            role = role,
            queenTagColor = queenTagColor,
            queenNumber = queenNumber,
            queenYear = queenYear,
            queenLine = queenLine,
            isolationFromDate = selectedIsolationFromDateMillis,
            isolationToDate = selectedIsolationToDateMillis,
            notes = notes,
            autoNumber = autoNumber,
            startingHiveNumber = startingHiveNumber
        )

        Toast.makeText(requireContext(), getString(R.string.message_hives_updated), Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private fun showDatePickerDialog(dateMillisProperty: kotlin.reflect.KMutableProperty0<Long?>, editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        dateMillisProperty.get()?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newCalendar = Calendar.getInstance().apply { set(year, month, day) }
                dateMillisProperty.set(newCalendar.timeInMillis)
                updateDateEditText(newCalendar.timeInMillis, editText)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateEditText(dateMillis: Long?, editText: TextInputEditText) {
        editText.setText(
            if (dateMillis != null) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateMillis))
            } else {
                ""
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        _binding = null
    }
}
