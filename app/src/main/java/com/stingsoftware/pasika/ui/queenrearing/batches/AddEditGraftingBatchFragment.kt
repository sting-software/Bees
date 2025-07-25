package com.stingsoftware.pasika.ui.queenrearing.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
    private var motherColoniesList: List<Hive> = emptyList()
    private var selectedGraftingDate: Long = System.currentTimeMillis()

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
        updateDateInView()

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
        }
    }

    private fun saveBatch() {
        val batchName = binding.batchNameEditText.text.toString()
        val motherHiveName = binding.motherHiveAutoComplete.text.toString()
        val cellCount = binding.cellsGraftedEditText.text.toString().toIntOrNull() ?: 0
        val notes = binding.notesEditText.text.toString()

        val motherHive = motherColoniesList.find { it.hiveNumber == motherHiveName }

        if (batchName.isBlank() || motherHive == null || cellCount <= 0) {
            // TODO: Show error message to user
            return
        }

        val newBatch = GraftingBatch(
            name = batchName,
            graftingDate = selectedGraftingDate,
            motherHiveId = motherHive.id,
            cellsGrafted = cellCount,
            notes = notes
        )

        viewModel.saveBatch(newBatch, cellCount)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
