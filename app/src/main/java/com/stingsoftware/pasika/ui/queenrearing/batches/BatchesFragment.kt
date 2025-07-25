package com.stingsoftware.pasika.ui.queenrearing.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.databinding.FragmentBatchesListBinding
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BatchesFragment : Fragment() {

    private var _binding: FragmentBatchesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QueenRearingViewModel by viewModels({requireParentFragment()})

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val batchesAdapter = GraftingBatchAdapter()
        binding.recyclerViewBatches.apply {
            adapter = batchesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.graftingBatches.observe(viewLifecycleOwner) { batches ->
            batchesAdapter.submitList(batches)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}