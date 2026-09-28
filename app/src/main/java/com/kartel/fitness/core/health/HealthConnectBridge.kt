package com.kartel.fitness.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Мост к Health Connect (roadmap, модуль "Health") — только чтение:
 * шаги, сон, пульс. Предварительное условие (проверяется пользователем вручную
 * один раз в настройках Samsung Health): синхронизация Samsung Health -> Health
 * Connect должна быть включена, иначе данных здесь не будет, даже если
 * разрешения выданы.
 *
 * Разрешения запрашиваются по каждому типу данных отдельно, как того требует roadmap.
 */
class HealthConnectBridge(private val context: Context) {

    companion object {
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
        )
    }

    enum class Availability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.UPDATE_REQUIRED
        else -> Availability.UNAVAILABLE
    }

    private fun client(): HealthConnectClient? =
        if (availability() == Availability.AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    suspend fun grantedPermissions(): Set<String> {
        val client = client() ?: return emptySet()
        return client.permissionController.getGrantedPermissions()
    }

    suspend fun hasAllPermissions(): Boolean = grantedPermissions().containsAll(PERMISSIONS)

    /** Суммарные шаги с начала текущих суток. */
    suspend fun readTodaySteps(): Long {
        val client = client() ?: return 0
        val startOfDay = java.time.LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val response: AggregationResult = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now()),
            )
        )
        return response[StepsRecord.COUNT_TOTAL] ?: 0
    }

    /** Длительность последней записанной сессии сна за последние 36 часов, если есть. */
    suspend fun readLastSleep(): Duration? {
        val client = client() ?: return null
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(Instant.now().minus(Duration.ofHours(36)), Instant.now()),
            )
        ).records
        val latest = records.maxByOrNull { it.endTime } ?: return null
        return Duration.between(latest.startTime, latest.endTime)
    }

    /** Шаги по дням за последние [days] дней, включая сегодня (для графика в Analytics). */
    suspend fun readStepsForLastDays(days: Int): List<Pair<LocalDate, Long>> {
        val client = client() ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startDate = today.minusDays((days - 1).toLong())
        return (0 until days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            date to (response[StepsRecord.COUNT_TOTAL] ?: 0)
        }
    }

    /** Последнее измерение пульса (уд/мин) за последние сутки, если есть. */
    suspend fun readLatestHeartRate(): Long? {
        val client = client() ?: return null
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(Instant.now().minus(Duration.ofHours(24)), Instant.now()),
            )
        ).records
        val lastSample = records
            .flatMap { it.samples }
            .maxByOrNull { it.time }
            ?: return null
        return lastSample.beatsPerMinute
    }
}
