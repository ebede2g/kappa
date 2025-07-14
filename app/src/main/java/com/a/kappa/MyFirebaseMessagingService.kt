package com.a.kappa

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private fun confirmDelivery(type: String, files: List<String>) {
        val url = "http//:5.58.30.179:25656" // Замінити на справжню IP-адресу
        val json = JSONObject().apply {
            put("type", type)
            put("files", JSONArray(files))
        }

        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true
                conn.outputStream.use { it.write(json.toString().toByteArray()) }
                Log.d("TASK", "Підтвердження доставки: $type -> $files")
            } catch (e: Exception) {
                Log.e("TASK", "Помилка підтвердження: ${e.message}")
            }
        }.start()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data["type"]?.let { type ->
            val filesJson = remoteMessage.data["files"] ?: return
            try {
                val filesArray = JSONArray(filesJson)
                val listOfIcsNames = mutableListOf<String>()
                val fullPaths = mutableListOf<String>()

                for (i in 0 until filesArray.length()) {
                    val fullPath = filesArray.getString(i)
                    val fileName = fullPath.substringAfterLast("/")
                    listOfIcsNames.add(fileName)
                    fullPaths.add(fullPath)
                }

                when (type) {
                    "toCreate" -> {
                        Log.d("TASK", "Отримано запит на створення: $listOfIcsNames")
                        TaskUtil.getTaskFromServer(this, listOfIcsNames)
                    }
                    "toRemove" -> {
                        Log.d("TASK", "Отримано запит на видалення: $listOfIcsNames")
                        TaskUtil.removeLocalTasks(this, listOfIcsNames)
                    }
                    else -> {
                        Log.w("TASK", "Невідомий тип дії: $type")
                        return
                    }
                }

                confirmDelivery(type, fullPaths)

            } catch (e: Exception) {
                Log.e("TASK", "Помилка обробки повідомлення: ${e.message}")
            }
        }
    }
}
