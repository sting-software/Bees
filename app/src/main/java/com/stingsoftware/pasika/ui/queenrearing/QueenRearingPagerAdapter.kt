package com.stingsoftware.pasika.ui.queenrearing

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.ui.queenrearing.analytics.AnalyticsFragment
import com.stingsoftware.pasika.ui.queenrearing.batches.BatchesFragment
import com.stingsoftware.pasika.ui.queenrearing.colonies.ColoniesFragment
import com.stingsoftware.pasika.ui.queenrearing.timeline.TimelineFragment

const val BATCHES_PAGE_INDEX = 0
const val TIMELINE_PAGE_INDEX = 1
const val COLONIES_PAGE_INDEX = 2
const val ANALYTICS_PAGE_INDEX = 3

/**
 * An adapter that provides the fragments for the ViewPager in the Queen Rearing section.
 */
class QueenRearingPagerAdapter(private val fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**
     * A map of the ViewPager page indexes to their respective Fragment creators.
     * This makes it easy to add, remove, or reorder tabs.
     */
    private val tabFragmentsCreators: Map<Int, () -> Fragment> = mapOf(
        BATCHES_PAGE_INDEX to { BatchesFragment() },
        TIMELINE_PAGE_INDEX to { TimelineFragment() },
        COLONIES_PAGE_INDEX to { ColoniesFragment() },
        ANALYTICS_PAGE_INDEX to { AnalyticsFragment() }
    )

    override fun getItemCount() = tabFragmentsCreators.size

    override fun createFragment(position: Int): Fragment {
        return tabFragmentsCreators[position]?.invoke()
            ?: throw IndexOutOfBoundsException(
                fragment.requireContext().getString(
                    R.string.invalid_position,
                    position
                )
            )
    }
}
