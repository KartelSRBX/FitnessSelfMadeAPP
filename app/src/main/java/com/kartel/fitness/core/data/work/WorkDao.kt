package com.kartel.fitness.core.data.work

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * FROM work_tasks ORDER BY (deadlineMillis IS NULL), deadlineMillis, createdAt DESC")
    fun observeAll(): Flow<List<WorkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: WorkEntity): Long

    @Update
    suspend fun update(task: WorkEntity)

    @Delete
    suspend fun delete(task: WorkEntity)
}
