package com.ccec.timer.stationkiosk.ui

import com.ccec.timer.stationkiosk.model.StationSnapshotDto

enum class UiSource {
    LIVE,
    CACHE,
    DISCONNECTED
}

data class StationUiState(
    val snapshot: StationSnapshotDto,
    val source: UiSource,
    val updatedAtMillis: Long
)
