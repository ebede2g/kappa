package com.a.kappa

// MyFirebaseMessagingService.kt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data["type"] == "new_files") {
            val filesJson = remoteMessage.data["files"]
            try {
                val filesArray = JSONArray(filesJson)
                val listOfIcsNames = mutableListOf<String>()
                for (i in 0 until filesArray.length()) {
                    val fileName = filesArray.getString(i)
                    Log.d("TASK", "Новий файл: $fileName")
                    listOfIcsNames.add(fileName)
                }
                TaskUtil.getTaskFromServer(this,listOfIcsNames)
            } catch (e: Exception) {
                Log.e("TASK", "Помилка при обробці файлів: ${e.message}")
            }
        }
    }
}

