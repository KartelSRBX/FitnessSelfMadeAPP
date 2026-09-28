package com.kartel.fitness

import android.app.Application
import androidx.room.Room
import com.kartel.fitness.core.data.AppDatabase

class FitnessApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "fitness.db").build()
    }
}
