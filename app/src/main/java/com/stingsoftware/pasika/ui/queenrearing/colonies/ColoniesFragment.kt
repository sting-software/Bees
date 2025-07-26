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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColoniesFragment : Fragment() {

    private var _binding: FragmentColoniesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QueenRearingViewModel by viewModels({ requireParentFragment() })

    // Properties to hold the lists to check for the empty state
    private var motherColonies: List<Hive>? = null
    private var starterColonies: List<Hive>? = null
    private var finisherColonies: List<Hive>? = null
    private var nucleusColonies: List<Hive>? = null

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
        val onEditClicked: (Hive) -> Unit = { hive ->
            val action = QueenRearingFragmentDirections.actionQueenRearingFragmentToAddEditHiveFragment(
                hiveId = hive.id,
                apiaryId = hive.apiaryId,
                title = getString(R.string.title_edit_hive)
            )
            parentFragment?.findNavController()?.navigate(action)
        }

        val motherAdapter = createAdapter(onEditClicked)
        val starterAdapter = createAdapter(onEditClicked)
        val finisherAdapter = createAdapter(onEditClicked)
        val nucleusAdapter = createAdapter(onEditClicked)

        binding.recyclerViewMother.apply { adapter = motherAdapter; layoutManager = LinearLayoutManager(context) }
        binding.recyclerViewStarter.apply { adapter = starterAdapter; layoutManager = LinearLayoutManager(context) }
        binding.recyclerViewFinisher.apply { adapter = finisherAdapter; layoutManager = LinearLayoutManager(context) }
        binding.recyclerViewNucleus.apply { adapter = nucleusAdapter; layoutManager = LinearLayoutManager(context) }

        viewModel.getMotherColonies().observe(viewLifecycleOwner) { motherAdapter.submitList(it); motherColonies = it; updateEmptyStateVisibility() }
        viewModel.getStarterColonies().observe(viewLifecycleOwner) { starterAdapter.submitList(it); starterColonies = it; updateEmptyStateVisibility() }
        viewModel.getFinisherColonies().observe(viewLifecycleOwner) { finisherAdapter.submitList(it); finisherColonies = it; updateEmptyStateVisibility() }
        viewModel.getNucleusColonies().observe(viewLifecycleOwner) { nucleusAdapter.submitList(it); nucleusColonies = it; updateEmptyStateVisibility() }
    }

    private fun createAdapter(onEditClicked: (Hive) -> Unit) = HiveAdapter(
        onItemClick = {},
        onEditClick = onEditClicked,
        onDeleteSwipe = {},
        onLongClick = {},
        onSelectionChange = {}
    )
    private fun updateEmptyStateVisibility() {
        // Return if not all lists have been initialized yet
        if (motherColonies == null || starterColonies == null || finisherColonies == null || nucleusColonies == null) return

        val areAllEmpty = motherColonies!!.isEmpty() && starterColonies!!.isEmpty() && finisherColonies!!.isEmpty() && nucleusColonies!!.isEmpty()

        if (areAllEmpty) {
            binding.scrollViewColonies.visibility = View.GONE
            binding.emptyState.root.visibility = View.VISIBLE
            binding.emptyState.textViewEmptyMessage.text =
                getString(R.string.no_colonies_assigned)
        } else {
            binding.scrollViewColonies.visibility = View.VISIBLE
            binding.emptyState.root.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
