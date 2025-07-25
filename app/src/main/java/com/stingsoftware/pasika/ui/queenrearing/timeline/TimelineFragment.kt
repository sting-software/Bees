package com.stingsoftware.pasika.ui.queenrearing.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.databinding.FragmentTimelineBinding
import com.stingsoftware.pasika.todo.TodoAdapter
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QueenRearingViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val todoAdapter = TodoAdapter(
            onTaskClicked = {},
            onTaskChecked = { _, _ -> },
            onLongClick = {},
            onSelectionChange = {}
        )

        binding.recyclerViewTimeline.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.queenRearingTasks.observe(viewLifecycleOwner) { tasks ->
            todoAdapter.submitList(tasks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
