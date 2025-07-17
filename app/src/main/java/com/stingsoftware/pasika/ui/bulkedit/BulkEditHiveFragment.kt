package com.stingsoftware.pasika.ui.bulkedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
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

    private lateinit var autoNumberingWarningTextView: TextView

    // Properties to hold all selected dates
    private var selectedLastInspectionDateMillis: Long? = null
    private var selectedIsolationFromDateMillis: Long? = null
    private var selectedIsolationToDateMillis: Long? = null

    private var numberOfSelectedHives: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBulkEditHiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoNumberingWarningTextView = view.findViewById(R.id.text_view_auto_numbering_warning)
        activity?.title = getString(R.string.title_bulk_edit_hives)

        setupSpinners()
        setupDatePickers()
        setupFramesTextWatchers()

        bulkEditHiveViewModel.selectedHives.observe(viewLifecycleOwner) { hives ->
            if (hives.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_no_hives_selected_for_editing),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
                return@observe
            }
            numberOfSelectedHives = hives.size
            binding.textViewSelectedHivesCount.text =
                getString(R.string.bulk_edit_hives_count, numberOfSelectedHives)
            prefillCommonValues(hives)
        }

        binding.checkboxAutoNumberHivesBulk.setOnCheckedChangeListener { _, isChecked ->
            binding.textInputLayoutStartingHiveNumberBulk.visibility =
                if (isChecked) View.VISIBLE else View.GONE
            autoNumberingWarningTextView.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculatedQuantity()
            }
        }
        binding.editTextStartingHiveNumberBulk.addTextChangedListener(textWatcher)


        binding.buttonSaveBulkEdit.setOnClickListener { applyBulkEdit() }
        binding.buttonCancelBulkEdit.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupSpinners() {
        val materialChoices = resources.getStringArray(R.array.material_choices)
        val materialAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, materialChoices)
        binding.autoCompleteTextViewMaterial.setAdapter(materialAdapter)

        val hiveTypes = resources.getStringArray(R.array.hive_types)
        val hiveTypeAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, hiveTypes)
        binding.autoCompleteTextViewHiveType.setAdapter(hiveTypeAdapter)

        val frameTypes = resources.getStringArray(R.array.frame_types)
        val frameTypeAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, frameTypes)
        binding.autoCompleteTextViewFrameType.setAdapter(frameTypeAdapter)

        val breedChoices = resources.getStringArray(R.array.breed_choices)
        val breedAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, breedChoices)
        binding.autoCompleteTextViewBreed.setAdapter(breedAdapter)

        val queenTagColors = resources.getStringArray(R.array.queen_tag_colors)
        val queenTagColorAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, queenTagColors)
        binding.autoCompleteTextViewQueenTagColor.setAdapter(queenTagColorAdapter)
    }

    private fun setupDatePickers() {
        binding.editTextHiveLastInspectionDate.setOnClickListener {
            showDatePickerDialog(
                ::selectedLastInspectionDateMillis,
                binding.editTextHiveLastInspectionDate
            )
        }
        binding.textInputLayoutHiveLastInspectionDate.setEndIconOnClickListener {
            showDatePickerDialog(
                ::selectedLastInspectionDateMillis,
                binding.editTextHiveLastInspectionDate
            )
        }
        // NOTE: XML for these fields needs to be added to fragment_bulk_edit_hive.xml
        // binding.editTextIsolationFromDate.setOnClickListener {
        //     showDatePickerDialog(::selectedIsolationFromDateMillis, binding.editTextIsolationFromDate)
        // }
        // binding.editTextIsolationToDate.setOnClickListener {
        //     showDatePickerDialog(::selectedIsolationToDateMillis, binding.editTextIsolationToDate)
        // }
    }

    private fun setupFramesTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateFramesTotal()
            }
        }
        binding.editTextFramesEggs.addTextChangedListener(textWatcher)
        binding.editTextFramesOpenBrood.addTextChangedListener(textWatcher)
        binding.editTextFramesCappedBrood.addTextChangedListener(textWatcher)
        binding.editTextFramesFeed.addTextChangedListener(textWatcher)
    }

    private fun updateFramesTotal() {
        val eggs = binding.editTextFramesEggs.text.toString().toIntOrNull() ?: 0
        val openBrood = binding.editTextFramesOpenBrood.text.toString().toIntOrNull() ?: 0
        val cappedBrood = binding.editTextFramesCappedBrood.text.toString().toIntOrNull() ?: 0
        val feed = binding.editTextFramesFeed.text.toString().toIntOrNull() ?: 0
        val total = eggs + openBrood + cappedBrood + feed
        binding.editTextFramesTotal.setText(total.toString())
    }

    private fun prefillCommonValues(hives: List<Hive>) {
        if (hives.isEmpty()) return
        val firstHive = hives[0]

        // Prefill all common values from the first hive if they are consistent across all selected hives
        if (hives.all { it.material == firstHive.material }) binding.autoCompleteTextViewMaterial.setText(
            firstHive.material,
            false
        )
        if (hives.all { it.hiveType == firstHive.hiveType }) binding.autoCompleteTextViewHiveType.setText(
            firstHive.hiveType,
            false
        )
        if (hives.all { it.frameType == firstHive.frameType }) binding.autoCompleteTextViewFrameType.setText(
            firstHive.frameType,
            false
        )
        if (hives.all { it.breed == firstHive.breed }) binding.autoCompleteTextViewBreed.setText(
            firstHive.breed,
            false
        )
        if (hives.all { it.framesTotal == firstHive.framesTotal }) binding.editTextFramesTotal.setText(
            firstHive.framesTotal?.toString()
        )
        if (hives.all { it.queenTagColor == firstHive.queenTagColor }) binding.autoCompleteTextViewQueenTagColor.setText(
            firstHive.queenTagColor,
            false
        )
        if (hives.all { it.queenNumber == firstHive.queenNumber }) binding.editTextQueenNumber.setText(
            firstHive.queenNumber
        )
        if (hives.all { it.queenYear == firstHive.queenYear }) binding.editTextQueenYear.setText(
            firstHive.queenYear
        )
        if (hives.all { it.queenLine == firstHive.queenLine }) binding.editTextQueenLine.setText(
            firstHive.queenLine
        )
        if (hives.all { it.treatment == firstHive.treatment }) binding.editTextTreatment.setText(
            firstHive.treatment
        )
        if (hives.all { it.notes == firstHive.notes }) binding.editTextHiveNotes.setText(firstHive.notes)

        if (hives.all { it.defensivenessRating == firstHive.defensivenessRating } && firstHive.defensivenessRating != null) {
            when (firstHive.defensivenessRating) {
                1 -> binding.radioDefensiveness1.isChecked = true
                2 -> binding.radioDefensiveness2.isChecked = true
                3 -> binding.radioDefensiveness3.isChecked = true
                4 -> binding.radioDefensiveness4.isChecked = true
            }
        }

        if (hives.all { it.lastInspectionDate == firstHive.lastInspectionDate }) {
            selectedLastInspectionDateMillis = firstHive.lastInspectionDate
            updateDateEditText(
                selectedLastInspectionDateMillis,
                binding.editTextHiveLastInspectionDate
            )
        }
    }

    // In BulkEditHiveFragment.kt

    private fun applyBulkEdit() {
        // Gather all values from UI fields
        val material = binding.autoCompleteTextViewMaterial.text.toString().trim().ifEmpty { null }
        val hiveType = binding.autoCompleteTextViewHiveType.text.toString().trim().ifEmpty { null }
        val frameType =
            binding.autoCompleteTextViewFrameType.text.toString().trim().ifEmpty { null }
        val breed = binding.autoCompleteTextViewBreed.text.toString().trim().ifEmpty { null }
        val queenTagColor =
            binding.autoCompleteTextViewQueenTagColor.text.toString().trim().ifEmpty { null }
        val queenNumber = binding.editTextQueenNumber.text.toString().trim().ifEmpty { null }
        val queenYear = binding.editTextQueenYear.text.toString().trim().ifEmpty { null }
        val queenLine = binding.editTextQueenLine.text.toString().trim().ifEmpty { null }
        val treatment = binding.editTextTreatment.text.toString().trim().ifEmpty { null }
        val notes = binding.editTextHiveNotes.text.toString().trim().ifEmpty { null }

        val framesTotal = binding.editTextFramesTotal.text.toString().trim().toIntOrNull()
        val framesEggs = binding.editTextFramesEggs.text.toString().trim().toIntOrNull()
        val framesOpenBrood = binding.editTextFramesOpenBrood.text.toString().trim().toIntOrNull()
        val framesCappedBrood =
            binding.editTextFramesCappedBrood.text.toString().trim().toIntOrNull()
        val framesFeed = binding.editTextFramesFeed.text.toString().trim().toIntOrNull()

        val defensivenessRating = when (binding.radioGroupDefensiveness.checkedRadioButtonId) {
            R.id.radio_defensiveness_1 -> 1
            R.id.radio_defensiveness_2 -> 2
            R.id.radio_defensiveness_3 -> 3
            R.id.radio_defensiveness_4 -> 4
            else -> null
        }

        val autoNumber = binding.checkboxAutoNumberHivesBulk.isChecked
        var startingHiveNumber: Int? = null
        if (autoNumber) {
            startingHiveNumber =
                binding.editTextStartingHiveNumberBulk.text.toString().trim().toIntOrNull()
            if (startingHiveNumber == null || startingHiveNumber <= 0) {
                binding.textInputLayoutStartingHiveNumberBulk.error =
                    getString(R.string.error_invalid_number_range)
                return
            } else {
                binding.textInputLayoutStartingHiveNumberBulk.error = null
            }
        }

        // --- EDITED: Added the missing endingHiveNumber = null parameter ---
        bulkEditHiveViewModel.updateSelectedHives(
            hiveType = hiveType,
            frameType = frameType,
            framesTotal = framesTotal,
            breed = breed,
            lastInspectionDate = selectedLastInspectionDateMillis,
            notes = notes,
            autoNumber = autoNumber,
            startingHiveNumber = startingHiveNumber,
//            endingHiveNumber = null,
            material = material,
            queenTagColor = queenTagColor,
            queenNumber = queenNumber,
            queenYear = queenYear,
            queenLine = queenLine,
            defensivenessRating = defensivenessRating,
            framesEggs = framesEggs,
            framesOpenBrood = framesOpenBrood,
            framesCappedBrood = framesCappedBrood,
            framesFeed = framesFeed,
            treatment = treatment,
            queenCells = null,
            isolationFromDate = null,
            isolationToDate = null,
            givenBuiltCombs = null,
            givenFoundation = null,
            givenBrood = null,
            givenBeesKg = null,
            givenHoneyKg = null,
            givenSugarKg = null
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.message_hives_updated),
            Toast.LENGTH_SHORT
        ).show()
        findNavController().popBackStack()
    }

    private fun showDatePickerDialog(
        dateMillisProperty: kotlin.reflect.KMutableProperty0<Long?>,
        editText: com.google.android.material.textfield.TextInputEditText
    ) {
        val calendar = Calendar.getInstance()
        dateMillisProperty.get()?.let { calendar.timeInMillis = it }
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newCalendar = Calendar.getInstance().apply { set(year, month, day) }
                dateMillisProperty.set(newCalendar.timeInMillis)
                updateDateEditText(newCalendar.timeInMillis, editText)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
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

    private fun updateCalculatedQuantity() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}