package com.stingsoftware.pasika.ui.queenrearing.colonies

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.databinding.ItemHiveBinding
import com.stingsoftware.pasika.databinding.ItemTodoHeaderBinding
import androidx.core.content.ContextCompat

sealed class ColonyListItem {
    data class HeaderItem(val key: String, val title: String, val isExpanded: Boolean) : ColonyListItem()
    data class HiveItem(val hive: Hive) : ColonyListItem()
}

class ColoniesAdapter(
    private val onHeaderClick: (String) -> Unit,
    private val onHiveClick: (Hive) -> Unit
) : ListAdapter<ColonyListItem, RecyclerView.ViewHolder>(ColonyDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_HIVE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ColonyListItem.HeaderItem -> TYPE_HEADER
            is ColonyListItem.HiveItem -> TYPE_HIVE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemTodoHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            TYPE_HIVE -> HiveViewHolder(
                ItemHiveBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ColonyListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is ColonyListItem.HiveItem -> (holder as HiveViewHolder).bind(item.hive)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("PAYLOAD_EXPAND_COLLAPSE") && holder is HeaderViewHolder) {
            val item = getItem(position) as ColonyListItem.HeaderItem
            holder.animateExpandCollapse(item.isExpanded)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemTodoHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as ColonyListItem.HeaderItem
                    onHeaderClick(item.key)
                }
            }
        }

        fun bind(header: ColonyListItem.HeaderItem) {
            binding.headerTitle.text = header.title
            binding.expandIcon.visibility = View.VISIBLE
            binding.expandIcon.setImageResource(R.drawable.ic_expand_more)
            binding.expandIcon.rotation = if (header.isExpanded) 180f else 0f
        }

        fun animateExpandCollapse(isExpanded: Boolean) {
            val targetRotation = if (isExpanded) 180f else 0f
            ObjectAnimator.ofFloat(binding.expandIcon, "rotation", targetRotation).apply {
                duration = 300
                start()
            }
        }
    }

    inner class HiveViewHolder(private val binding: ItemHiveBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hive: Hive) {
            binding.textViewHiveNumber.text = hive.hiveNumber?.let {
                itemView.context.getString(R.string.hive_number_format, it)
            } ?: itemView.context.getString(R.string.hive_number_unknown)

            if (hive.hiveType.isNullOrBlank()) {
                binding.textViewHiveType.visibility = View.GONE
            } else {
                binding.textViewHiveType.visibility = View.VISIBLE
                binding.textViewHiveType.text =
                    itemView.context.getString(R.string.hive_type_format, hive.hiveType)
            }

            if (hive.frameType.isNullOrBlank()) {
                binding.textViewFrameType.visibility = View.GONE
            } else {
                binding.textViewFrameType.visibility = View.VISIBLE
                binding.textViewFrameType.text =
                    itemView.context.getString(R.string.frame_type_format, hive.frameType)
            }

            if (hive.breed.isNullOrBlank()) {
                binding.textViewBreed.visibility = View.GONE
            } else {
                binding.textViewBreed.visibility = View.VISIBLE
                binding.textViewBreed.text =
                    itemView.context.getString(R.string.breed_format, hive.breed)
            }

            binding.root.setOnClickListener { onHiveClick(hive) }
            binding.imageButtonEditHive.setOnClickListener { onHiveClick(hive) }

            binding.checkboxHiveSelect.visibility = View.GONE
        }
    }

    class ColonyDiffCallback : DiffUtil.ItemCallback<ColonyListItem>() {
        override fun areItemsTheSame(oldItem: ColonyListItem, newItem: ColonyListItem): Boolean {
            return when {
                oldItem is ColonyListItem.HeaderItem && newItem is ColonyListItem.HeaderItem -> oldItem.key == newItem.key
                oldItem is ColonyListItem.HiveItem && newItem is ColonyListItem.HiveItem -> oldItem.hive.id == newItem.hive.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ColonyListItem, newItem: ColonyListItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: ColonyListItem, newItem: ColonyListItem): Any? {
            if (oldItem is ColonyListItem.HeaderItem && newItem is ColonyListItem.HeaderItem) {
                if (oldItem.isExpanded != newItem.isExpanded) {
                    return "PAYLOAD_EXPAND_COLLAPSE"
                }
            }
            return null
        }
    }
}
