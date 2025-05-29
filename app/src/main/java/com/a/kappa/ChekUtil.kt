package com.a.kappa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

object ChekUtil {


    fun hasPermissionToCalendar(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }



    fun isServerOnline(onResult: (Boolean) -> Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("http://5.58.30.179:5000/ping")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Мінімізуємо лог - лише коротке повідомлення
                Log.w("SERVER_CHECK", "Сервер не відповідає: ${e.message}")
                onResult(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val isOnline = response.isSuccessful && response.code in 200..299
                    Log.d("SERVER_CHECK", "Сервер онлайн: $isOnline (HTTP код: ${response.code})")
                    onResult(isOnline)
                }
            }
        })
    }



    fun isObserverOnline(ctx: Context, callback: (Boolean) -> Unit) {
        if (!hasPermissionToCalendar(ctx)) {
            Log.d("TASK", "ERR  >> Дозвіл на календар не надано")
            callback(false)
        } else {
            Log.d("TASK", "gut  >> Дозвіл на календар Є")
            if (UserPrefs.getIs_offline()) {
                Log.d("TASK", "DONE >> Режим Офлайн")
                callback(false)
            } else {
                Log.d("TASK", "gut  >> Режим Онлайн")
                if (UserPrefs.getUID() == "") {
                    Log.d("TASK", "ERR  >> UID порожнє, отже не ініціалізовано онлайн календар (вхід не виконано)")
                    callback(false)
                } else {
                    Log.d("TASK", "gut  >> UID ініціалізовано")
                    if (UserPrefs.getToken() == "") {
                        Log.d("TASK", "ERR  >> Токен не отримано, отже сервер офлайн")
                        callback(false)
                    } else {
                        Log.d("TASK", "gut  >> Токен присутній")
                        isServerOnline { isOnline ->
                            if (!isOnline) {
                                Log.d("TASK", "DONE >> Сервер не досяжний / офлайн. Але записано в локальний календар. Режим Офлайн")
                                callback(false)
                            } else {
                                Log.d("TASK", "gut  >> Сервер в досяжності")
                                Log.d("TASK", "DONE >> Режим Онлайн")
                                callback(true)
                            }
                        }
                    }
                }
            }
        }
    }











    @RequiresApi(Build.VERSION_CODES.O)
    fun isCalDAVCredentialsValid(caldavUrl: String, userName: String, userPwd: String): Boolean {
        if (caldavUrl.isBlank() || userName.isBlank() || userPwd.isBlank()) {
            return false
        }

        val client = OkHttpClient()
        val xmlBody = """
        <?xml version="1.0" encoding="UTF-8"?>
        <d:propfind xmlns:d="DAV:">
          <d:prop>
            <d:current-user-principal />
          </d:prop>
        </d:propfind>
        """.trimIndent()

        val request = Request.Builder()
            .url(caldavUrl)
            .method("PROPFIND", RequestBody.create("application/xml".toMediaTypeOrNull(), xmlBody))
            .header("Authorization", Credentials.basic(userName, userPwd))
            .header("Depth", "0")
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            val isValid = response.isSuccessful || response.code == 207
            response.close()
            isValid
        } catch (e: IOException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }



    fun isCalDAVOnlineWithAuth(callback: (Boolean) -> Unit) {
        val url = UserPrefs.getUrl()
        val username = UserPrefs.getUserName()
        val password = UserPrefs.getPwd()

        val credential = Credentials.basic(username, password)

        val request = Request.Builder()
            .url(url)
            .method("OPTIONS", null)
            .header("Authorization", credential)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                val online = response.isSuccessful
                response.close()
                callback(online)
            }
        })
    }


}