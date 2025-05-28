package com.a.kappa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.a.kappa.PermissionHelper.checkAndRequestCalendarPermission
import okhttp3.*
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.ContentValues
import android.content.Context

import android.provider.CalendarContract

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneId


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1001

    private lateinit var statusText: TextView

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
                Toast.makeText(this, "Надайте дозвіл на викорсиатння календаря", Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this, "Дозвіл надано!", Toast.LENGTH_LONG).show()
                statusText.setText( UserPrefs.getStatus())
            }
        }
    }

    private fun hasPermissionToCalendar(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestCalendarPermission(this, PERMISSION_REQUEST_CODE)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        statusText = findViewById(R.id.status)
        statusText.setText(if (hasPermissionToCalendar()) UserPrefs.getStatus() else UserPrefs.getStatus()+"\n> Надайте дозвіл на викорсиатння календаря")

        findViewById<Button>(R.id.goToSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val btnAddTask = findViewById<Button>(R.id.btnAddTask)
        btnAddTask.setOnClickListener{
            val startMillis = System.currentTimeMillis() + 2 * 60 * 60 * 1000
            val title = "titlee"
            val descr = "descrr"

            if(!hasPermissionToCalendar()){
                Log.d("TASK","ERR  >> Дозвіл на календар не надано")
            }else{
                Log.d("TASK","gut  >> Дозвіл на календар Є")
                if(UserPrefs.getIs_offline()){
                    Log.d("TASK","DONE >> Режим Офлайн")
                    TaskUtil.AddLocalTasks(this,title, descr, startMillis)
                }else{
                    Log.d("TASK","gut  >> Режим Онлайн")
                    if(UserPrefs.getUID()==""){
                        Log.d("TASK","ERR  >> UID порожнє, отже не ініціаолізвоано онлайн календар(вхід не виконано)")
                    }else{
                        Log.d("TASK","gut  >> UID ініціалізовано")
                        if (UserPrefs.getToken()==""){
                            Log.d("TASK","ERR  >> Токен не отримано, отже серер офлайн")
                        }else{
                            Log.d("TASK","gut  >> Токен присутній")
                            isServerOnline() { isOnline ->
                                if (!isOnline){
                                    Log.d("TASK","DONE >> Сервер не досяжний/офлайн. Але записано в локальний календар. Режим Офлайн")
                                    TaskUtil.AddLocalTasks(this, title, descr, startMillis)
                                }else{
                                    Log.d("TASK","gut  >> Сервер в досяжності")
                                    Log.d("TASK","DONE >> Режим Онлайн")
                                    TaskUtil.AddServerTask(title, descr, startMillis)
                                }
                            }
                        }
                    }
                }
            }
        }

        val btnGoToReminders = findViewById<Button>(R.id.goToReminders)
        btnGoToReminders.setOnClickListener{
            startActivity(Intent(this,RemindersActivity::class.java))
        }

    }
}