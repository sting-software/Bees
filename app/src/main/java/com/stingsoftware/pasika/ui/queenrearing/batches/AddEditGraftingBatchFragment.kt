package com.stingsoftware.pasika.ui.queenrearing.batches

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.CustomTask
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
    private lateinit var customTaskAdapter: CustomTaskAdapter

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
        setupCustomTasks()
        setupFieldListeners() // Add this call
        updateDateInView()

        viewModel.batch.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                currentBatch = it
                binding.batchNameEditText.setText(it.name)
                selectedGraftingDate = it.graftingDate
                binding.cellsGraftedEditText.setText(it.cellsGrafted.toString())
                binding.useStarterCheckBox.isChecked = it.useStarterColony
                binding.notesEditText.setText(it.notes)
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
            val hiveNames = hives.mapNotNull { it.hiveNumber }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, hiveNames)
            binding.motherHiveAutoComplete.setAdapter(adapter)

            currentBatch?.let { batch ->
                val motherHive = hives.find { it.id == batch.motherHiveId }
                motherHive?.let {
                    binding.motherHiveAutoComplete.setText(it.hiveNumber, false)
                }
            }
        }
    }

    private fun setupCustomTasks() {
        customTaskAdapter = CustomTaskAdapter { task ->
            viewModel.removeCustomTask(task)
        }
        binding.customTasksRecyclerView.apply {
            adapter = customTaskAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.customTasks.observe(viewLifecycleOwner) { tasks ->
            customTaskAdapter.submitList(tasks)
        }

        binding.addCustomTaskButton.setOnClickListener {
            showAddCustomTaskDialog()
        }
    }

    private fun setupFieldListeners() {
        binding.batchNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    binding.batchNameLayout.error = null
                }
            }
        })

        binding.motherHiveAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val motherHive = motherColoniesList.find { it.hiveNumber == s.toString() }
                if (motherHive != null) {
                    binding.motherHiveLayout.error = null
                }
            }
        })

        binding.cellsGraftedEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val cellCount = s.toString().toIntOrNull()
                if (cellCount != null && cellCount > 0) {
                    binding.cellsGraftedLayout.error = null
                }
            }
        })
    }

    private fun showAddCustomTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_custom_task, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.task_title_edit_text)
        val daysEditText = dialogView.findViewById<EditText>(R.id.task_days_edit_text)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_custom_task)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val title = titleEditText.text.toString().trim()
                val days = daysEditText.text.toString().toIntOrNull()
                if (title.isNotEmpty() && days != null) {
                    viewModel.addCustomTask(CustomTask(batchId = 0, title = title, daysAfterGrafting = days))
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun saveBatch() {
        val batchName = binding.batchNameEditText.text.toString().trim()
        val motherHiveName = binding.motherHiveAutoComplete.text.toString()
        val cellCountText = binding.cellsGraftedEditText.text.toString()
        val useStarter = binding.useStarterCheckBox.isChecked
        val notes = binding.notesEditText.text.toString()

        val motherHive = motherColoniesList.find { it.hiveNumber == motherHiveName }
        var isValid = true

        if (batchName.isBlank()) {
            binding.batchNameLayout.error = getString(R.string.error_field_cannot_be_empty, getString(R.string.batch_name))
            isValid = false
        } else {
            binding.batchNameLayout.error = null
        }

        if (motherHive == null) {
            binding.motherHiveLayout.error = getString(R.string.select_mother)
            isValid = false
        } else {
            binding.motherHiveLayout.error = null
        }

        val cellCount = cellCountText.toIntOrNull()
        if (cellCount == null || cellCount <= 0) {
            binding.cellsGraftedLayout.error = getString(R.string.error_must_be_positive_number,
                getString(R.string.number_of_cells)
            )
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
            useStarterColony = useStarter,
            notes = notes
        ) ?: GraftingBatch(
            name = batchName,
            graftingDate = selectedGraftingDate,
            motherHiveId = motherHive!!.id,
            cellsGrafted = cellCount!!,
            useStarterColony = useStarter,
            notes = notes
        )

        viewModel.saveBatch(batchToSave, viewModel.customTasks.value ?: emptyList())
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
