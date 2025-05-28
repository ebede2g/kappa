package com.a.kappa

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TaskUtil {

    fun convertMillisToLocalDateTimeString(startMillis: Long): String {
        val instant = Instant.ofEpochMilli(startMillis)
        val zone = ZoneId.systemDefault()
        val localDateTime = instant.atZone(zone).toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        return localDateTime.format(formatter)
    }

    private fun convertLocalDateTimeStringToMillis(dateTimeStr: String): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val localDateTime = LocalDateTime.parse(dateTimeStr, formatter)
        val zone = ZoneId.systemDefault()
        return localDateTime.atZone(zone).toInstant().toEpochMilli()
    }

    private fun getCalendarIdByName(calName: String, context: Context): Long {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(calName)

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            }
        }
        return -1L
    }

    fun AddServerTask(title: String, description: String, startMillis: Long) {
        Log.d("TASK","----AddServerTasks----")

        val startDateTime = convertMillisToLocalDateTimeString(startMillis)
        val dueDateTime = convertMillisToLocalDateTimeString(startMillis + 60_000) // +1 хв



        val url = UserPrefs.getUrl()
        val name = UserPrefs.getUserName()
        val pwd = UserPrefs.getPwd()

        val fileName = title+startDateTime+".ics"
        val fullUrl = "$url$fileName"

        Log.d("TASK_LOG", "Full URL: $fullUrl")

        val icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//MyApp//EN
        BEGIN:VTODO
        UID:$fileName
        DTSTAMP:$startDateTime
        DTSTART:$startDateTime
        DUE:$dueDateTime
        SUMMARY:$title
        DESCRIPTION:$description
        STATUS:NEEDS-ACTION
        END:VTODO
        END:VCALENDAR
    """.trimIndent()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(fullUrl)
            .put(icsContent.toRequestBody("text/calendar; charset=utf-8".toMediaType()))
            .header("Authorization", Credentials.basic(name, pwd))
            .build()

        Log.d("TASK", "Pushing to $fullUrl")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TASK", "Push failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Log.d("TASK", "Push successful: ${response.code}")
                } else {
                    Log.e("TASK", "Push failed: ${response.code} ${response.message}")
                    Log.e("TASK", "Response body: $responseBody")
                }
            }
        })
    }

    fun AddLocalTasks(context: Context, title: String, description: String, startMillis: Long) {
        Log.d("TASK","----AddLocalTasks----")

        val calName = UserPrefs.getUID()

        val calendarId = getCalendarIdByName(calName, context)
        if (calendarId == -1L) {
            Log.e("TASK", "Календар не знайдено")
            return
        }

        val endMillis = startMillis + 60 * 1000 // +1 хв

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

        if (uri != null) {
            Log.d("TASK", "Подію додано: $uri")
        } else {
            Log.e("TASK", "Помилка додавання події")
        }
    }



    fun getTaskFromServer(context: Context, listOfIcsFilesToGet: List<String>) {
        val urlBase = UserPrefs.getUrl().trimEnd('/')
        val name = UserPrefs.getUserName()
        val pwd = UserPrefs.getPwd()

        for (fileName in listOfIcsFilesToGet) {
            val fileUrl = "$urlBase/$fileName"
            Log.d("TASK", "Requesting file: $fileUrl")

            val request = Request.Builder()
                .url(fileUrl)
                .get()
                .header("Authorization", Credentials.basic(name, pwd))
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("TASK", "Failed to get $fileName: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            Log.e("TASK", "Failed to get $fileName: ${response.code} ${response.message}")
                            return
                        }
                        val bodyStr = response.body?.string()
                        Log.d("TASK", "File $fileName content received")

                        val title = Regex("""SUMMARY:(.+)""").find(bodyStr ?: "")?.groups?.get(1)?.value ?: "No title"
                        val description = Regex("""DESCRIPTION:(.+)""").find(bodyStr ?: "")?.groups?.get(1)?.value ?: "No description"
                        val dtstartStr = Regex("""DTSTART:(.+)""").find(bodyStr ?: "")?.groups?.get(1)?.value ?: ""
                        val timeStart = if (dtstartStr.isNotEmpty()) convertLocalDateTimeStringToMillis(dtstartStr) else System.currentTimeMillis()

                        AddLocalTasks(context, title, description, timeStart)

                        Log.d("TASK", "Title: $title")
                        Log.d("TASK", "Description: $description")
                    }
                }
            })
        }
    }










    //шукає на серері список тасків та скачує його в локлаьний календар
    fun AddLocalTaskFromCalDavList(context: Context, listOfIcsFilesToGet: List<String>, onFinished: () -> Unit) {
        val urlBase = UserPrefs.getUrl().trimEnd('/')
        val name = UserPrefs.getUserName()
        val pwd = UserPrefs.getPwd()

        if (listOfIcsFilesToGet.isEmpty()) {
            onFinished()  // Якщо немає файлів, відразу викликаємо onFinished
            return
        }

        var completedRequests = 0
        val totalRequests = listOfIcsFilesToGet.size

        // синхронізація для роботи з completedRequests
        val lock = Any()

        for (fileName in listOfIcsFilesToGet) {
            val fileUrl = "$urlBase/$fileName.ics"
            Log.d("TASK", "Requesting file: $fileUrl")

            val request = Request.Builder()
                .url(fileUrl)
                .get()
                .header("Authorization", Credentials.basic(name, pwd))
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("TASK", "Failed to get $fileName: ${e.message}")
                    checkIfAllCompleted()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            Log.e("TASK", "Failed to get $fileName: ${response.code} ${response.message}")
                            checkIfAllCompleted()
                            return
                        }
                        val bodyStr = response.body?.string()
                        Log.d("TASK", "File $fileName content received")

                        val title = Regex("""SUMMARY:(.+)""").find(bodyStr ?: "")?.groups?.get(1)?.value ?: "No title"
                        val description = Regex("""DESCRIPTION:(.+)""").find(bodyStr ?: "")?.groups?.get(1)?.value ?: "No description"
                        val dtstartStr = Regex("""DTSTART:(.+)""").find(bodyStr ?: "")?.groups?.get(1)?.value ?: ""
                        val timeStart = if (dtstartStr.isNotEmpty()) convertLocalDateTimeStringToMillis(dtstartStr) else System.currentTimeMillis()

                        AddLocalTasks(context, title, description, timeStart)

                        Log.d("TASK", "Title: $title")
                        Log.d("TASK", "Description: $description")

                        checkIfAllCompleted()
                    }
                }

                private fun checkIfAllCompleted() {
                    synchronized(lock) {
                        completedRequests++
                        if (completedRequests == totalRequests) {
                            onFinished()
                        }
                    }
                }
            })
        }
    }








    fun fetchIcsEventSummaries(callback: (List<String>?) -> Unit) {
        val name = UserPrefs.getUserName()
        val pwd = UserPrefs.getPwd()

        val request = Request.Builder()
            .url(UserPrefs.getUrl())
            .get()
            .header("Authorization", Credentials.basic(name, pwd))
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TASK", "Failed to fetch ICS content: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("TASK", "Failed to fetch ICS: ${response.code} ${response.message}")
                    callback(null)
                    return
                }

                val bodyStr = response.body?.string() ?: ""
                Log.d("TASK", "ICS content received")

                val summaries = bodyStr.lines()
                    .map { it.trim() }
                    .filter { it.startsWith("SUMMARY:") }
                    .map { it.removePrefix("SUMMARY:") }

                val dtstarts = bodyStr.lines()
                    .map { it.trim() }
                    .filter { it.startsWith("DTSTART:") }
                    .map { it.removePrefix("DTSTART:") }

                if (summaries.isNotEmpty() && dtstarts.isNotEmpty()) {
                    // Комбінуємо summary + dtstart в окремі рядки
                    val combined = summaries.zip(dtstarts) { summary, dtstart -> "$summary$dtstart" }
                    callback(combined)  // Повертаємо нормальний список рядків
                } else {
                    callback(null)
                }
            }
        })
    }






}