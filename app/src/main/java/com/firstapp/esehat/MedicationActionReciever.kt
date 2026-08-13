package com.firstapp.esehat

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MedicationActionReciever :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val medicationId =
            intent.getIntExtra(
                "medicationId",
                -1
            )

        if (medicationId == -1) {
            return
        }

        val status =
            when (intent.action) {

                "ACTION_TAKE" ->
                    MedStatus.TAKEN

                "ACTION_SKIP" ->
                    MedStatus.SKIPPED

                else ->
                    return
            }

        MedicationStorage.updateStatus(
            context,
            medicationId,
            status
        )

        MedicationReminderScheduler.cancel(
            context,
            medicationId
        )

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.cancel(
            medicationId
        )
    }
}