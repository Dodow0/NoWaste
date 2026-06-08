package com.nowaste.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nowaste.app.MainActivity
import com.nowaste.app.R
import com.nowaste.app.ServiceLocator
import com.nowaste.app.data.FoodItem
import com.nowaste.app.navigation.FoodDeepLinks
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ExpiryReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        ensureNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val today = LocalDate.now()
        val repository = ServiceLocator.foodRepository(applicationContext)
        val nearExpiryDays = ServiceLocator.appSettings(applicationContext).nearExpiryDays
        val items = repository.getItemsForReminderCheck(today, nearExpiryDays)
        items.forEach { item ->
            sendReminder(
                item = item,
                today = today,
                nearExpiryDays = item.reminderDaysBeforeExpiry ?: nearExpiryDays,
            )
        }
        return Result.success()
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.expiry_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = applicationContext.getString(R.string.expiry_reminder_channel_description)
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun sendReminder(item: FoodItem, today: LocalDate, nearExpiryDays: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val daysUntilExpiry = ChronoUnit.DAYS.between(today, item.expiryDate)
        val title = when {
            daysUntilExpiry in 1..nearExpiryDays.coerceAtLeast(1) -> applicationContext.resources.getQuantityString(
                R.plurals.notification_title_near_expiry,
                daysUntilExpiry.toInt(),
                daysUntilExpiry,
            )
            daysUntilExpiry == 0L -> applicationContext.getString(R.string.notification_title_today)
            daysUntilExpiry == -1L -> applicationContext.getString(R.string.notification_title_expired_yesterday)
            else -> return
        }
        val text = applicationContext.getString(
            R.string.notification_body,
            item.name,
            item.expiryDate.toString(),
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntentFor(item))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(item.id.toInt(), notification)
    }

    private fun contentIntentFor(item: FoodItem): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            FoodDeepLinks.foodItem(item.id).toUri(),
            applicationContext,
            MainActivity::class.java,
        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            applicationContext,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "expiry_reminders"
    }
}
