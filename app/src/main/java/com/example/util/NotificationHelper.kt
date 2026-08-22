package com.example.util

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
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_PARTNER_PAIRING = "lockdraw_partner_pairing"
    private const val NOTIFICATION_ID_PAIRING = 2001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifiche Partner"
            val descriptionText = "Notifiche quando il partner si collega o interagisce con la stanza"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_PARTNER_PAIRING, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showPartnerConnectedNotification(
        context: Context,
        partnerName: String,
        roomCode: String
    ) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ROOM_CODE", roomCode)
            putExtra("EXTRA_PARTNER_CONNECTED", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_PAIRING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayName = partnerName.ifBlank { "Il tuo partner" }
        val title = "❤️ Partner Connesso!"
        val content = "$displayName si è collegato alla stanza ($roomCode). Ora i vostri schermi sono abbinati in tempo reale!"

        val notification = NotificationCompat.Builder(context, CHANNEL_PARTNER_PAIRING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PAIRING, notification)
        } catch (e: SecurityException) {
            // Permission not granted or restricted
        } catch (e: Exception) {
            // Non-fatal notification error
        }
    }
}
