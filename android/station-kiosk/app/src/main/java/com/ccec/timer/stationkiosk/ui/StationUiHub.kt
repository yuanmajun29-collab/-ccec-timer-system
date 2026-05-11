package com.ccec.timer.stationkiosk.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI 与 MQTT 服务之间的轻量状态总线 */
object StationUiHub {
    private val _state = MutableStateFlow<StationUiState?>(null)
    val state: StateFlow<StationUiState?> = _state.asStateFlow()

    fun emit(state: StationUiState) {
        _state.value = state
    }

    fun clear() {
        _state.value = null
    }
}
