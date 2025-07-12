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
import androidx.navigation.fragment.navArgs
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

    private val args: BulkEditHiveFragmentArgs by navArgs()

    // ViewModel is now injected by Hilt
    private val bulkEditHiveViewModel: BulkEditHiveViewModel by viewModels()

    private var _binding: FragmentBulkEditHiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var autoNumberingWarningTextView: TextView

    private var selectedDateMillis: Long? = null
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

        activity?.title = "Bulk Edit Hives"

        val hiveTypes = resources.getStringArray(R.array.hive_types)
        val hiveTypeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, hiveTypes)
        binding.autoCompleteTextViewHiveType.setAdapter(hiveTypeAdapter)

        val frameTypes = resources.getStringArray(R.array.frame_types)
        val frameTypeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, frameTypes)
        binding.autoCompleteTextViewFrameType.setAdapter(frameTypeAdapter)

        bulkEditHiveViewModel.selectedHives.observe(viewLifecycleOwner) { hives ->
            if (hives.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No hives selected for bulk editing.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return@observe
            }
            numberOfSelectedHives = hives.size
            binding.textViewSelectedHivesCount.text = getString(R.string.bulk_edit_hives_count, numberOfSelectedHives)
            prefillCommonValues(hives)
        }

        binding.checkboxAutoNumberHivesBulk.setOnCheckedChangeListener { _, isChecked ->
            binding.textInputLayoutStartingHiveNumberBulk.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.textInputLayoutEndingHiveNumberBulk.visibility = if (isChecked) View.VISIBLE else View.GONE
            autoNumberingWarningTextView.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateCalculatedQuantity()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculatedQuantity()
            }
        }
        binding.editTextStartingHiveNumberBulk.addTextChangedListener(textWatcher)
        binding.editTextEndingHiveNumberBulk.addTextChangedListener(textWatcher)

        binding.editTextHiveLastInspectionDate.setOnClickListener {
            showDatePickerDialog()
        }
        binding.textInputLayoutHiveLastInspectionDate.setEndIconOnClickListener {
            showDatePickerDialog()
        }

        binding.buttonSaveBulkEdit.setOnClickListener {
            applyBulkEdit()
        }

        binding.buttonCancelBulkEdit.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun prefillCommonValues(hives: List<Hive>) {
        if (hives.isEmpty()) return

        val firstHive = hives[0]

        if (hives.all { it.hiveType == firstHive.hiveType }) {
            binding.autoCompleteTextViewHiveType.setText(firstHive.hiveType, false)
        }
        if (hives.all { it.frameType == firstHive.frameType }) {
            binding.autoCompleteTextViewFrameType.setText(firstHive.frameType, false)
        }
        if (hives.all { it.framesTotal == firstHive.framesTotal }) {
            binding.editTextNumberOfFrames.setText(firstHive.framesTotal?.toString())
        }
        if (hives.all { it.breed == firstHive.breed }) {
            binding.editTextBreed.setText(firstHive.breed)
        }
        if (hives.all { it.lastInspectionDate == firstHive.lastInspectionDate }) {
            selectedDateMillis = firstHive.lastInspectionDate
            updateDateEditText(firstHive.lastInspectionDate)
        }
        if (hives.all { it.notes == firstHive.notes }) {
            binding.editTextHiveNotes.setText(firstHive.notes)
        }
    }

    private fun updateCalculatedQuantity() {
        if (binding.checkboxAutoNumberHivesBulk.isChecked) {
            val startNumStr = binding.editTextStartingHiveNumberBulk.text?.toString() ?: ""
            val endNumStr = binding.editTextEndingHiveNumberBulk.text?.toString() ?: ""

            val startingHiveNumber = startNumStr.toIntOrNull()
            val endingHiveNumber = endNumStr.toIntOrNull()

            if (startingHiveNumber != null && endingHiveNumber != null && endingHiveNumber >= startingHiveNumber) {
                val calculatedCount = endingHiveNumber - startingHiveNumber + 1
                if (calculatedCount != numberOfSelectedHives) {
                    autoNumberingWarningTextView.text =
                        getString(R.string.bulk_edit_quantity_mismatch_warning, calculatedCount, numberOfSelectedHives)
                    autoNumberingWarningTextView.visibility = View.VISIBLE
                    binding.buttonSaveBulkEdit.isEnabled = false
                } else {
                    autoNumberingWarningTextView.visibility = View.GONE
                    binding.buttonSaveBulkEdit.isEnabled = true
                }
            } else {
                autoNumberingWarningTextView.text = getString(R.string.bulk_edit_invalid_range_warning)
                autoNumberingWarningTextView.visibility = View.VISIBLE
                binding.buttonSaveBulkEdit.isEnabled = false
            }
        } else {
            autoNumberingWarningTextView.visibility = View.GONE
            binding.buttonSaveBulkEdit.isEnabled = true
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        selectedDateMillis?.let {
            calendar.timeInMillis = it
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val newCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDayOfMonth)
            }
            selectedDateMillis = newCalendar.timeInMillis
            updateDateEditText(selectedDateMillis)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateDateEditText(dateMillis: Long?) {
        if (dateMillis != null) {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.editTextHiveLastInspectionDate.setText(formatter.format(Date(dateMillis)))
        } else {
            binding.editTextHiveLastInspectionDate.setText("")
        }
    }

    private fun applyBulkEdit() {
        val hiveType = binding.autoCompleteTextViewHiveType.text?.toString()?.trim()?.ifEmpty { null }
        val frameType = binding.autoCompleteTextViewFrameType.text?.toString()?.trim()?.ifEmpty { null }
        val framesTotal = binding.editTextNumberOfFrames.text?.toString()?.trim()?.toIntOrNull()
        val breed = binding.editTextBreed.text?.toString()?.trim()?.ifEmpty { null }
        val notes = binding.editTextHiveNotes.text?.toString()?.trim()?.ifEmpty { null }

        val autoNumber = binding.checkboxAutoNumberHivesBulk.isChecked
        var startingHiveNumber: Int? = null
        var endingHiveNumber: Int? = null

        if (autoNumber) {
            startingHiveNumber = binding.editTextStartingHiveNumberBulk.text?.toString()?.trim()?.toIntOrNull()
            endingHiveNumber = binding.editTextEndingHiveNumberBulk.text?.toString()?.trim()?.toIntOrNull()

            if (startingHiveNumber == null || startingHiveNumber <= 0) {
                binding.textInputLayoutStartingHiveNumberBulk.error = "Starting number must be a positive number"
                return
            } else {
                binding.textInputLayoutStartingHiveNumberBulk.error = null
            }
        }

        bulkEditHiveViewModel.updateSelectedHives(
            hiveType = hiveType,
            frameType = frameType,
            framesTotal = framesTotal,
            breed = breed,
            lastInspectionDate = selectedDateMillis,
            notes = notes,
            autoNumber = autoNumber,
            startingHiveNumber = startingHiveNumber,
            endingHiveNumber = endingHiveNumber
        )

        Toast.makeText(requireContext(), "Hives updated successfully!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}