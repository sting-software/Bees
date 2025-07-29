package com.stingsoftware.pasika.ui.queenrearing.colonies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.databinding.FragmentColoniesListBinding
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragmentDirections
import com.stingsoftware.pasika.ui.queenrearing.SearchableFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColoniesFragment : Fragment(), SearchableFragment {

    private var _binding: FragmentColoniesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ColoniesViewModel by viewModels()
    private lateinit var coloniesAdapter: ColoniesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColoniesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        coloniesAdapter = ColoniesAdapter(
            onHeaderClick = { key ->
                viewModel.toggleHeaderExpanded(key)
            },
            onHiveClick = { hive ->
                val action = QueenRearingFragmentDirections.actionQueenRearingFragmentToAddEditHiveFragment(
                    hiveId = hive.id,
                    apiaryId = hive.apiaryId,
                    title = getString(R.string.title_edit_hive)
                )
                parentFragment?.findNavController()?.navigate(action)
            }
        )

        binding.recyclerViewColonies.apply {
            adapter = coloniesAdapter
            layoutManager = LinearLayoutManager(context)
            (itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun setupObservers() {
        viewModel.coloniesList.observe(viewLifecycleOwner) { list ->
            coloniesAdapter.submitList(list)
            val isTrulyEmpty = list.isEmpty()
            binding.recyclerViewColonies.visibility = if (isTrulyEmpty) View.GONE else View.VISIBLE
            binding.emptyState.root.visibility = if (isTrulyEmpty) View.VISIBLE else View.GONE
            if (isTrulyEmpty) {
                binding.emptyState.textViewEmptyMessage.text = getString(R.string.no_colonies_assigned)
            }
        }
    }

    override fun search(query: String?) {
        // This functionality would need to be implemented in the new ColoniesViewModel if desired.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
