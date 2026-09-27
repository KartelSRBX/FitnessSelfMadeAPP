package com.kartel.fitness.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kartel.fitness.feature.analytics.AnalyticsScreen
import com.kartel.fitness.feature.health.HealthScreen
import com.kartel.fitness.feature.nutrition.NutritionScreen
import com.kartel.fitness.feature.plans.PlansScreen
import com.kartel.fitness.feature.schedule.ScheduleScreen
import com.kartel.fitness.feature.work.WorkScreen

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.PLANS.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable(Destination.PLANS.route) { PlansScreen() }
            composable(Destination.WORK.route) { WorkScreen() }
            composable(Destination.SCHEDULE.route) { ScheduleScreen() }
            composable(Destination.HEALTH.route) { HealthScreen() }
            composable(Destination.NUTRITION.route) { NutritionScreen() }
            composable(Destination.ANALYTICS.route) { AnalyticsScreen() }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}
