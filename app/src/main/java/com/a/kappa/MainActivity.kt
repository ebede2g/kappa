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
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset


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



        val taskTitle = findViewById<EditText>(R.id.taskTitle)
        val taskDescr = findViewById<EditText>(R.id.taskDesc)
        val btnAddTask = findViewById<Button>(R.id.btnAddTask)

        btnAddTask.isEnabled = false
        taskTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Вмикаємо кнопку, якщо введено 2 або більше символів
                btnAddTask.isEnabled = (s?.length ?: 0) >= 2
            }
            override fun afterTextChanged(s: Editable?) {}
        })



        btnAddTask.setOnClickListener{


            val intervals = SpacedAlgorithm.twilin(6, 1.2).toMutableList()
            intervals.add(0, LocalDateTime.now().plusMinutes(3))  // додати на початок Поточний час + 3 хвилини
            intervals.forEach {
                val t = it.toString()
                Log.d("TASK", t)
            }


            val title = taskTitle.text.toString()
            val descr = taskDescr.text.toString()

            ChekUtil.isOnline(this) { online ->
                if (online) {
                    intervals.forEach {
                        val millis = it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        TaskUtil.AddServerTask(title, descr, millis)
                    }
                } else {
                    intervals.forEach {
                        val millis = it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        TaskUtil.AddLocalTasks(this, title, descr, millis)
                    }
                }
            }

            taskTitle.setText("")
            taskDescr.setText("")
        }

        val btnGoToReminders = findViewById<Button>(R.id.goToReminders)
        btnGoToReminders.setOnClickListener{
            startActivity(Intent(this,RemindersActivity::class.java))
        }

    }
}