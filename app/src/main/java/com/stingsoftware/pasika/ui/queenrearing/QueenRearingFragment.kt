package com.stingsoftware.pasika.ui.queenrearing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.databinding.FragmentQueenRearingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QueenRearingFragment : Fragment() {

    private var _binding: FragmentQueenRearingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQueenRearingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = binding.viewPager
        val tabLayout = binding.tabs

        // Set up the ViewPager with the PagerAdapter
        viewPager.adapter = QueenRearingPagerAdapter(this)

        // Attach the TabLayout to the ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }.attach()

        // Set up the FloatingActionButton to navigate to the Add/Edit Batch screen
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_queenRearingFragment_to_addEditGraftingBatchFragment)
        }
    }

    /**
     * Returns the title for each tab based on its position.
     */
    private fun getTabTitle(position: Int): String? {
        return when (position) {
            BATCHES_PAGE_INDEX -> getString(R.string.batches)
            TIMELINE_PAGE_INDEX -> getString(R.string.timeline)
            COLONIES_PAGE_INDEX -> getString(R.string.colonies)
            ANALYTICS_PAGE_INDEX -> getString(R.string.analytics)
            else -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
