package com.a.kappa
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// PermissionHelper.kt

object PermissionHelper {

    private val CALENDAR_PERMISSIONS = arrayOf(
        android.Manifest.permission.WRITE_CALENDAR,
        android.Manifest.permission.READ_CALENDAR
    )

    fun areCalendarPermissionsGranted(activity: Activity): Boolean =
        CALENDAR_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

    fun checkAndRequestCalendarPermission(activity: Activity, requestCode: Int) {
        if (!areCalendarPermissionsGranted(activity)) {
            ActivityCompat.requestPermissions(activity, CALENDAR_PERMISSIONS, requestCode)
        }
    }
}
