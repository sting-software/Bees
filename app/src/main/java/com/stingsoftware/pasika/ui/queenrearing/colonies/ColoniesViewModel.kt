package com.stingsoftware.pasika.ui.queenrearing.colonies

import android.content.Context
import androidx.lifecycle.*
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.data.HiveRole
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class ColoniesViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _headerExpandedState = MutableStateFlow(mapOf(
        "mother" to true,
        "starter" to true,
        "finisher" to true,
        "nucleus" to true
    ))

    val coloniesList: LiveData<List<ColonyListItem>> = combine(
        repository.getHivesByRole(HiveRole.MOTHER),
        repository.getHivesByRole(HiveRole.STARTER),
        repository.getHivesByRole(HiveRole.FINISHER),
        repository.getHivesByRole(HiveRole.NUCLEUS),
        _headerExpandedState
    ) { mother, starter, finisher, nucleus, expandedState ->
        val items = mutableListOf<ColonyListItem>()

        if (mother.isNotEmpty()) {
            val isExpanded = expandedState["mother"] ?: true
            items.add(ColonyListItem.HeaderItem("mother", context.getString(R.string.mother_colonies), isExpanded))
            if (isExpanded) {
                items.addAll(mother.map { ColonyListItem.HiveItem(it) })
            }
        }
        if (starter.isNotEmpty()) {
            val isExpanded = expandedState["starter"] ?: true
            items.add(ColonyListItem.HeaderItem("starter", context.getString(R.string.starter_colonies), isExpanded))
            if (isExpanded) {
                items.addAll(starter.map { ColonyListItem.HiveItem(it) })
            }
        }
        if (finisher.isNotEmpty()) {
            val isExpanded = expandedState["finisher"] ?: true
            items.add(ColonyListItem.HeaderItem("finisher", context.getString(R.string.finisher_colonies), isExpanded))
            if (isExpanded) {
                items.addAll(finisher.map { ColonyListItem.HiveItem(it) })
            }
        }
        if (nucleus.isNotEmpty()) {
            val isExpanded = expandedState["nucleus"] ?: true
            items.add(ColonyListItem.HeaderItem("nucleus", context.getString(R.string.nucleus_colonies), isExpanded))
            if (isExpanded) {
                items.addAll(nucleus.map { ColonyListItem.HiveItem(it) })
            }
        }
        items
    }.asLiveData()

    fun toggleHeaderExpanded(key: String) {
        val currentMap = _headerExpandedState.value.toMutableMap()
        currentMap[key] = !(currentMap[key] ?: true)
        _headerExpandedState.value = currentMap
    }
}
