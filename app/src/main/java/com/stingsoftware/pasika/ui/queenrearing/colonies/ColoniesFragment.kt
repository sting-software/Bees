package com.stingsoftware.pasika.ui.queenrearing.colonies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.databinding.FragmentColoniesListBinding
import com.stingsoftware.pasika.ui.apiarydetail.HiveAdapter
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragmentDirections
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import com.stingsoftware.pasika.ui.queenrearing.SearchableFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColoniesFragment : Fragment(), SearchableFragment {

    private var _binding: FragmentColoniesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QueenRearingViewModel by viewModels({ requireParentFragment() })

    // State holders
    private var motherColonies: List<Hive> = emptyList()
    private var starterColonies: List<Hive> = emptyList()
    private var finisherColonies: List<Hive> = emptyList()
    private var nucleusColonies: List<Hive> = emptyList()
    private var anyBatchUsesStarter: Boolean = false

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
        setupObservers()
    }

    private fun setupRecyclerViews() {
        val onEditClicked: (Hive) -> Unit = { hive ->
            val action = QueenRearingFragmentDirections.actionQueenRearingFragmentToAddEditHiveFragment(
                hiveId = hive.id,
                apiaryId = hive.apiaryId,
                title = getString(R.string.title_edit_hive)
            )
            parentFragment?.findNavController()?.navigate(action)
        }

        binding.recyclerViewMother.apply { adapter = createAdapter(onEditClicked); layoutManager = LinearLayoutManager(context) }
        binding.recyclerViewStarter.apply { adapter = createAdapter(onEditClicked); layoutManager = LinearLayoutManager(context) }
        binding.recyclerViewFinisher.apply { adapter = createAdapter(onEditClicked); layoutManager = LinearLayoutManager(context) }
        binding.recyclerViewNucleus.apply { adapter = createAdapter(onEditClicked); layoutManager = LinearLayoutManager(context) }
    }

    private fun setupObservers() {
        val motherAdapter = binding.recyclerViewMother.adapter as HiveAdapter
        val starterAdapter = binding.recyclerViewStarter.adapter as HiveAdapter
        val finisherAdapter = binding.recyclerViewFinisher.adapter as HiveAdapter
        val nucleusAdapter = binding.recyclerViewNucleus.adapter as HiveAdapter

        viewModel.anyBatchUsesStarter.observe(viewLifecycleOwner) {
            anyBatchUsesStarter = it
            updateOverallVisibility()
        }

        viewModel.getMotherColonies().observe(viewLifecycleOwner) {
            motherColonies = it
            motherAdapter.submitList(it)
            updateOverallVisibility()
        }
        viewModel.getStarterColonies().observe(viewLifecycleOwner) {
            starterColonies = it
            starterAdapter.submitList(it)
            updateOverallVisibility()
        }
        viewModel.getFinisherColonies().observe(viewLifecycleOwner) {
            finisherColonies = it
            finisherAdapter.submitList(it)
            updateOverallVisibility()
        }
        viewModel.getNucleusColonies().observe(viewLifecycleOwner) {
            nucleusColonies = it
            nucleusAdapter.submitList(it)
            updateOverallVisibility()
        }
    }

    private fun createAdapter(onEditClicked: (Hive) -> Unit) = HiveAdapter(
        onItemClick = {},
        onEditClick = onEditClicked,
        onDeleteSwipe = {},
        onLongClick = {},
        onSelectionChange = {}
    )

    private fun updateOverallVisibility() {
        val showMothers = motherColonies.isNotEmpty()
        val showStarters = anyBatchUsesStarter && starterColonies.isNotEmpty()
        val showFinishers = finisherColonies.isNotEmpty()
        val showNuclei = nucleusColonies.isNotEmpty()

        binding.motherColoniesCard.visibility = if (showMothers) View.VISIBLE else View.GONE
        binding.starterColoniesCard.visibility = if (showStarters) View.VISIBLE else View.GONE
        binding.finisherColoniesCard.visibility = if (showFinishers) View.VISIBLE else View.GONE
        binding.nucleusColoniesCard.visibility = if (showNuclei) View.VISIBLE else View.GONE

        val isAnyCardVisible = showMothers || showStarters || showFinishers || showNuclei

        if (isAnyCardVisible) {
            binding.scrollViewColonies.visibility = View.VISIBLE
            binding.emptyState.root.visibility = View.GONE
        } else {
            binding.scrollViewColonies.visibility = View.GONE
            binding.emptyState.root.visibility = View.VISIBLE
            binding.emptyState.textViewEmptyMessage.text = getString(R.string.no_colonies_assigned)
        }
    }

    override fun search(query: String?) {
        viewModel.setSearchQuery(query)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
