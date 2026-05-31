package com.firstapp.esehat

data class Medication(
    val id: Int,
    val name: String,
    val timing: String,
    val description: String,
    var status: MedStatus = MedStatus.PENDING
)

enum class MedStatus {
    PENDING, TAKEN, SKIPPED
}