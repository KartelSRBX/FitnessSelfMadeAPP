package com.kartel.fitness.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kartel.fitness.feature.ai.AiTestScreen
import com.kartel.fitness.feature.analytics.AnalyticsScreen
import com.kartel.fitness.feature.health.HealthScreen
import com.kartel.fitness.feature.nutrition.NutritionScreen
import com.kartel.fitness.feature.plans.PlansScreen
import com.kartel.fitness.feature.schedule.ScheduleScreen
import com.kartel.fitness.feature.timer.TimerScreen
import com.kartel.fitness.feature.work.WorkScreen

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.PLANS.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.PLANS.route) { PlansScreen() }
            composable(Destination.WORK.route) { WorkScreen() }
            composable(Destination.SCHEDULE.route) { ScheduleScreen() }
            composable(Destination.HEALTH.route) { HealthScreen() }
            composable(Destination.NUTRITION.route) { NutritionScreen() }
            composable(Destination.ANALYTICS.route) { AnalyticsScreen() }
            composable(Destination.AI.route) { AiTestScreen() }
            composable(Destination.TIMER.route) { TimerScreen() }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 7 вкладок тесно помещаются в одну строку — подпись показываем только у выбранной
    // вкладки (адаптивно освобождает место остальным), и всегда в одну строку без переноса.
    NavigationBar {
        Destination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                alwaysShowLabel = false,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                label = {
                    Text(
                        stringResource(destination.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    )
                },
            )
        }
    }
}
