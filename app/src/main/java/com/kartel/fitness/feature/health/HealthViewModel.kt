package com.kartel.fitness.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.fitness.core.health.HealthConnectBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration

data class HealthUiState(
    val availability: HealthConnectBridge.Availability = HealthConnectBridge.Availability.UNAVAILABLE,
    val permissionsGranted: Boolean = false,
    val steps: Long? = null,
    val sleep: Duration? = null,
    val heartRate: Long? = null,
    val loading: Boolean = false,
)

class HealthViewModel(private val bridge: HealthConnectBridge) : ViewModel() {

    private val _state = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    val permissions: Set<String> = HealthConnectBridge.PERMISSIONS

    /** Проверяет доступность Health Connect и разрешения, и если всё есть — сразу читает данные. */
    fun checkStatusAndRefresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, availability = bridge.availability())
            val granted = bridge.hasAllPermissions()
            _state.value = _state.value.copy(permissionsGranted = granted)
            if (granted) refreshData() else _state.value = _state.value.copy(loading = false)
        }
    }

    private suspend fun refreshData() {
        val steps = bridge.readTodaySteps()
        val sleep = bridge.readLastSleep()
        val heartRate = bridge.readLatestHeartRate()
        _state.value = _state.value.copy(
            steps = steps,
            sleep = sleep,
            heartRate = heartRate,
            loading = false,
        )
    }
}
