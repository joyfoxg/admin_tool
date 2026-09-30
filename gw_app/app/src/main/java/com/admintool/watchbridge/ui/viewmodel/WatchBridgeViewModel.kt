package com.admintool.watchbridge.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.admintool.watchbridge.data.model.HotspotStatus
import com.admintool.watchbridge.domain.repository.HotspotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchUiState(
    val keepScreenOn: Boolean = true,
    val autoTimeoutMinutes: Int = 5,
    val remainingSeconds: Int? = null
)

@HiltViewModel
class WatchBridgeViewModel @Inject constructor(
    private val repository: HotspotRepository
) : ViewModel() {

    val hotspotStatus: StateFlow<HotspotStatus> = repository.hotspotStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HotspotStatus.Stopped
        )

    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            hotspotStatus.collect { status ->
                if (status is HotspotStatus.Running) {
                    startCountdownTimer()
                } else {
                    stopCountdownTimer()
                }
            }
        }
    }

    fun toggleHotspot() {
        when (hotspotStatus.value) {
            is HotspotStatus.Running, is HotspotStatus.Starting -> {
                repository.stopHotspot()
            }
            else -> {
                repository.startHotspot()
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        val timeoutMins = _uiState.value.autoTimeoutMinutes
        if (timeoutMins <= 0) return

        var remaining = timeoutMins * 60
        _uiState.value = _uiState.value.copy(remainingSeconds = remaining)

        timerJob = viewModelScope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
            }
            // Auto timeout reached -> Stop hotspot to save watch battery
            repository.stopHotspot()
        }
    }

    private fun stopCountdownTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = _uiState.value.copy(remainingSeconds = null)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(keepScreenOn = enabled)
    }

    fun setTimeoutMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(autoTimeoutMinutes = minutes)
    }
}
