package com.kartel.fitness.core.data.timer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY endMillis ASC")
    fun observeAll(): Flow<List<TimerEntity>>

    @Insert
    suspend fun insert(timer: TimerEntity): Long

    @Delete
    suspend fun delete(timer: TimerEntity)
}
