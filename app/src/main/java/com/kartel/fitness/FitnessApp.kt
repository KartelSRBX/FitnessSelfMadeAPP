package com.kartel.fitness

import android.app.Application
import androidx.room.Room
import com.kartel.fitness.core.data.AppDatabase

class FitnessApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "fitness.db")
            // TODO: заменить на настоящие Migration-объекты перед первым релизом —
            // пока приложение в разработке, схема меняется часто, и данные не жалко.
            .fallbackToDestructiveMigration()
            .build()
    }
}
