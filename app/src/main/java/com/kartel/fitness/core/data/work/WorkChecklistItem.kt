package com.kartel.fitness.core.data.work

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Пункт чеклиста внутри задачи Work — подзадачи для более крупных задач. */
@Entity(tableName = "work_checklist_items")
data class WorkChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workTaskId: Long,
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
