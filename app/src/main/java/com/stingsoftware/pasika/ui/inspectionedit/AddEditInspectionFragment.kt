package com.stingsoftware.pasika.ui.inspectionedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.databinding.FragmentAddEditInspectionBinding
import com.stingsoftware.pasika.viewmodel.AddEditInspectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddEditInspectionFragment : Fragment() {

    private val args: AddEditInspectionFragmentArgs by navArgs()
    private val addEditInspectionViewModel: AddEditInspectionViewModel by viewModels()

    private var _binding: FragmentAddEditInspectionBinding? = null
    private val binding get() = _binding!!

    private var selectedInspectionDateMillis: Long? = null
    private var originalInspection: Inspection? = null
    private var isNewInspection: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditInspectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isNewInspection = args.inspectionId == -1L
        activity?.title = if (isNewInspection) getString(R.string.title_add_inspection) else getString(R.string.title_edit_inspection)

        setupDatePickers()
        setupConditionalViews()
        setupSaveCancelButtons()
        setupBackButtonHandler() // Setup the back button auto-save logic

        if (!isNewInspection) {
            addEditInspectionViewModel.getInspection(args.inspectionId)
                .observe(viewLifecycleOwner) { inspection ->
                    inspection?.let {
                        originalInspection = it.copy() // Store a copy of the original state
                        populateInspectionData(it)
                    }
                }
        } else {
            // For a new inspection, set the default state
            selectedInspectionDateMillis = System.currentTimeMillis()
            updateDateEditText(selectedInspectionDateMillis, binding.editTextInspectionDate)
            originalInspection = createInspectionFromInput() // The initial state is an empty form
        }

        addEditInspectionViewModel.saveCompleted.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess == true) { // Only navigate back on successful save
                findNavController().popBackStack()
                addEditInspectionViewModel.resetSaveCompleted()
            } else if (isSuccess == false) {
                Toast.makeText(requireContext(), getString(R.string.error_save_inspection_failed), Toast.LENGTH_LONG).show()
                addEditInspectionViewModel.resetSaveCompleted()
            }
        }
    }

    private fun setupBackButtonHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasChanges()) {
                    saveInspection()
                } else {
                    // No changes, so just navigate back
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun hasChanges(): Boolean {
        val currentInspection = createInspectionFromInput()
        return originalInspection != currentInspection
    }

    private fun createInspectionFromInput(): Inspection {
        val inspectionDate = selectedInspectionDateMillis ?: System.currentTimeMillis()
        val queenCellsPresent = binding.checkboxQueenCellsPresent.isChecked
        val queenCellsCount = binding.editTextQueenCellsCount.text.toString().trim().toIntOrNull()
        val framesEggsCount = binding.editTextFramesEggsCount.text.toString().trim().toIntOrNull()
        val framesOpenBroodCount = binding.editTextFramesOpenBroodCount.text.toString().trim().toIntOrNull()
        val framesCappedBroodCount = binding.editTextFramesCappedBroodCount.text.toString().trim().toIntOrNull()
        val framesHoneyCount = binding.editTextFramesHoneyCount.text.toString().trim().toIntOrNull()
        val framesPollenCount = binding.editTextFramesPollenCount.text.toString().trim().toIntOrNull()
        val pestsDiseasesObserved = binding.editTextPestsDiseasesObserved.text.toString().trim().ifEmpty { null }
        val treatment = binding.editTextTreatmentApplied.text.toString().trim().ifEmpty { null }
        val defensivenessRating = when (binding.radioGroupTemperament.checkedRadioButtonId) {
            R.id.radio_temperament_1 -> 1
            R.id.radio_temperament_2 -> 2
            R.id.radio_temperament_3 -> 3
            R.id.radio_temperament_4 -> 4
            else -> null
        }
        val managementActionsTaken = binding.editTextManagementActionsTaken.text.toString().trim().ifEmpty { null }
        val notes = binding.editTextInspectionNotes.text.toString().trim().ifEmpty { null }

        return Inspection(
            id = if (isNewInspection) 0L else args.inspectionId,
            hiveId = args.hiveId,
            inspectionDate = inspectionDate,
            queenCellsPresent = queenCellsPresent,
            queenCellsCount = queenCellsCount,
            framesEggsCount = framesEggsCount,
            framesOpenBroodCount = framesOpenBroodCount,
            framesCappedBroodCount = framesCappedBroodCount,
            framesHoneyCount = framesHoneyCount,
            framesPollenCount = framesPollenCount,
            pestsDiseasesObserved = pestsDiseasesObserved,
            treatment = treatment,
            defensivenessRating = defensivenessRating,
            managementActionsTaken = managementActionsTaken,
            notes = notes
        )
    }

    private fun setupDatePickers() {
        binding.editTextInspectionDate.setOnClickListener {
            showDatePickerDialog(::selectedInspectionDateMillis, binding.editTextInspectionDate)
        }
        binding.textInputLayoutInspectionDate.setEndIconOnClickListener {
            showDatePickerDialog(::selectedInspectionDateMillis, binding.editTextInspectionDate)
        }
    }

    private fun showDatePickerDialog(
        dateMillisProperty: kotlin.reflect.KMutableProperty0<Long?>,
        editText: com.google.android.material.textfield.TextInputEditText
    ) {
        val calendar = Calendar.getInstance()
        dateMillisProperty.get()?.let { calendar.timeInMillis = it }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val newCalendar = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDayOfMonth) }
            dateMillisProperty.set(newCalendar.timeInMillis)
            updateDateEditText(newCalendar.timeInMillis, editText)
        }, year, month, day).show()
    }

    private fun updateDateEditText(
        dateMillis: Long?,
        editText: com.google.android.material.textfield.TextInputEditText
    ) {
        if (dateMillis != null) {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            editText.setText(formatter.format(Date(dateMillis)))
        } else {
            editText.setText("")
        }
    }

    private fun setupConditionalViews() {
        binding.checkboxQueenCellsPresent.setOnCheckedChangeListener { _, isChecked ->
            binding.textInputLayoutQueenCellsCount.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.editTextQueenCellsCount.setText("")
            }
        }
    }

    private fun setupSaveCancelButtons() {
        binding.buttonSaveInspection.setOnClickListener {
            saveInspection()
        }
        binding.buttonCancelInspection.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun populateInspectionData(inspection: Inspection) {
        selectedInspectionDateMillis = inspection.inspectionDate
        updateDateEditText(selectedInspectionDateMillis, binding.editTextInspectionDate)
        binding.checkboxQueenCellsPresent.isChecked = inspection.queenCellsPresent ?: false
        if (inspection.queenCellsPresent == true) {
            binding.textInputLayoutQueenCellsCount.visibility = View.VISIBLE
            binding.editTextQueenCellsCount.setText(inspection.queenCellsCount?.toString())
        } else {
            binding.textInputLayoutQueenCellsCount.visibility = View.GONE
        }
        binding.editTextFramesEggsCount.setText(inspection.framesEggsCount?.toString())
        binding.editTextFramesOpenBroodCount.setText(inspection.framesOpenBroodCount?.toString())
        binding.editTextFramesCappedBroodCount.setText(inspection.framesCappedBroodCount?.toString())
        binding.editTextFramesHoneyCount.setText(inspection.framesHoneyCount?.toString())
        binding.editTextFramesPollenCount.setText(inspection.framesPollenCount?.toString())
        binding.editTextPestsDiseasesObserved.setText(inspection.pestsDiseasesObserved)
        binding.editTextTreatmentApplied.setText(inspection.treatment)
        when (inspection.defensivenessRating) {
            1 -> binding.radioTemperament1.isChecked = true
            2 -> binding.radioTemperament2.isChecked = true
            3 -> binding.radioTemperament3.isChecked = true
            4 -> binding.radioTemperament4.isChecked = true
        }
        binding.editTextManagementActionsTaken.setText(inspection.managementActionsTaken)
        binding.editTextInspectionNotes.setText(inspection.notes)
    }

    private fun saveInspection() {
        val inspection = createInspectionFromInput()
        if (isNewInspection) {
            addEditInspectionViewModel.saveInspection(inspection)
        } else {
            addEditInspectionViewModel.updateInspection(inspection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
