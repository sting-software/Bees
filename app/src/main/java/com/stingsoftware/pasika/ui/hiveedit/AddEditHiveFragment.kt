package com.stingsoftware.pasika.ui.hiveedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveRole
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
    private val addEditHiveViewModel: AddEditHiveViewModel by viewModels()

    private var _binding: FragmentAddEditHiveBinding? = null
    private val binding get() = _binding!!

    // State tracking variables
    private var isEditMode = false
    private var originalHive: Hive? = null // Crucial for comparing changes
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

        setupMenu()
        setupBackButtonHandler()
        isEditMode = args.hiveId != -1L

        // Setup all UI components and listeners
        setupSpinners()
        setupRoleSpinner()
        setupConditionalViews(!isEditMode)
        setupDatePickers()
        setupTextWatchers()
        setupFramesTextWatchers()

        if (isEditMode) {
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_edit_hive)
            addEditHiveViewModel.getHive(args.hiveId).observe(viewLifecycleOwner) { hive ->
                hive?.let {
                    originalHive = it
                    populateHiveData(it)
                    updateFramesTotal()
                }
            }
        } else {
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_add_hive)
            updateFramesTotal()
        }

        binding.buttonSaveHive.setOnClickListener { handleSave() }
        binding.buttonCancelHive.setOnClickListener { findNavController().popBackStack() }
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
                        handleSave()
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

    private fun hasChanges(): Boolean {
        val currentHive = createHiveFromInput()
        return if (isEditMode) {
            originalHive != null && originalHive != currentHive
        } else {
            // For a new hive, any non-default value is a change.
            // Create a hive object that represents a completely empty form.
            val defaultHive = Hive(
                id = 0L,
                apiaryId = args.apiaryId,
                hiveNumber = null,
                hiveType = null,
                hiveTypeOther = null,
                frameType = null,
                frameTypeOther = null,
                material = null,
                materialOther = null,
                breed = null,
                breedOther = null,
                lastInspectionDate = null,
                notes = null,
                queenTagColor = null,
                queenTagColorOther = null,
                queenNumber = null,
                queenYear = null,
                queenLine = null,
                queenCells = null,
                isolationFromDate = null,
                isolationToDate = null,
                defensivenessRating = null,
                framesTotal = 0, // Total is 0 when individual frames are empty/0
                framesEggs = null,
                framesOpenBrood = null,
                framesCappedBrood = null,
                framesFeed = null,
                givenBuiltCombs = null,
                givenFoundation = null,
                givenBrood = null,
                givenBeesKg = null,
                givenHoneyKg = null,
                givenSugarKg = null,
                treatment = null,
                role = HiveRole.PRODUCTION // Default role for new hives
            )
            currentHive != defaultHive
        }
    }

    private fun handleAutoSaveOnExit() {
        if (hasChanges()) {
            handleSave()
        } else {
            findNavController().popBackStack()
        }
    }


    private fun handleSave() {
        // Divert to a separate function if adding multiple hives
        if (!isEditMode && binding.checkboxAddMultipleHives.isChecked) {
            saveMultipleHives()
            return
        }

        // Standard logic for saving a single hive (new or edited)
        val hiveToSave = createHiveFromInput()
        val dateChanged =
            isEditMode && originalHive?.lastInspectionDate != selectedHiveLastInspectionDateMillis

        if (dateChanged && selectedHiveLastInspectionDateMillis != null) {
            showUpdateAllHivesDialog(hiveToSave)
        } else {
            saveHiveAndExit(hiveToSave)
        }
    }

    private fun showUpdateAllHivesDialog(hiveToSave: Hive) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.update_inspection_date))
            .setMessage(getString(R.string.apply_this_new_date_to_all_hives_in_this_apiary))
            .setPositiveButton(getString(R.string.update_all)) { _, _ ->
                addEditHiveViewModel.updateInspectionDateForApiary(
                    hiveToSave.apiaryId,
                    hiveToSave.lastInspectionDate!!
                )
                saveHiveAndExit(hiveToSave)
            }
            .setNegativeButton(getString(R.string.just_this_one)) { _, _ ->
                saveHiveAndExit(hiveToSave)
            }
            .setNeutralButton(R.string.action_cancel, null)
            .show()
    }

    private fun saveHiveAndExit(hive: Hive) {
        addEditHiveViewModel.saveOrUpdateHive(hive)
        Toast.makeText(
            requireContext(),
            if (isEditMode) getString(R.string.message_hive_updated) else getString(
                R.string.message_hive_saved
            ),
            Toast.LENGTH_SHORT
        ).show()
        findNavController().popBackStack()
    }

    private fun saveMultipleHives() {
        val autoNumber = binding.checkboxAutomaticNumbering.isChecked
        val quantity: Int?
        var startingHiveNumber: Int? = null
        var endingHiveNumber: Int? = null

        if (autoNumber) {
            startingHiveNumber = binding.editTextFromHiveNumber.text.toString().trim().toIntOrNull()
            endingHiveNumber = binding.editTextToHiveNumber.text.toString().trim().toIntOrNull()
            if (startingHiveNumber == null || endingHiveNumber == null || endingHiveNumber < startingHiveNumber) {
                binding.textInputLayoutToHiveNumber.error =
                    getString(R.string.error_invalid_number_range)
                return
            }
            quantity = endingHiveNumber - startingHiveNumber + 1
        } else {
            quantity = binding.editTextQuantity.text.toString().trim().toIntOrNull()
            if (quantity == null || quantity <= 0) {
                binding.textInputLayoutQuantity.error =
                    getString(R.string.error_must_be_positive_number)
                return
            }
        }

        // Gather all data from the form to pass to the ViewModel
        val hiveData = createHiveFromInput()

        addEditHiveViewModel.saveOrUpdateHives(
            apiaryId = args.apiaryId,
            hiveType = hiveData.hiveType,
            hiveTypeOther = hiveData.hiveTypeOther,
            frameType = hiveData.frameType,
            frameTypeOther = hiveData.frameTypeOther,
            material = hiveData.material,
            materialOther = hiveData.materialOther,
            breed = hiveData.breed,
            breedOther = hiveData.breedOther,
            lastInspectionDate = hiveData.lastInspectionDate,
            notes = hiveData.notes,
            quantity = quantity,
            autoNumber = autoNumber,
            startingHiveNumber = startingHiveNumber,
            endingHiveNumber = endingHiveNumber,
            queenTagColor = hiveData.queenTagColor,
            queenTagColorOther = hiveData.queenTagColorOther,
            queenNumber = hiveData.queenNumber,
            queenYear = hiveData.queenYear,
            queenLine = hiveData.queenLine,
            queenCells = hiveData.queenCells,
            isolationFromDate = hiveData.isolationFromDate,
            isolationToDate = hiveData.isolationToDate,
            defensivenessRating = hiveData.defensivenessRating,
            framesTotal = hiveData.framesTotal,
            framesEggs = hiveData.framesEggs,
            framesOpenBrood = hiveData.framesOpenBrood,
            framesCappedBrood = hiveData.framesCappedBrood,
            framesFeed = hiveData.framesFeed,
            givenBuiltCombs = hiveData.givenBuiltCombs,
            givenFoundation = hiveData.givenFoundation,
            givenBrood = hiveData.givenBrood,
            givenBeesKg = hiveData.givenBeesKg,
            givenHoneyKg = hiveData.givenHoneyKg,
            givenSugarKg = hiveData.givenSugarKg,
            treatment = hiveData.treatment,
            role = hiveData.role
        )
        Toast.makeText(
            requireContext(),
            getString(R.string.hives_added_successfully, quantity), Toast.LENGTH_SHORT
        ).show()
        findNavController().popBackStack()
    }

    private fun createHiveFromInput(): Hive {
        val material = binding.autoCompleteTextViewMaterial.text.toString().trim().ifEmpty { null }
        val materialOther = binding.editTextMaterialOther.text.toString().trim().ifEmpty { null }
        val finalMaterial = if (material.equals(
                getString(R.string.other),
                ignoreCase = true
            )
        ) materialOther else material

        val hiveType = binding.autoCompleteTextViewHiveType.text.toString().trim().ifEmpty { null }
        val hiveTypeOther = binding.editTextHiveTypeOther.text.toString().trim().ifEmpty { null }
        val finalHiveType = if (hiveType.equals(
                getString(R.string.other),
                ignoreCase = true
            )
        ) hiveTypeOther else hiveType

        val frameType =
            binding.autoCompleteTextViewFrameType.text.toString().trim().ifEmpty { null }
        val frameTypeOther = binding.editTextFrameTypeOther.text.toString().trim().ifEmpty { null }
        val finalFrameType = if (frameType.equals(
                getString(R.string.other),
                ignoreCase = true
            )
        ) frameTypeOther else frameType

        val breed = binding.autoCompleteTextViewBreed.text.toString().trim().ifEmpty { null }
        val breedOther = binding.editTextBreedOther.text.toString().trim().ifEmpty { null }
        val finalBreed =
            if (breed.equals(getString(R.string.other), ignoreCase = true)) breedOther else breed

        val framesTotal = binding.textViewFramesTotalValue.text.toString().toIntOrNull()
        val framesEggs = binding.editTextFramesEggs.text.toString().trim().toIntOrNull()
        val framesOpenBrood = binding.editTextFramesOpenBrood.text.toString().trim().toIntOrNull()
        val framesCappedBrood =
            binding.editTextFramesCappedBrood.text.toString().trim().toIntOrNull()
        val framesFeed = binding.editTextFramesFeed.text.toString().trim().toIntOrNull()

        val queenTagColor =
            binding.autoCompleteTextViewQueenTagColor.text.toString().trim().ifEmpty { null }
        val queenTagColorOther =
            binding.editTextQueenTagColorOther.text.toString().trim().ifEmpty { null }
        val finalQueenTagColor = if (queenTagColor.equals(
                getString(R.string.other),
                ignoreCase = true
            )
        ) queenTagColorOther else queenTagColor

        val queenNumber = binding.editTextQueenNumber.text.toString().trim().ifEmpty { null }
        val queenYear = binding.editTextQueenYear.text.toString().trim().ifEmpty { null }
        val queenLine = binding.editTextQueenLine.text.toString().trim().ifEmpty { null }

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

        val selectedRoleLabel = binding.autoCompleteTextViewRole.text.toString()
        val role = HiveRole.entries.find { it.getLabel(requireContext()) == selectedRoleLabel }
            ?: HiveRole.PRODUCTION

        return Hive(
            id = if (isEditMode) args.hiveId else 0L,
            apiaryId = args.apiaryId,
            hiveNumber = if (isEditMode) binding.editTextHiveNumber.text.toString().trim()
                .ifEmpty { null } else null,
            hiveType = finalHiveType,
            hiveTypeOther = hiveTypeOther,
            frameType = finalFrameType,
            frameTypeOther = frameTypeOther,
            material = finalMaterial,
            materialOther = materialOther,
            breed = finalBreed,
            breedOther = breedOther,
            lastInspectionDate = selectedHiveLastInspectionDateMillis,
            notes = notes,
            queenTagColor = finalQueenTagColor,
            queenTagColorOther = queenTagColorOther,
            queenNumber = queenNumber,
            queenYear = queenYear,
            queenLine = queenLine,
            queenCells = null, // UI for this is commented out, so passing null
            isolationFromDate = selectedIsolationFromDateMillis,
            isolationToDate = selectedIsolationToDateMillis,
            defensivenessRating = defensivenessRating,
            framesTotal = framesTotal,
            framesEggs = framesEggs,
            framesOpenBrood = framesOpenBrood,
            framesCappedBrood = framesCappedBrood,
            framesFeed = framesFeed,
            givenBuiltCombs = givenBuiltCombs,
            givenFoundation = givenFoundation,
            givenBrood = givenBrood,
            givenBeesKg = givenBeesKg,
            givenHoneyKg = givenHoneyKg,
            givenSugarKg = givenSugarKg,
            treatment = treatment,
            role = role
        )
    }

    private fun populateHiveData(hive: Hive) {
        binding.editTextHiveNumber.setText(hive.hiveNumber)
        binding.autoCompleteTextViewMaterial.setText(hive.material, false)
        binding.editTextMaterialOther.setText(hive.materialOther)
        toggleOtherFieldVisibility(hive.material ?: "", binding.textInputLayoutMaterialOther)

        binding.autoCompleteTextViewHiveType.setText(hive.hiveType, false)
        binding.editTextHiveTypeOther.setText(hive.hiveTypeOther)
        toggleOtherFieldVisibility(hive.hiveType ?: "", binding.textInputLayoutHiveTypeOther)

        binding.autoCompleteTextViewFrameType.setText(hive.frameType, false)
        binding.editTextFrameTypeOther.setText(hive.frameTypeOther)
        toggleOtherFieldVisibility(hive.frameType ?: "", binding.textInputLayoutFrameTypeOther)

        binding.autoCompleteTextViewBreed.setText(hive.breed, false)
        binding.editTextBreedOther.setText(hive.breedOther)
        toggleOtherFieldVisibility(hive.breed ?: "", binding.textInputLayoutBreedOther)

        binding.editTextFramesEggs.setText(hive.framesEggs?.toString())
        binding.editTextFramesOpenBrood.setText(hive.framesOpenBrood?.toString())
        binding.editTextFramesCappedBrood.setText(hive.framesCappedBrood?.toString())
        binding.editTextFramesFeed.setText(hive.framesFeed?.toString())

        binding.autoCompleteTextViewQueenTagColor.setText(hive.queenTagColor, false)
        binding.editTextQueenTagColorOther.setText(hive.queenTagColorOther)
        toggleOtherFieldVisibility(
            hive.queenTagColor ?: "",
            binding.textInputLayoutQueenTagColorOther
        )

        binding.editTextQueenNumber.setText(hive.queenNumber)
        binding.editTextQueenYear.setText(hive.queenYear)
        binding.editTextQueenLine.setText(hive.queenLine)

        selectedIsolationFromDateMillis = hive.isolationFromDate
        updateDateEditText(selectedIsolationFromDateMillis, binding.editTextIsolationFromDate)
        selectedIsolationToDateMillis = hive.isolationToDate
        updateDateEditText(selectedIsolationToDateMillis, binding.editTextIsolationToDate)

        when (hive.defensivenessRating) {
            1 -> binding.radioDefensiveness1.isChecked = true
            2 -> binding.radioDefensiveness2.isChecked = true
            3 -> binding.radioDefensiveness3.isChecked = true
            4 -> binding.radioDefensiveness4.isChecked = true
            else -> binding.radioGroupDefensiveness.clearCheck()
        }

        binding.editTextGivenBuiltCombs.setText(hive.givenBuiltCombs?.toString())
        binding.editTextGivenFoundation.setText(hive.givenFoundation?.toString())
        binding.editTextGivenBrood.setText(hive.givenBrood?.toString())
        binding.editTextGivenBeesKg.setText(hive.givenBeesKg?.toString())
        binding.editTextGivenHoneyKg.setText(hive.givenHoneyKg?.toString())
        binding.editTextGivenSugarKg.setText(hive.givenSugarKg?.toString())
        binding.editTextTreatment.setText(hive.treatment)

        selectedHiveLastInspectionDateMillis = hive.lastInspectionDate
        updateDateEditText(
            selectedHiveLastInspectionDateMillis,
            binding.editTextHiveLastInspectionDate
        )
        binding.editTextHiveNotes.setText(hive.notes)

        binding.autoCompleteTextViewRole.setText(hive.role.getLabel(requireContext()), false)
    }

    private fun setupDatePickers() {
        binding.editTextHiveLastInspectionDate.setOnClickListener {
            showDatePickerDialog(
                ::selectedHiveLastInspectionDateMillis,
                binding.editTextHiveLastInspectionDate
            )
        }
        binding.textInputLayoutHiveLastInspectionDate.setEndIconOnClickListener {
            showDatePickerDialog(
                ::selectedHiveLastInspectionDateMillis,
                binding.editTextHiveLastInspectionDate
            )
        }
        binding.editTextIsolationFromDate.setOnClickListener {
            showDatePickerDialog(
                ::selectedIsolationFromDateMillis,
                binding.editTextIsolationFromDate
            )
        }
        binding.textInputLayoutIsolationFromDate.setEndIconOnClickListener {
            showDatePickerDialog(
                ::selectedIsolationFromDateMillis,
                binding.editTextIsolationFromDate
            )
        }
        binding.editTextIsolationToDate.setOnClickListener {
            showDatePickerDialog(
                ::selectedIsolationToDateMillis,
                binding.editTextIsolationToDate
            )
        }
        binding.textInputLayoutIsolationToDate.setEndIconOnClickListener {
            showDatePickerDialog(
                ::selectedIsolationToDateMillis,
                binding.editTextIsolationToDate
            )
        }
    }

    private fun showDatePickerDialog(
        dateMillisProperty: kotlin.reflect.KMutableProperty0<Long?>,
        editText: TextInputEditText
    ) {
        val calendar = Calendar.getInstance()
        dateMillisProperty.get()?.let { calendar.timeInMillis = it }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val newCalendar = Calendar.getInstance()
                .apply { set(selectedYear, selectedMonth, selectedDayOfMonth) }
            dateMillisProperty.set(newCalendar.timeInMillis)
            updateDateEditText(newCalendar.timeInMillis, editText)
        }, year, month, day).show()
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

    private fun setupSpinners() {
        val materialChoices = resources.getStringArray(R.array.material_choices)
        val materialAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, materialChoices)
        binding.autoCompleteTextViewMaterial.setAdapter(materialAdapter)
        binding.autoCompleteTextViewMaterial.setOnItemClickListener { _, _, _, _ ->
            toggleOtherFieldVisibility(
                binding.autoCompleteTextViewMaterial.text.toString(),
                binding.textInputLayoutMaterialOther
            )
        }

        val hiveTypes = resources.getStringArray(R.array.hive_types)
        val hiveTypeAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, hiveTypes)
        binding.autoCompleteTextViewHiveType.setAdapter(hiveTypeAdapter)
        binding.autoCompleteTextViewHiveType.setOnItemClickListener { _, _, _, _ ->
            toggleOtherFieldVisibility(
                binding.autoCompleteTextViewHiveType.text.toString(),
                binding.textInputLayoutHiveTypeOther
            )
        }

        val frameTypes = resources.getStringArray(R.array.frame_types)
        val frameTypeAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, frameTypes)
        binding.autoCompleteTextViewFrameType.setAdapter(frameTypeAdapter)
        binding.autoCompleteTextViewFrameType.setOnItemClickListener { _, _, _, _ ->
            toggleOtherFieldVisibility(
                binding.autoCompleteTextViewFrameType.text.toString(),
                binding.textInputLayoutFrameTypeOther
            )
        }

        val breedChoices = resources.getStringArray(R.array.breed_choices)
        val breedAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, breedChoices)
        binding.autoCompleteTextViewBreed.setAdapter(breedAdapter)
        binding.autoCompleteTextViewBreed.setOnItemClickListener { _, _, _, _ ->
            toggleOtherFieldVisibility(
                binding.autoCompleteTextViewBreed.text.toString(),
                binding.textInputLayoutBreedOther
            )
        }

        val queenTagColors = resources.getStringArray(R.array.queen_tag_colors)
        val queenTagColorAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, queenTagColors)
        binding.autoCompleteTextViewQueenTagColor.setAdapter(queenTagColorAdapter)
        binding.autoCompleteTextViewQueenTagColor.setOnItemClickListener { _, _, _, _ ->
            toggleOtherFieldVisibility(
                binding.autoCompleteTextViewQueenTagColor.text.toString(),
                binding.textInputLayoutQueenTagColorOther
            )
        }
    }

    private fun setupRoleSpinner() {
        val roleLabels = HiveRole.entries.map { it.getLabel(requireContext()) }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, roleLabels)
        binding.autoCompleteTextViewRole.setAdapter(adapter)

        if (!isEditMode) {
            binding.autoCompleteTextViewRole.setText(
                HiveRole.PRODUCTION.getLabel(requireContext()),
                false
            )
        }
    }

    private fun toggleOtherFieldVisibility(
        selectedItem: String,
        otherInputField: com.google.android.material.textfield.TextInputLayout
    ) {
        otherInputField.visibility =
            if (selectedItem.equals(getString(R.string.other), ignoreCase = true)) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun setupConditionalViews(isNewHive: Boolean) {
        if (isNewHive) {
            binding.checkboxAddMultipleHives.visibility = View.VISIBLE
            binding.textInputLayoutHiveNumber.visibility = View.VISIBLE
            binding.textInputLayoutQuantity.visibility = View.GONE
            binding.editTextQuantity.setText("")

            binding.checkboxAddMultipleHives.setOnCheckedChangeListener { _, isChecked ->
                binding.checkboxAutomaticNumbering.visibility =
                    if (isChecked) View.VISIBLE else View.GONE
                binding.textInputLayoutHiveNumber.visibility =
                    if (isChecked) View.GONE else View.VISIBLE
                binding.textInputLayoutQuantity.visibility =
                    if (isChecked) View.VISIBLE else View.GONE
                if (isChecked) {
                    binding.editTextQuantity.setText("1")
                } else {
                    binding.editTextHiveNumber.setText("")
                    binding.checkboxAutomaticNumbering.isChecked = false
                }
            }

            binding.checkboxAutomaticNumbering.setOnCheckedChangeListener { _, isChecked ->
                binding.layoutAutoNumberingRange.visibility =
                    if (isChecked) View.VISIBLE else View.GONE
                binding.editTextQuantity.isEnabled = !isChecked
                if (isChecked) {
                    binding.editTextQuantity.setText("")
                } else {
                    binding.editTextQuantity.setText("1")
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

    private fun setupTextWatchers() {
        val autoNumberWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.textInputLayoutFromHiveNumber.error = null
                binding.textInputLayoutToHiveNumber.error = null
            }

            override fun afterTextChanged(s: Editable?) {
                updateCalculatedQuantity()
            }
        }
        binding.editTextFromHiveNumber.addTextChangedListener(autoNumberWatcher)
        binding.editTextToHiveNumber.addTextChangedListener(autoNumberWatcher)

        binding.editTextQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.textInputLayoutQuantity.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFramesTextWatchers() {
        val framesWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateFramesTotal()
            }
        }
        binding.editTextFramesEggs.addTextChangedListener(framesWatcher)
        binding.editTextFramesOpenBrood.addTextChangedListener(framesWatcher)
        binding.editTextFramesCappedBrood.addTextChangedListener(framesWatcher)
        binding.editTextFramesFeed.addTextChangedListener(framesWatcher)
    }

    private fun updateFramesTotal() {
        val eggs = binding.editTextFramesEggs.text.toString().toIntOrNull() ?: 0
        val openBrood = binding.editTextFramesOpenBrood.text.toString().toIntOrNull() ?: 0
        val cappedBrood = binding.editTextFramesCappedBrood.text.toString().toIntOrNull() ?: 0
        val feed = binding.editTextFramesFeed.text.toString().toIntOrNull() ?: 0
        val total = eggs + openBrood + cappedBrood + feed
        binding.textViewFramesTotalValue.text = getString(R.string.integer_placeholder, total)
    }

    private fun updateCalculatedQuantity() {
        if (binding.checkboxAutomaticNumbering.isChecked) {
            val startNum = binding.editTextFromHiveNumber.text.toString().toIntOrNull()
            val endNum = binding.editTextToHiveNumber.text.toString().toIntOrNull()
            if (startNum != null && endNum != null && endNum >= startNum) {
                val quantity = endNum - startNum + 1
                binding.editTextQuantity.setText(getString(R.string.integer_placeholder, quantity))
            } else {
                binding.editTextQuantity.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        _binding = null
    }
}
