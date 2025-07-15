package com.stingsoftware.pasika.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.ApiaryType
import com.stingsoftware.pasika.databinding.FragmentBulkEditApiaryBinding
import com.stingsoftware.pasika.viewmodel.BulkEditApiaryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class BulkEditApiaryFragment : Fragment() {

    private val viewModel: BulkEditApiaryViewModel by viewModels()

    private var _binding: FragmentBulkEditApiaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBulkEditApiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.title_bulk_edit_apiaries)

        setupDropdown()

        viewModel.selectedApiaries.observe(viewLifecycleOwner) { apiaries ->
            binding.textViewSelectedApiariesCount.text = getString(R.string.bulk_edit_apiaries_count, apiaries.size)
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(),
                    getString(R.string.message_apiaries_updated), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.error_update_apiaries_failed), Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonSaveBulkEdit.setOnClickListener {
            val location = binding.editTextApiaryLocation.text.toString().trim().ifEmpty { null }
            val typeString = binding.autoCompleteTextViewApiaryType.text.toString().uppercase(Locale.getDefault()).ifEmpty { null }
            val type = typeString?.let { runCatching { ApiaryType.valueOf(it) }.getOrNull() }
            val notes = binding.editTextNotes.text.toString().trim().ifEmpty { null }

            if (location == null && type == null && notes == null) {
                Toast.makeText(requireContext(),
                    getString(R.string.error_bulk_edit_apiaries_at_least_one_field), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.updateApiaries(location, type, notes)
        }

        binding.buttonCancelBulkEdit.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupDropdown() {
        val apiaryTypes = ApiaryType.entries.map { it.name.lowercase(Locale.getDefault()).replaceFirstChar { char -> char.titlecase(Locale.getDefault()) } }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, apiaryTypes)
        binding.autoCompleteTextViewApiaryType.setAdapter(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}