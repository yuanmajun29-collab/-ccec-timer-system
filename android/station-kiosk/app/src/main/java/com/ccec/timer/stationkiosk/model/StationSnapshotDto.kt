package com.ccec.timer.stationkiosk.model

import com.google.gson.annotations.SerializedName

/** 与后端 StationSnapshot JSON（Jackson camelCase）对齐 */
data class StationSnapshotDto(
    val stationCode: String? = null,
    val so: String? = null,
    val esn: String? = null,
    val engineType: String? = null,
    val standardCt: Int = 0,
    val elapsed: Long = 0,
    val remain: Long = 0,
    val status: String? = null,
    val color: String? = null,
    val serverTime: String? = null,
    @SerializedName("completedActualSeconds") val completedActualSeconds: Long? = null,
    @SerializedName("cycleStartTime") val cycleStartTime: String? = null
)
