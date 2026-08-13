package com.firstapp.esehat

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object MedicationReminderScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(
        context: Context,
        medication: Medication
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val calendar =
            Calendar.getInstance()

        calendar.set(
            Calendar.HOUR_OF_DAY,
            medication.reminderHour
        )

        calendar.set(
            Calendar.MINUTE,
            medication.reminderMinute
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        if (
            calendar.timeInMillis <=
            System.currentTimeMillis()
        ) {

            calendar.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        val intent =
            Intent(
                context,
                MedicationReminderReceiver::class.java
            ).apply {

                putExtra(
                    "medicationId",
                    medication.id
                )

                putExtra(
                    "medicationName",
                    medication.name
                )

                putExtra(
                    "description",
                    medication.description
                )
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                medication.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancel(
        context: Context,
        medicationId: Int
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val intent =
            Intent(
                context,
                MedicationReminderReceiver::class.java
            )

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                medicationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            pendingIntent
        )
    }
}