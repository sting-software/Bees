package com.stingsoftware.pasika.ui.hiveedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.databinding.FragmentAddEditHiveBinding
import com.stingsoftware.pasika.viewmodel.AddEditHiveViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddEditHiveFragment : Fragment() {

    private val args: AddEditHiveFragmentArgs by navArgs()

    // ViewModel is now injected by Hilt
    private val addEditHiveViewModel: AddEditHiveViewModel by viewModels()

    private var _binding: FragmentAddEditHiveBinding? = null
    private val binding get() = _binding!!

    private var selectedHiveLastInspectionDateMillis: Long? = null
    private var selectedIsolationFromDateMillis: Long? = null
    private var selectedIsolationToDateMillis: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditHiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiaryId = args.apiaryId
        val hiveId = args.hiveId
        val isNewHive = hiveId == -1L

        setupSpinners()
        setupConditionalViews(isNewHive)
        setupDatePickers()
        setupTextWatchers()
        setupFramesTextWatchers()
        setupSaveCancelButtons(apiaryId, hiveId, isNewHive)

        if (!isNewHive) {
            activity?.title = getString(R.string.title_edit_hive)
            addEditHiveViewModel.getHive(hiveId).observe(viewLifecycleOwner) { hive ->
                hive?.let {
                    populateHiveData(it)
                    updateFramesTotal()
                } ?: run {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_hive_not_found), Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        } else {
            activity?.title = getString(R.string.title_add_hive)
            updateFramesTotal()
        }

        addEditHiveViewModel.saveCompleted.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess != null) {
                if (isSuccess) {
                    if (isNewHive) {
                        val messageQuantity = if (binding.checkboxAddMultipleHives.isChecked) {
                            binding.editTextQuantity.text.toString().trim().toIntOrNull() ?: 0
                        } else {
                            1
                        }
                        Toast.makeText(requireContext(), getString(R.string.message_hives_added, messageQuantity), Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(),
                            getString(R.string.message_hives_updated), Toast.LENGTH_SHORT).show()
                        selectedHiveLastInspectionDateMillis?.let { newHiveDate ->
                            if (isAdded) {
                                // The call to the now-included function
                                showOverwriteApiaryDateDialog(apiaryId, newHiveDate)
                            } else {
                                findNavController().popBackStack()
                            }
                        } ?: run {
                            findNavController().popBackStack()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_save_hive_failed), Toast.LENGTH_LONG).show()
                }
                addEditHiveViewModel.resetSaveCompleted()
            }
        }
    }

    private fun setupSpinners() {
        val materialChoices = resources.getStringArray(R.array.material_choices)
        val materialAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, materialChoices)
        binding.autoCompleteTextViewMaterial.setAdapter(materialAdapter)
        binding.autoCompleteTextViewMaterial.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                toggleOtherFieldVisibility(s.toString(), binding.textInputLayoutMaterialOther)
            }
        })

        val hiveTypes = resources.getStringArray(R.array.hive_types)
        val hiveTypeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, hiveTypes)
        binding.autoCompleteTextViewHiveType.setAdapter(hiveTypeAdapter)
        binding.autoCompleteTextViewHiveType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                toggleOtherFieldVisibility(s.toString(), binding.textInputLayoutHiveTypeOther)
            }
        })

        val frameTypes = resources.getStringArray(R.array.frame_types)
        val frameTypeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, frameTypes)
        binding.autoCompleteTextViewFrameType.setAdapter(frameTypeAdapter)
        binding.autoCompleteTextViewFrameType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                toggleOtherFieldVisibility(s.toString(), binding.textInputLayoutFrameTypeOther)
            }
        })

        val breedChoices = resources.getStringArray(R.array.breed_choices)
        val breedAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, breedChoices)
        binding.autoCompleteTextViewBreed.setAdapter(breedAdapter)
        binding.autoCompleteTextViewBreed.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                toggleOtherFieldVisibility(s.toString(), binding.textInputLayoutBreedOther)
            }
        })

        val queenTagColors = resources.getStringArray(R.array.queen_tag_colors)
        val queenTagColorAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, queenTagColors)
        binding.autoCompleteTextViewQueenTagColor.setAdapter(queenTagColorAdapter)
        binding.autoCompleteTextViewQueenTagColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                toggleOtherFieldVisibility(s.toString(), binding.textInputLayoutQueenTagColorOther)
            }
        })
    }

    private fun toggleOtherFieldVisibility(selectedItem: String, otherInputField: com.google.android.material.textfield.TextInputLayout) {
        if (selectedItem.equals(getString(R.string.other), ignoreCase = true)) {
            otherInputField.visibility = View.VISIBLE
        } else {
            otherInputField.visibility = View.GONE
        }
    }

    private fun setupConditionalViews(isNewHive: Boolean) {
        if (isNewHive) {
            binding.checkboxAddMultipleHives.visibility = View.VISIBLE
            binding.textInputLayoutHiveNumber.visibility = View.VISIBLE
            binding.textInputLayoutQuantity.visibility = View.GONE
            binding.editTextQuantity.setText("")

            binding.checkboxAddMultipleHives.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    binding.checkboxAutomaticNumbering.visibility = View.VISIBLE
                    binding.textInputLayoutHiveNumber.visibility = View.GONE
                    binding.textInputLayoutQuantity.visibility = View.VISIBLE
                    binding.editTextQuantity.setText("1")
                    binding.layoutAutoNumberingRange.visibility = View.GONE
                } else {
                    binding.checkboxAutomaticNumbering.visibility = View.GONE
                    binding.layoutAutoNumberingRange.visibility = View.GONE
                    binding.textInputLayoutHiveNumber.visibility = View.VISIBLE
                    binding.textInputLayoutQuantity.visibility = View.GONE
                    binding.editTextHiveNumber.setText("")
                }
                updateCalculatedQuantity()
            }

            binding.checkboxAutomaticNumbering.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    binding.layoutAutoNumberingRange.visibility = View.VISIBLE
                    binding.textInputLayoutQuantity.visibility = View.VISIBLE
                    binding.editTextQuantity.setText("")
                    binding.editTextQuantity.isEnabled = false
                } else {
                    binding.layoutAutoNumberingRange.visibility = View.GONE
                    binding.textInputLayoutQuantity.visibility = View.VISIBLE
                    binding.editTextQuantity.setText("1")
                    binding.editTextQuantity.isEnabled = true
                }
                updateCalculatedQuantity()
            }
        } else {
            binding.checkboxAddMultipleHives.visibility = View.GONE
            binding.checkboxAutomaticNumbering.visibility = View.GONE
            binding.layoutAutoNumberingRange.visibility = View.GONE
            binding.textInputLayoutQuantity.visibility = View.GONE
            binding.textInputLayoutHiveNumber.visibility = View.VISIBLE
            binding.editTextHiveNumber.isEnabled = true
        }
    }

    private fun setupDatePickers() {
        binding.editTextHiveLastInspectionDate.setOnClickListener {
            showDatePickerDialog(::selectedHiveLastInspectionDateMillis, binding.editTextHiveLastInspectionDate)
        }
        binding.textInputLayoutHiveLastInspectionDate.setEndIconOnClickListener {
            showDatePickerDialog(::selectedHiveLastInspectionDateMillis, binding.editTextHiveLastInspectionDate)
        }
        binding.editTextIsolationFromDate.setOnClickListener {
            showDatePickerDialog(::selectedIsolationFromDateMillis, binding.editTextIsolationFromDate)
        }
        binding.textInputLayoutIsolationFromDate.setEndIconOnClickListener {
            showDatePickerDialog(::selectedIsolationFromDateMillis, binding.editTextIsolationFromDate)
        }
        binding.editTextIsolationToDate.setOnClickListener {
            showDatePickerDialog(::selectedIsolationToDateMillis, binding.editTextIsolationToDate)
        }
        binding.textInputLayoutIsolationToDate.setEndIconOnClickListener {
            showDatePickerDialog(::selectedIsolationToDateMillis, binding.editTextIsolationToDate)
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

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculatedQuantity()
                binding.textInputLayoutFromHiveNumber.error = null
                binding.textInputLayoutToHiveNumber.error = null
            }
        }
        binding.editTextFromHiveNumber.addTextChangedListener(textWatcher)
        binding.editTextToHiveNumber.addTextChangedListener(textWatcher)
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
        binding.textViewFramesTotalValue.text = total.toString()
    }

    private fun setupSaveCancelButtons(apiaryId: Long, hiveId: Long, isNewHive: Boolean) {
        binding.buttonSaveHive.setOnClickListener {
            saveHive(apiaryId, hiveId, isNewHive)
        }
        binding.buttonCancelHive.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun populateHiveData(hive: Hive) {
        binding.editTextHiveNumber.setText(hive.hiveNumber)
        binding.autoCompleteTextViewMaterial.setText(hive.material, false)
        if (hive.material.equals(getString(R.string.other), ignoreCase = true) && !hive.materialOther.isNullOrEmpty()) {
            binding.textInputLayoutMaterialOther.visibility = View.VISIBLE
            binding.editTextMaterialOther.setText(hive.materialOther)
        } else {
            binding.textInputLayoutMaterialOther.visibility = View.GONE
        }
        binding.autoCompleteTextViewHiveType.setText(hive.hiveType, false)
        if (hive.hiveType.equals(getString(R.string.other), ignoreCase = true) && !hive.hiveTypeOther.isNullOrEmpty()) {
            binding.textInputLayoutHiveTypeOther.visibility = View.VISIBLE
            binding.editTextHiveTypeOther.setText(hive.hiveTypeOther)
        } else {
            binding.textInputLayoutHiveTypeOther.visibility = View.GONE
        }
        binding.autoCompleteTextViewFrameType.setText(hive.frameType, false)
        if (hive.frameType.equals(getString(R.string.other), ignoreCase = true) && !hive.frameTypeOther.isNullOrEmpty()) {
            binding.textInputLayoutFrameTypeOther.visibility = View.VISIBLE
            binding.editTextFrameTypeOther.setText(hive.frameTypeOther)
        } else {
            binding.textInputLayoutFrameTypeOther.visibility = View.GONE
        }
        binding.autoCompleteTextViewBreed.setText(hive.breed, false)
        if (hive.breed.equals(getString(R.string.other), ignoreCase = true) && !hive.breedOther.isNullOrEmpty()) {
            binding.textInputLayoutBreedOther.visibility = View.VISIBLE
            binding.editTextBreedOther.setText(hive.breedOther)
        } else {
            binding.textInputLayoutBreedOther.visibility = View.GONE
        }
        binding.editTextFramesEggs.setText(hive.framesEggs?.toString())
        binding.editTextFramesOpenBrood.setText(hive.framesOpenBrood?.toString())
        binding.editTextFramesCappedBrood.setText(hive.framesCappedBrood?.toString())
        binding.editTextFramesFeed.setText(hive.framesFeed?.toString())
        binding.autoCompleteTextViewQueenTagColor.setText(hive.queenTagColor, false)
        if (hive.queenTagColor.equals(getString(R.string.other), ignoreCase = true) && !hive.queenTagColorOther.isNullOrEmpty()) {
            binding.textInputLayoutQueenTagColorOther.visibility = View.VISIBLE
            binding.editTextQueenTagColorOther.setText(hive.queenTagColorOther)
        } else {
            binding.textInputLayoutQueenTagColorOther.visibility = View.GONE
        }
        binding.editTextQueenNumber.setText(hive.queenNumber)
        binding.editTextQueenYear.setText(hive.queenYear)
        binding.editTextQueenLine.setText(hive.queenLine)
        binding.editTextQueenCells.setText(hive.queenCells?.toString())
        selectedIsolationFromDateMillis = hive.isolationFromDate
        updateDateEditText(selectedIsolationFromDateMillis, binding.editTextIsolationFromDate)
        selectedIsolationToDateMillis = hive.isolationToDate
        updateDateEditText(selectedIsolationToDateMillis, binding.editTextIsolationToDate)
        when (hive.defensivenessRating) {
            1 -> binding.radioDefensiveness1.isChecked = true
            2 -> binding.radioDefensiveness2.isChecked = true
            3 -> binding.radioDefensiveness3.isChecked = true
            4 -> binding.radioDefensiveness4.isChecked = true
        }
        binding.editTextGivenBuiltCombs.setText(hive.givenBuiltCombs?.toString())
        binding.editTextGivenFoundation.setText(hive.givenFoundation?.toString())
        binding.editTextGivenBrood.setText(hive.givenBrood?.toString())
        binding.editTextGivenBeesKg.setText(hive.givenBeesKg?.toString())
        binding.editTextGivenHoneyKg.setText(hive.givenHoneyKg?.toString())
        binding.editTextGivenSugarKg.setText(hive.givenSugarKg?.toString())
        binding.editTextTreatment.setText(hive.treatment)
        selectedHiveLastInspectionDateMillis = hive.lastInspectionDate
        updateDateEditText(selectedHiveLastInspectionDateMillis, binding.editTextHiveLastInspectionDate)
        binding.editTextHiveNotes.setText(hive.notes)
    }

    private fun updateCalculatedQuantity() {
        if (binding.checkboxAddMultipleHives.isChecked && binding.checkboxAutomaticNumbering.isChecked) {
            val startNumStr = binding.editTextFromHiveNumber.text.toString()
            val endNumStr = binding.editTextToHiveNumber.text.toString()
            val startingHiveNumber = startNumStr.toIntOrNull()
            val endingHiveNumber = endNumStr.toIntOrNull()
            if (startingHiveNumber != null && endingHiveNumber != null && endingHiveNumber >= startingHiveNumber) {
                val calculatedCount = endingHiveNumber - startingHiveNumber + 1
                binding.editTextQuantity.setText(calculatedCount.toString())
            } else if (startingHiveNumber != null && endingHiveNumber == null) {
                binding.editTextQuantity.setText("1")
            } else {
                binding.editTextQuantity.setText("")
            }
            binding.editTextQuantity.isEnabled = false
        } else if (binding.checkboxAddMultipleHives.isChecked && !binding.checkboxAutomaticNumbering.isChecked) {
            binding.editTextQuantity.isEnabled = true
        } else {
            binding.editTextQuantity.isEnabled = false
        }
    }

    private fun saveHive(apiaryId: Long, hiveId: Long, isNewHive: Boolean) {
        val material = binding.autoCompleteTextViewMaterial.text.toString().trim().ifEmpty { null }
        val materialOther = binding.editTextMaterialOther.text.toString().trim().ifEmpty { null }
        val finalMaterial = if (material.equals(getString(R.string.hint_other_material), ignoreCase = true)) materialOther else material
        val hiveType = binding.autoCompleteTextViewHiveType.text.toString().trim().ifEmpty { null }
        val hiveTypeOther = binding.editTextHiveTypeOther.text.toString().trim().ifEmpty { null }
        val finalHiveType = if (hiveType.equals(getString(R.string.hint_other_hive_type), ignoreCase = true)) hiveTypeOther else hiveType
        val frameType = binding.autoCompleteTextViewFrameType.text.toString().trim().ifEmpty { null }
        val frameTypeOther = binding.editTextFrameTypeOther.text.toString().trim().ifEmpty { null }
        val finalFrameType = if (frameType.equals(getString(R.string.hint_other_frame_type), ignoreCase = true)) frameTypeOther else frameType
        val breed = binding.autoCompleteTextViewBreed.text.toString().trim().ifEmpty { null }
        val breedOther = binding.editTextBreedOther.text.toString().trim().ifEmpty { null }
        val finalBreed = if (breed.equals(getString(R.string.hint_other_breed), ignoreCase = true)) breedOther else breed
        val framesTotal = binding.textViewFramesTotalValue.text.toString().toIntOrNull()
        val framesEggs = binding.editTextFramesEggs.text.toString().trim().toIntOrNull()
        val framesOpenBrood = binding.editTextFramesOpenBrood.text.toString().trim().toIntOrNull()
        val framesCappedBrood = binding.editTextFramesCappedBrood.text.toString().trim().toIntOrNull()
        val framesFeed = binding.editTextFramesFeed.text.toString().trim().toIntOrNull()
        val queenTagColor = binding.autoCompleteTextViewQueenTagColor.text.toString().trim().ifEmpty { null }
        val queenTagColorOther = binding.editTextQueenTagColorOther.text.toString().trim().ifEmpty { null }
        val finalQueenTagColor = if (queenTagColor.equals(getString(R.string.hint_other_color), ignoreCase = true)) queenTagColorOther else queenTagColor
        val queenNumber = binding.editTextQueenNumber.text.toString().trim().ifEmpty { null }
        val queenYear = binding.editTextQueenYear.text.toString().trim().ifEmpty { null }
        val queenLine = binding.editTextQueenLine.text.toString().trim().ifEmpty { null }
        val queenCells = binding.editTextQueenCells.text.toString().trim().toIntOrNull()
        val isolationFromDate = selectedIsolationFromDateMillis
        val isolationToDate = selectedIsolationToDateMillis
        val defensivenessRating = when (binding.radioGroupDefensiveness.checkedRadioButtonId) {
            R.id.radio_defensiveness_1 -> 1
            R.id.radio_defensiveness_2 -> 2
            R.id.radio_defensiveness_3 -> 3
            R.id.radio_defensiveness_4 -> 4
            else -> null
        }
        val givenBuiltCombs = binding.editTextGivenBuiltCombs.text.toString().trim().toIntOrNull()
        val givenFoundation = binding.editTextGivenFoundation.text.toString().trim().toIntOrNull()
        val givenBrood = binding.editTextGivenBrood.text.toString().trim().toIntOrNull()
        val givenBeesKg = binding.editTextGivenBeesKg.text.toString().trim().toDoubleOrNull()
        val givenHoneyKg = binding.editTextGivenHoneyKg.text.toString().trim().toDoubleOrNull()
        val givenSugarKg = binding.editTextGivenSugarKg.text.toString().trim().toDoubleOrNull()
        val treatment = binding.editTextTreatment.text.toString().trim().ifEmpty { null }
        val notes = binding.editTextHiveNotes.text.toString().trim().ifEmpty { null }

        if (isNewHive && binding.checkboxAddMultipleHives.isChecked) {
            val autoNumber = binding.checkboxAutomaticNumbering.isChecked
            var quantity: Int?
            var startingHiveNumber: Int? = null
            var endingHiveNumber: Int? = null
            if (autoNumber) {
                val startNumStr = binding.editTextFromHiveNumber.text.toString().trim()
                val endNumStr = binding.editTextToHiveNumber.text.toString().trim()
                startingHiveNumber = startNumStr.toIntOrNull()
                endingHiveNumber = endNumStr.toIntOrNull()
                if (startingHiveNumber == null || startingHiveNumber <= 0) {
                    binding.textInputLayoutFromHiveNumber.error = getString(R.string.error_invalid_number_range)
                    return
                } else {
                    binding.textInputLayoutFromHiveNumber.error = null
                }
                if (endingHiveNumber == null || endingHiveNumber < startingHiveNumber) {
                    binding.textInputLayoutToHiveNumber.error = getString(R.string.error_invalid_number_range)
                    return
                } else {
                    binding.textInputLayoutToHiveNumber.error = null
                }
                quantity = endingHiveNumber - startingHiveNumber + 1
            } else {
                val quantityStr = binding.editTextQuantity.text.toString().trim()
                quantity = quantityStr.toIntOrNull()
                if (quantity == null || quantity <= 0) {
                    binding.textInputLayoutQuantity.error =
                        getString(R.string.error_must_be_positive_number)
                    return
                } else {
                    binding.textInputLayoutQuantity.error = null
                }
            }
            addEditHiveViewModel.saveOrUpdateHives(
                apiaryId = apiaryId, hiveType = finalHiveType, hiveTypeOther = hiveTypeOther,
                frameType = finalFrameType, frameTypeOther = frameTypeOther, material = finalMaterial,
                materialOther = materialOther, breed = finalBreed, breedOther = breedOther,
                lastInspectionDate = selectedHiveLastInspectionDateMillis, notes = notes,
                quantity = quantity, autoNumber = autoNumber, startingHiveNumber = startingHiveNumber,
                endingHiveNumber = endingHiveNumber, queenTagColor = finalQueenTagColor, queenTagColorOther = queenTagColorOther,
                queenNumber = queenNumber, queenYear = queenYear, queenLine = queenLine,
                queenCells = queenCells, isolationFromDate = isolationFromDate, isolationToDate = isolationToDate,
                defensivenessRating = defensivenessRating, framesTotal = framesTotal, framesEggs = framesEggs,
                framesOpenBrood = framesOpenBrood, framesCappedBrood = framesCappedBrood, framesFeed = framesFeed,
                givenBuiltCombs = givenBuiltCombs, givenFoundation = givenFoundation, givenBrood = givenBrood,
                givenBeesKg = givenBeesKg, givenHoneyKg = givenHoneyKg, givenSugarKg = givenSugarKg,
                treatment = treatment
            )
        } else {
            val hiveNumber = binding.editTextHiveNumber.text.toString().trim().ifEmpty { null }
            val hive = Hive(
                id = if (isNewHive) 0L else hiveId, apiaryId = apiaryId, hiveNumber = hiveNumber,
                hiveType = finalHiveType, hiveTypeOther = hiveTypeOther, frameType = finalFrameType,
                frameTypeOther = frameTypeOther, material = finalMaterial, materialOther = materialOther,
                breed = finalBreed, breedOther = breedOther, lastInspectionDate = selectedHiveLastInspectionDateMillis,
                notes = notes, queenTagColor = finalQueenTagColor, queenTagColorOther = queenTagColorOther,
                queenNumber = queenNumber, queenYear = queenYear, queenLine = queenLine,
                queenCells = queenCells, isolationFromDate = isolationFromDate, isolationToDate = isolationToDate,
                defensivenessRating = defensivenessRating, framesTotal = framesTotal, framesEggs = framesEggs,
                framesOpenBrood = framesOpenBrood, framesCappedBrood = framesCappedBrood, framesFeed = framesFeed,
                givenBuiltCombs = givenBuiltCombs, givenFoundation = givenFoundation, givenBrood = givenBrood,
                givenBeesKg = givenBeesKg, givenHoneyKg = givenHoneyKg, givenSugarKg = givenSugarKg,
                treatment = treatment
            )
            addEditHiveViewModel.saveOrUpdateHive(hive)
        }
    }

    // The missing function is now included
    private fun showOverwriteApiaryDateDialog(apiaryId: Long, newDateMillis: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_update_apiary_date))
            .setMessage(getString(R.string.dialog_message_update_apiary_date))
            .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                addEditHiveViewModel.updateApiaryLastInspectionDate(apiaryId, newDateMillis)
                Toast.makeText(requireContext(),
                    getString(R.string.message_apiary_date_updated), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton(getString(R.string.dialog_no)) { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}