//UserPrefers

package com.a.kappa

import android.content.Context
import android.content.SharedPreferences
import android.app.Application


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        UserPrefs.init(this)
    }
}


object UserPrefs {
    private const val PREFS_NAME = "UserData"

    private const val CAlDAV_URL = "calDav_URL"
    private const val CAlDAV_PWD = "calDav_PWD"

    private const val Calendar_IP = "cal_IP"
    private const val Calendar_UserName = "cal_UserName"
    private const val Calendar_UID = "cal_UID"
    private const val Calendar_ID = "cal_ID"

    private const val Mobile_TOKEN = "mob_token"
    private const val IsOfflineAccount = "is_offline"
    private const val Status = "status"

    private const val Slider_N = "slider_n"
    private const val Slider_J = "slider_j"

    private const val IsSyncing = "is_syncing"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }


    fun getUrl(): String = prefs.getString(CAlDAV_URL, "") ?: ""
    fun setUrl(url: String) {
        prefs.edit().putString(CAlDAV_URL, url).apply()
    }

    fun getPwd(): String = prefs.getString(CAlDAV_PWD, "") ?: ""
    fun setPwd(pwd: String) {
        prefs.edit().putString(CAlDAV_PWD, pwd).apply()
    }


    fun getUserName(): String = prefs.getString(Calendar_UserName, "") ?: ""
    fun setUserName(a: String) {
        prefs.edit().putString(Calendar_UserName, a).apply()
    }

    fun getIp(): String = prefs.getString(Calendar_IP, "") ?: ""
    fun setIp(a: String) {
        prefs.edit().putString(Calendar_IP, a).apply()
    }

    fun getUID(): String = prefs.getString(Calendar_UID, "") ?: ""
    fun setUID(a: String) {
        prefs.edit().putString(Calendar_UID, a).apply()
    }

    fun getID(): Long = prefs.getLong(Calendar_ID, -1L)
    fun setID(id: Long) {
        prefs.edit().putLong(Calendar_ID, id).apply()
    }


    fun getToken(): String = prefs.getString(Mobile_TOKEN, "") ?: ""
    fun setToken(nam: String) {
        prefs.edit().putString(Mobile_TOKEN, nam).apply()
    }

    fun getIs_offline(): Boolean = prefs.getBoolean(IsOfflineAccount, false)
    fun setIs_offline(a: Boolean) {
        prefs.edit().putBoolean(IsOfflineAccount, a).apply()
    }

    fun getStatus(): String = prefs.getString(Status, "Севрер не знайдено\n" +"Поля порожні")?:""
    fun setStatus(a: String) {
        prefs.edit().putString(Status, a).apply()
    }



    fun getSlider_N(): Int = prefs.getInt(Slider_N, 8)
    fun setSlider_N(a: Int) {
        prefs.edit().putInt(Slider_N, a).apply()
    }

    fun getSlider_J(): Int = prefs.getInt(Slider_J, 1532)
    fun setSlider_J(a: Int) {
        prefs.edit().putInt(Slider_J, a).apply()
    }

    fun IsSyncing(): Boolean = prefs.getBoolean(IsSyncing, false)
    fun IsSyncing(a: Boolean) {
        prefs.edit().putBoolean(IsSyncing, a).apply()
    }

}
