package com.ccec.timer.stationkiosk

import android.content.Context
import java.util.UUID

/** 稳定设备标识，用于遥测与远程指令 topic。 */
object DeviceIdentity {
    private const val PREFS = "ccec_device_identity"
    private const val KEY_ID = "device_uuid"

    fun getId(context: Context): String {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = sp.getString(KEY_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            sp.edit().putString(KEY_ID, id).apply()
        }
        return id
    }
}
