package com.firstapp.esehat

data class Medication(
    val id: Int,
    val name: String,
    val timing: String,
    val description: String,
    var status: MedStatus = MedStatus.PENDING,
    var reminderEnabled: Boolean = false,
    var reminderHour: Int = 21,
    var reminderMinute: Int = 0
)

enum class MedStatus {
    PENDING,
    TAKEN,
    SKIPPED
}