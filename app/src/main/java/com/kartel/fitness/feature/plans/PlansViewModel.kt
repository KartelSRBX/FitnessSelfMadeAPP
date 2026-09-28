package com.kartel.fitness.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.data.plans.PlanDao
import com.kartel.fitness.core.data.plans.PlanEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlansViewModel(private val dao: PlanDao) : ViewModel() {

    val plans: StateFlow<List<PlanEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPlan(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.upsert(PlanEntity(title = title.trim(), description = description.trim()))
        }
    }

    fun updateProgress(plan: PlanEntity, progressPercent: Int, note: String) {
        viewModelScope.launch {
            dao.update(
                plan.copy(
                    progressPercent = progressPercent.coerceIn(0, 100),
                    lastProgressNote = note.trim(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deletePlan(plan: PlanEntity) {
        viewModelScope.launch { dao.delete(plan) }
    }
}
