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
    const val CHANNEL_PARTNER_DRAWING = "lockdraw_partner_drawing"
    private const val NOTIFICATION_ID_PAIRING = 2001
    private const val NOTIFICATION_ID_DRAWING = 2002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val pairingChannel = NotificationChannel(
                CHANNEL_PARTNER_PAIRING,
                "Connessione Partner",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche quando il partner si collega alla stanza condivisa"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(pairingChannel)

            val drawingChannel = NotificationChannel(
                CHANNEL_PARTNER_DRAWING,
                "Disegni & Sticker del Partner",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avvisi in tempo reale quando il partner disegna sulla tela condivisa"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(drawingChannel)
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

    fun showPartnerDrawingNotification(
        context: Context,
        partnerName: String,
        roomCode: String,
        detailText: String = "ha appena disegnato qualcosa per te! 🎨"
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
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DRAWING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayName = partnerName.ifBlank { "Il tuo partner" }
        val title = "🎨 Nuovo disegno da $displayName"
        val content = "$displayName $detailText"

        val notification = NotificationCompat.Builder(context, CHANNEL_PARTNER_DRAWING)
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
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DRAWING, notification)
        } catch (e: SecurityException) {
            // Permission not granted or restricted
        } catch (e: Exception) {
            // Non-fatal notification error
        }
    }
}
