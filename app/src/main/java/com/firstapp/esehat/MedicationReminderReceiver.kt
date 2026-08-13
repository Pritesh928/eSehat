package com.firstapp.esehat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class MedicationReminderReceiver :
    BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val medicationId =
            intent.getIntExtra(
                "medicationId",
                -1
            )

        val medicationName =
            intent.getStringExtra(
                "medicationName"
            ) ?: "Medication"

        val description =
            intent.getStringExtra(
                "description"
            ) ?: "Time to take your medication"

        createChannel(context)

        playSound(context)

        val takeIntent =
            Intent(
                context,
                MedicationActionReciever::class.java
            ).apply {

                action =
                    "ACTION_TAKE"

                putExtra(
                    "medicationId",
                    medicationId
                )
            }

        val skipIntent =
            Intent(
                context,
                MedicationActionReciever::class.java
            ).apply {

                action =
                    "ACTION_SKIP"

                putExtra(
                    "medicationId",
                    medicationId
                )
            }

        val takePending =
            PendingIntent.getBroadcast(
                context,
                medicationId * 2 + 1,
                takeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val skipPending =
            PendingIntent.getBroadcast(
                context,
                medicationId * 2 + 2,
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val openIntent =
            Intent(
                context,
                MainActivity::class.java
            )

        val openPending =
            PendingIntent.getActivity(
                context,
                medicationId + 1000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                context,
                "MEDICATION_REMINDERS"
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    "💊 $medicationName"
                )
                .setContentText(
                    description
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "It's time to take $medicationName"
                        )
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setAutoCancel(false)
                .setContentIntent(
                    openPending
                )
                .addAction(
                    0,
                    "Skip",
                    skipPending
                )
                .addAction(
                    0,
                    "Take",
                    takePending
                )
                .build()

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            medicationId,
            notification
        )

        MedicationReminderScheduler.schedule(
            context,
            MedicationStorage.load(context)
                .firstOrNull {
                    it.id == medicationId
                } ?: return
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(
        context: Context
    ) {

        val channel =
            NotificationChannel(
                "MEDICATION_REMINDERS",
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    "Medication reminder notifications"

                enableVibration(true)
            }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(
            channel
        )
    }

    private fun playSound(
        context: Context
    ) {

        try {

            val player =
                MediaPlayer.create(
                    context,
                    R.raw.medicine_reminder
                )

            player?.setOnCompletionListener {
                it.release()
            }

            player?.start()

        } catch (_: Exception) {
        }
    }
}