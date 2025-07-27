package com.stingsoftware.pasika.ui.queenrearing

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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

        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.queen_rearing)

        setupViewPager()
        setupMenu()

        binding.fab.setOnClickListener {
            val action = QueenRearingFragmentDirections.actionQueenRearingFragmentToAddEditGraftingBatchFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupViewPager() {
        val viewPager = binding.viewPager
        val tabLayout = binding.tabs

        viewPager.adapter = QueenRearingPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }.attach()
    }

    private fun getTabTitle(position: Int): String? {
        return when (position) {
            BATCHES_PAGE_INDEX -> getString(R.string.batches)
            TIMELINE_PAGE_INDEX -> getString(R.string.timeline)
            COLONIES_PAGE_INDEX -> getString(R.string.colonies)
            ANALYTICS_PAGE_INDEX -> getString(R.string.analytics)
            else -> null
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_search, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val currentFragment = childFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
                        if (currentFragment is SearchableFragment) {
                            currentFragment.search(newText)
                        }
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false // Let fragments handle their own menu items
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
