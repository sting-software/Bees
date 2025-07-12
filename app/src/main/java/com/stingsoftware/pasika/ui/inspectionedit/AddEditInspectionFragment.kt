package com.stingsoftware.pasika.ui.inspectionedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditInspectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hiveId = args.hiveId
        val inspectionId = args.inspectionId
        val isNewInspection = inspectionId == -1L

        activity?.title = if (isNewInspection) "Add New Inspection" else "Edit Inspection"

        setupDatePickers()
        setupConditionalViews()
        setupSaveCancelButtons(hiveId, inspectionId, isNewInspection)

        if (!isNewInspection) {
            addEditInspectionViewModel.getInspection(inspectionId).observe(viewLifecycleOwner) { inspection ->
                inspection?.let {
                    populateInspectionData(it)
                } ?: run {
                    Toast.makeText(requireContext(), "Inspection not found!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        } else {
            selectedInspectionDateMillis = System.currentTimeMillis()
            updateDateEditText(selectedInspectionDateMillis, binding.editTextInspectionDate)
        }

        addEditInspectionViewModel.saveCompleted.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess != null) {
                if (isSuccess) {
                    Toast.makeText(requireContext(), "Inspection saved successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Failed to save inspection. Please try again.", Toast.LENGTH_LONG).show()
                }
                addEditInspectionViewModel.resetSaveCompleted()
            }
        }
    }

    private fun setupDatePickers() {
        binding.editTextInspectionDate.setOnClickListener {
            showDatePickerDialog(::selectedInspectionDateMillis, binding.editTextInspectionDate)
        }
        binding.textInputLayoutInspectionDate.setEndIconOnClickListener {
            showDatePickerDialog(::selectedInspectionDateMillis, binding.editTextInspectionDate)
        }
    }

    private fun showDatePickerDialog(dateMillisProperty: kotlin.reflect.KMutableProperty0<Long?>, editText: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
        dateMillisProperty.get()?.let {
            calendar.timeInMillis = it
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            dateMillisProperty.set(newCalendar.timeInMillis)
            updateDateEditText(dateMillisProperty.get(), editText)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateDateEditText(dateMillis: Long?, editText: com.google.android.material.textfield.TextInputEditText) {
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

    private fun setupSaveCancelButtons(hiveId: Long, inspectionId: Long, isNewInspection: Boolean) {
        binding.buttonSaveInspection.setOnClickListener {
            saveInspection(hiveId, inspectionId, isNewInspection)
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

        // The following two lines were removed as the views don't exist in the layout
        // binding.editTextHoneyStoresEstimate.setText(inspection.honeyStoresEstimateFrames?.toString())
        // binding.editTextPollenStoresEstimate.setText(inspection.pollenStoresEstimateFrames?.toString())

        binding.editTextPestsDiseasesObserved.setText(inspection.pestsDiseasesObserved)
        binding.editTextTreatmentApplied.setText(inspection.treatmentApplied)

        when (inspection.temperamentRating) {
            1 -> binding.radioTemperament1.isChecked = true
            2 -> binding.radioTemperament2.isChecked = true
            3 -> binding.radioTemperament3.isChecked = true
            4 -> binding.radioTemperament4.isChecked = true
        }

        binding.editTextManagementActionsTaken.setText(inspection.managementActionsTaken)
        binding.editTextInspectionNotes.setText(inspection.notes)
    }

    private fun saveInspection(hiveId: Long, inspectionId: Long, isNewInspection: Boolean) {
        val inspectionDate = selectedInspectionDateMillis ?: System.currentTimeMillis()
        val queenCellsPresent = binding.checkboxQueenCellsPresent.isChecked
        val queenCellsCount = binding.editTextQueenCellsCount.text.toString().trim().toIntOrNull()
        val framesEggsCount = binding.editTextFramesEggsCount.text.toString().trim().toIntOrNull()
        val framesOpenBroodCount = binding.editTextFramesOpenBroodCount.text.toString().trim().toIntOrNull()
        val framesCappedBroodCount = binding.editTextFramesCappedBroodCount.text.toString().trim().toIntOrNull()
        val framesHoneyCount = binding.editTextFramesHoneyCount.text.toString().trim().toIntOrNull()
        val framesPollenCount = binding.editTextFramesPollenCount.text.toString().trim().toIntOrNull()

        // The following two lines were removed as the views don't exist in the layout
        // val honeyStoresEstimateFrames = binding.editTextHoneyStoresEstimate.text.toString().trim().toIntOrNull()
        // val pollenStoresEstimateFrames = binding.editTextPollenStoresEstimate.text.toString().trim().toIntOrNull()

        val pestsDiseasesObserved = binding.editTextPestsDiseasesObserved.text.toString().trim().ifEmpty { null }
        val treatmentApplied = binding.editTextTreatmentApplied.text.toString().trim().ifEmpty { null }
        val temperamentRating = when (binding.radioGroupTemperament.checkedRadioButtonId) {
            R.id.radio_temperament_1 -> 1
            R.id.radio_temperament_2 -> 2
            R.id.radio_temperament_3 -> 3
            R.id.radio_temperament_4 -> 4
            else -> null
        }
        val managementActionsTaken = binding.editTextManagementActionsTaken.text.toString().trim().ifEmpty { null }
        val notes = binding.editTextInspectionNotes.text.toString().trim().ifEmpty { null }

        val inspection = Inspection(
            id = if (isNewInspection) 0L else inspectionId,
            hiveId = hiveId,
            inspectionDate = inspectionDate,
            queenCellsPresent = queenCellsPresent,
            queenCellsCount = queenCellsCount,
            framesEggsCount = framesEggsCount,
            framesOpenBroodCount = framesOpenBroodCount,
            framesCappedBroodCount = framesCappedBroodCount,
            framesHoneyCount = framesHoneyCount,
            framesPollenCount = framesPollenCount,
            // Pass null for the fields that don't have corresponding UI inputs
            honeyStoresEstimateFrames = null,
            pollenStoresEstimateFrames = null,
            pestsDiseasesObserved = pestsDiseasesObserved,
            treatmentApplied = treatmentApplied,
            temperamentRating = temperamentRating,
            managementActionsTaken = managementActionsTaken,
            notes = notes
        )

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