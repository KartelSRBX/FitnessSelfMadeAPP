package com.kartel.fitness.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.kartel.fitness.R

enum class Destination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    PLANS("plans", R.string.nav_plans, Icons.Filled.Flag),
    WORK("work", R.string.nav_work, Icons.Filled.Checklist),
    SCHEDULE("schedule", R.string.nav_schedule, Icons.Filled.CalendarMonth),
    HEALTH("health", R.string.nav_health, Icons.Filled.Favorite),
    NUTRITION("nutrition", R.string.nav_nutrition, Icons.Filled.Restaurant),
    ANALYTICS("analytics", R.string.nav_analytics, Icons.Filled.Analytics),
}
