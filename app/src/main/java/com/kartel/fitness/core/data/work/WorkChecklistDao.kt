package com.kartel.fitness.core.data.work

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkChecklistDao {
    @Query("SELECT * FROM work_checklist_items WHERE workTaskId = :taskId ORDER BY createdAt ASC")
    fun observeForTask(taskId: Long): Flow<List<WorkChecklistItem>>

    @Insert
    suspend fun insert(item: WorkChecklistItem)

    @Update
    suspend fun update(item: WorkChecklistItem)

    @Delete
    suspend fun delete(item: WorkChecklistItem)

    @Query("DELETE FROM work_checklist_items WHERE workTaskId = :taskId")
    suspend fun deleteAllForTask(taskId: Long)
}
