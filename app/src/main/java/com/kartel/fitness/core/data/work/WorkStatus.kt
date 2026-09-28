package com.kartel.fitness.core.data.work

enum class WorkStatus {
    TODO, IN_PROGRESS, DONE;

    val label: String
        get() = when (this) {
            TODO -> "К выполнению"
            IN_PROGRESS -> "В работе"
            DONE -> "Сделано"
        }

    fun next(): WorkStatus = when (this) {
        TODO -> IN_PROGRESS
        IN_PROGRESS -> DONE
        DONE -> TODO
    }
}
