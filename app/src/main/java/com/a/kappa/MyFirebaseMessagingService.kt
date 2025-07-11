package com.a.kappa

// MyFirebaseMessagingService.kt

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data["type"] == "toCreate") {
            val filesJson = remoteMessage.data["files"]
            try {
                val filesArray = JSONArray(filesJson)
                val listOfIcsNames = mutableListOf<String>()
                for (i in 0 until filesArray.length()) {
                    val fullPath = filesArray.getString(i)
                    val fileName = fullPath.substringAfterLast("/")
                    Log.d("TASK", "Створити файл: $fileName")
                    listOfIcsNames.add(fileName)
                }
                TaskUtil.getTaskFromServer(this,listOfIcsNames)
            } catch (e: Exception) {
                Log.e("TASK", "Помилка при додаванні файлів: ${e.message}")
            }
        }


        if (remoteMessage.data["type"] == "toRemove") {
            val filesJson = remoteMessage.data["files"]
            try {
                val filesArray = JSONArray(filesJson)
                val listOfIcsNames = mutableListOf<String>()
                for (i in 0 until filesArray.length()) {
                    val fullPath = filesArray.getString(i)
                    val fileName = fullPath.substringAfterLast("/")
                    Log.d("TASK", "Видалити файл: $fileName")
                    listOfIcsNames.add(fileName)
                }
                TaskUtil.removeLocalTasks(this,listOfIcsNames)
            } catch (e: Exception) {
                Log.e("TASK", "Помилка при видаленні файлів: ${e.message}")
            }
        }










    }
}

