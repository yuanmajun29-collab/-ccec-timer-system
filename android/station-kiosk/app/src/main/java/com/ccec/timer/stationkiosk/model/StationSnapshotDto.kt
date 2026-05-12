package com.ccec.timer.stationkiosk.model

import com.google.gson.annotations.SerializedName

/**
 * 与后端推送 JSON 对齐：既支持旧版 `StationSnapshot` 扁平字段，也支持 V1.2 `STATE_UPDATE`
 *（`soNo`/`esnNo`/`state`/`ct`）。
 */
data class StationSnapshotDto(
    val type: String? = null,
    val stationCode: String? = null,
    @SerializedName("state") val wireState: String? = null,
    val so: String? = null,
    @SerializedName("soNo") val soNo: String? = null,
    val esn: String? = null,
    @SerializedName("esnNo") val esnNo: String? = null,
    val engineType: String? = null,
    val standardCt: Int = 0,
    @SerializedName("ct") val ctSeconds: Int? = null,
    val elapsed: Long = 0,
    val remain: Long = 0,
    val status: String? = null,
    val color: String? = null,
    val serverTime: String? = null,
    @SerializedName("completedActualSeconds") val completedActualSeconds: Long? = null,
    @SerializedName("cycleStartTime") val cycleStartTime: String? = null
) {
    fun displaySo(): String? = soNo?.takeIf { it.isNotBlank() } ?: so
    fun displayEsn(): String? = esnNo?.takeIf { it.isNotBlank() } ?: esn
    fun displayStatus(): String? = wireState?.takeIf { it.isNotBlank() } ?: status
}
