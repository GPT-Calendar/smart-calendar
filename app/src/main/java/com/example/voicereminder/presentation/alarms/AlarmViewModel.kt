package com.example.voicereminder.presentation.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.domain.AlarmResult
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.domain.models.Alarm
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for alarm list screen
 */
data class AlarmListUiState(
    val alarms: List<Alarm> = emptyList(),
    val nextAlarm: Alarm? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI state for alarm detail/edit screen
 */
data class AlarmDetailUiState(
    val alarm: Alarm? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * ViewModel for alarm management screens
 */
class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    
    private val alarmManager = EnhancedAlarmManager.getInstance(application)
    
    // Alarm list state
    private val _listUiState = MutableStateFlow(AlarmListUiState())
    val listUiState: StateFlow<AlarmListUiState> = _listUiState.asStateFlow()
    
    // Alarm detail state
    private val _detailUiState = MutableStateFlow(AlarmDetailUiState())
    val detailUiState: StateFlow<AlarmDetailUiState> = _detailUiState.asStateFlow()
    
    init {
        // Observe alarms
        viewModelScope.launch {
            alarmManager.getAllAlarmsFlow().collect { alarms ->
                _listUiState.update { it.copy(alarms = alarms, isLoading = false) }
            }
        }
        
        // Observe next alarm
        viewModelScope.launch {
            alarmManager.getNextAlarmFlow().collect { nextAlarm ->
                _listUiState.update { it.copy(nextAlarm = nextAlarm) }
            }
        }
    }
    
    /**
     * Create a new alarm
     */
    fun createAlarm(
        hour: Int,
        minute: Int,
        label: String = "Alarm",
        repeatDays: List<Int> = emptyList(),
        vibrate: Boolean = true
    ) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isSaving = true, error = null) }
            
            when (val result = alarmManager.createAlarm(
                hour = hour,
                minute = minute,
                label = label,
                repeatDays = repeatDays,
                vibrate = vibrate
            )) {
                is AlarmResult.Success -> {
                    _detailUiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is AlarmResult.Error -> {
                    _detailUiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }
    
    /**
     * Toggle alarm enabled state
     */
    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmManager.toggleAlarm(alarm.id, !alarm.isEnabled)
        }
    }
    
    /**
     * Delete an alarm
     */
    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            alarmManager.deleteAlarm(alarmId)
        }
    }
    
    /**
     * Update an existing alarm
     */
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isSaving = true, error = null) }
            
            when (val result = alarmManager.updateAlarm(alarm)) {
                is AlarmResult.Success -> {
                    _detailUiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is AlarmResult.Error -> {
                    _detailUiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }
    
    /**
     * Snooze an alarm
     */
    fun snoozeAlarm(alarmId: Long, minutes: Int = 5) {
        viewModelScope.launch {
            alarmManager.snoozeAlarm(alarmId, minutes)
        }
    }
    
    /**
     * Dismiss an alarm
     */
    fun dismissAlarm(alarmId: Long) {
        viewModelScope.launch {
            alarmManager.dismissAlarm(alarmId)
        }
    }
    
    /**
     * Load an alarm for editing
     */
    fun loadAlarm(alarmId: Long) {
        viewModelScope.launch {
            _detailUiState.update { it.copy(isEditing = true) }
            val alarm = alarmManager.getAlarmById(alarmId)
            _detailUiState.update { it.copy(alarm = alarm) }
        }
    }
    
    /**
     * Clear detail state
     */
    fun clearDetailState() {
        _detailUiState.value = AlarmDetailUiState()
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _listUiState.update { it.copy(error = null) }
        _detailUiState.update { it.copy(error = null) }
    }
    
    /**
     * Check if exact alarms can be scheduled
     */
    fun canScheduleExactAlarms(): Boolean {
        return alarmManager.canScheduleExactAlarms()
    }
}
