package com.a.kappa

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.a.kappa.PermissionHelper.checkAndRequestCalendarPermission
import java.time.LocalDateTime
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import java.time.ZoneId


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1001

    private lateinit var statusText: TextView
    private lateinit var btnGoToReminders: Button

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Дозвіл надано!", Toast.LENGTH_LONG).show()
                statusText.setText(UserPrefs.getStatus())
            } else {
                Toast.makeText(this, "Надайте дозвіл на використання календаря", Toast.LENGTH_LONG).show()
                statusText.setText(UserPrefs.getStatus() + "\n> Надайте дозвіл на використання календаря")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestCalendarPermission(this, PERMISSION_REQUEST_CODE)
        setContentView(R.layout.activity_main)

        val taskTitle = findViewById<EditText>(R.id.taskTitle)
        val taskDescr = findViewById<EditText>(R.id.taskDesc)
        val btnAddTask = findViewById<Button>(R.id.btnAddTask)

        btnGoToReminders = findViewById<Button>(R.id.goToReminders)


        var calPerm = ChekUtil.hasPermissionToCalendar(this)
        statusText = findViewById(R.id.status)
        statusText.setText(if (calPerm) UserPrefs.getStatus() else UserPrefs.getStatus()+"\n> Надайте дозвіл на викорсиатння календаря")
        btnGoToReminders.isEnabled=calPerm

        findViewById<Button>(R.id.goToSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }


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
            val intervals = SpacedAlgorithm.twlist(UserPrefs.getSlider_N(), UserPrefs.getSlider_J().toDouble()/1000)
                .map { it.withNano(0) }
                .toMutableList()
            //intervals.add(0, LocalDateTime.now().plusMinutes(3).withSecond(0).withNano(0))
            intervals.forEach {
                Log.d("TASK", it.toString())
            }



            val title = taskTitle.text.toString()
            val descr = taskDescr.text.toString()

            ChekUtil.isObserverOnline(this) { online ->
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

        btnGoToReminders.setOnClickListener{
            startActivity(Intent(this,RemindersActivity::class.java))
        }


        val seekBarN = findViewById<SeekBar>(R.id.verticalSeekBar_N)
        val seekBarJ = findViewById<SeekBar>(R.id.verticalSeekBar_J)
        val showN = findViewById<TextView>(R.id.ShowN)
        val showJ = findViewById<EditText>(R.id.ShowJ)
        val until = findViewById<TextView>(R.id.Untill)

        val sa_n = UserPrefs.getSlider_N()
        val sa_j = UserPrefs.getSlider_J()

        seekBarN.progress = sa_n
        seekBarJ.progress = sa_j

        showN.setText(sa_n.toString())
        showJ.setText((sa_j.toFloat()/1000) .toString())

        until.setText(SpacedAlgorithm.untilApro(sa_n,sa_j.toDouble()/1000).toString())


        seekBarN.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                showN.setText(progress.toString())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                UserPrefs.setSlider_N(sb?.progress ?: 3)
                until.setText(SpacedAlgorithm.untilApro(UserPrefs.getSlider_N(), UserPrefs.getSlider_J().toDouble()/1000).toString())
            }
        })

        seekBarJ.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                showJ.setText((progress.toFloat() / 1000).toString())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                UserPrefs.setSlider_J(sb?.progress ?: 1001)
                until.setText(SpacedAlgorithm.untilApro(UserPrefs.getSlider_N(), UserPrefs.getSlider_J().toDouble()/1000).toString())
            }
        })




    }

    override fun onResume() {
        super.onResume()
        var calPerm = ChekUtil.hasPermissionToCalendar(this)
        statusText.setText(if (calPerm) UserPrefs.getStatus() else UserPrefs.getStatus()+"\n> Надайте дозвіл на викорсиатння календаря")
        btnGoToReminders.isEnabled=calPerm
    }
}