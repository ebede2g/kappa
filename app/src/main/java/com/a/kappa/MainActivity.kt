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





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestCalendarPermission(this, PERMISSION_REQUEST_CODE)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        statusText = findViewById(R.id.status)
        statusText.setText(if (ChekUtil.hasPermissionToCalendar(this)) UserPrefs.getStatus() else UserPrefs.getStatus()+"\n> Надайте дозвіл на викорсиатння календаря")

        findViewById<Button>(R.id.goToSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val btnAddTask = findViewById<Button>(R.id.btnAddTask)
        btnAddTask.setOnClickListener{
            val startMillis = System.currentTimeMillis() + 2 * 60 * 60 * 1000
            val title = "titlee"
            val descr = "descrr"

            ChekUtil.isOnline(this) { online ->
                if (online) {
                    TaskUtil.AddServerTask(title, descr, startMillis)
                } else {
                    TaskUtil.AddLocalTasks(this, title, descr, startMillis)
                }
            }

        }

        val btnGoToReminders = findViewById<Button>(R.id.goToReminders)
        btnGoToReminders.setOnClickListener{
            startActivity(Intent(this,RemindersActivity::class.java))
        }

    }
}