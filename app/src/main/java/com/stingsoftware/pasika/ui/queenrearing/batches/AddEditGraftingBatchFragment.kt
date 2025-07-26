package com.stingsoftware.pasika.ui.queenrearing.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.databinding.FragmentAddEditGraftingBatchBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddEditGraftingBatchFragment : Fragment() {

    private var _binding: FragmentAddEditGraftingBatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditGraftingBatchViewModel by viewModels()
    private val args: AddEditGraftingBatchFragmentArgs by navArgs()
    private var motherColoniesList: List<Hive> = emptyList()
    private var selectedGraftingDate: Long = System.currentTimeMillis()
    private var currentBatch: GraftingBatch? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditGraftingBatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMotherColonyDropdown()
        setupDatePicker()
        // Set the initial date in the view when creating a new batch
        updateDateInView()

        viewModel.batch.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                currentBatch = it
                binding.batchNameEditText.setText(it.name)
                selectedGraftingDate = it.graftingDate
                binding.cellsGraftedEditText.setText(it.cellsGrafted.toString())
                binding.notesEditText.setText(it.notes)
                // This will overwrite the default date if editing an existing batch
                updateDateInView()
            }
        }

        binding.saveButton.setOnClickListener {
            saveBatch()
        }
    }

    private fun setupDatePicker() {
        binding.graftingDateEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_grafting_date))
                .setSelection(selectedGraftingDate)
                .build()

            datePicker.addOnPositiveButtonClickListener {
                selectedGraftingDate = it
                updateDateInView()
            }
            datePicker.show(parentFragmentManager, getString(R.string.date_picker))
        }
    }

    private fun updateDateInView() {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        binding.graftingDateEditText.setText(sdf.format(Date(selectedGraftingDate)))
    }

    private fun setupMotherColonyDropdown() {
        viewModel.motherColonies.observe(viewLifecycleOwner) { hives ->
            motherColoniesList = hives
            val hiveNames = hives.map { it.hiveNumber }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hiveNames)
            binding.motherHiveAutoComplete.setAdapter(adapter)

            // If we are editing a batch, set the selected mother hive
            currentBatch?.let { batch ->
                val motherHive = hives.find { it.id == batch.motherHiveId }
                motherHive?.let {
                    binding.motherHiveAutoComplete.setText(it.hiveNumber, false)
                }
            }
        }
    }

    private fun saveBatch() {
        val batchName = binding.batchNameEditText.text.toString().trim()
        val motherHiveName = binding.motherHiveAutoComplete.text.toString()
        val cellCountText = binding.cellsGraftedEditText.text.toString()
        val notes = binding.notesEditText.text.toString()

        val motherHive = motherColoniesList.find { it.hiveNumber == motherHiveName }
        var isValid = true

        if (batchName.isBlank()) {
            binding.batchNameLayout.error = getString(R.string.error_field_cannot_be_empty, "Batch Name")
            isValid = false
        } else {
            binding.batchNameLayout.error = null
        }

        if (motherHive == null) {
            binding.motherHiveLayout.error = "Please select a mother colony"
            isValid = false
        } else {
            binding.motherHiveLayout.error = null
        }

        val cellCount = cellCountText.toIntOrNull()
        if (cellCount == null || cellCount <= 0) {
            binding.cellsGraftedLayout.error = getString(R.string.error_must_be_positive_number, "Number of cells")
            isValid = false
        } else {
            binding.cellsGraftedLayout.error = null
        }

        if (!isValid) return

        val batchToSave = currentBatch?.copy(
            name = batchName,
            graftingDate = selectedGraftingDate,
            motherHiveId = motherHive!!.id,
            cellsGrafted = cellCount!!,
            notes = notes
        ) ?: GraftingBatch(
            name = batchName,
            graftingDate = selectedGraftingDate,
            motherHiveId = motherHive!!.id,
            cellsGrafted = cellCount!!,
            notes = notes
        )

        viewModel.saveBatch(batchToSave, cellCount)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
