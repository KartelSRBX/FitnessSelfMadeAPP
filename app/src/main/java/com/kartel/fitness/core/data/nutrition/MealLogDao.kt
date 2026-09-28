package com.kartel.fitness.core.data.nutrition

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_log WHERE loggedAt >= :sinceMillis ORDER BY loggedAt DESC")
    fun observeSince(sinceMillis: Long): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_log ORDER BY loggedAt DESC")
    suspend fun getAll(): List<MealLogEntity>

    @Insert
    suspend fun insert(entry: MealLogEntity)

    @Delete
    suspend fun delete(entry: MealLogEntity)
}
