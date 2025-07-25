package com.stingsoftware.pasika.ui.queenrearing.colonies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.databinding.FragmentColoniesListBinding
import com.stingsoftware.pasika.ui.apiarydetail.HiveAdapter
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColoniesFragment : Fragment() {

    private var _binding: FragmentColoniesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QueenRearingViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColoniesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
    }

    private fun setupRecyclerViews() {
        val motherAdapter = HiveAdapter(
            onItemClick = {},
            onEditClick = {},
            onDeleteSwipe = {},
            onLongClick = {},
            onSelectionChange = {}
        )
        val starterAdapter = HiveAdapter(
            onItemClick = {},
            onEditClick = {},
            onDeleteSwipe = {},
            onLongClick = {},
            onSelectionChange = {}
        )
        val finisherAdapter = HiveAdapter(
            onItemClick = {},
            onEditClick = {},
            onDeleteSwipe = {},
            onLongClick = {},
            onSelectionChange = {}
        )
        val nucleusAdapter = HiveAdapter(
            onItemClick = {},
            onEditClick = {},
            onDeleteSwipe = {},
            onLongClick = {},
            onSelectionChange = {}
        )

        binding.recyclerViewMother.apply {
            adapter = motherAdapter; layoutManager = LinearLayoutManager(context)
        }
        binding.recyclerViewStarter.apply {
            adapter = starterAdapter; layoutManager = LinearLayoutManager(context)
        }
        binding.recyclerViewFinisher.apply {
            adapter = finisherAdapter; layoutManager = LinearLayoutManager(context)
        }
        binding.recyclerViewNucleus.apply {
            adapter = nucleusAdapter; layoutManager = LinearLayoutManager(context)
        }

        viewModel.getMotherColonies().observe(viewLifecycleOwner) { motherAdapter.submitList(it) }
        viewModel.getStarterColonies().observe(viewLifecycleOwner) { starterAdapter.submitList(it) }
        viewModel.getFinisherColonies()
            .observe(viewLifecycleOwner) { finisherAdapter.submitList(it) }
        viewModel.getNucleusColonies().observe(viewLifecycleOwner) { nucleusAdapter.submitList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
