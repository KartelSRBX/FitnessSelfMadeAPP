package com.kartel.fitness.core.data.plans

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: PlanEntity): Long

    @Update
    suspend fun update(plan: PlanEntity)

    @Delete
    suspend fun delete(plan: PlanEntity)
}
