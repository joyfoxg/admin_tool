package com.admintool.deviceadmin.ui.viewmodel

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.admintool.deviceadmin.data.model.AppPermission
import com.admintool.deviceadmin.data.model.DeviceAdminApp
import com.admintool.deviceadmin.data.model.InstalledAppInfo
import com.admintool.deviceadmin.data.model.PrivilegeMode
import com.admintool.deviceadmin.data.repository.DeviceAdminRepository
import com.admintool.deviceadmin.data.repository.PermissionRepository
import com.admintool.deviceadmin.domain.usecase.CheckPrivilegeModeUseCase
import com.admintool.deviceadmin.domain.usecase.ForceDeactivateAdminUseCase
import com.admintool.deviceadmin.domain.usecase.GetDeviceAdminsUseCase
import com.admintool.deviceadmin.domain.usecase.GetGeneralAppsUseCase
import com.admintool.deviceadmin.domain.usecase.TogglePermissionUseCase
import com.admintool.deviceadmin.domain.usecase.UninstallAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainTab {
    DEVICE_ADMIN,
    GENERAL_APPS
}

enum class AdminFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    USER_ONLY,
    SYSTEM_ONLY
}

enum class GeneralAppFilter {
    ALL,
    USER_ONLY,
    SYSTEM_ONLY,
    DANGEROUS_ONLY,
    OVERLAY_ONLY
}

data class AdminUiState(
    val isLoading: Boolean = true,
    val selectedMainTab: MainTab = MainTab.DEVICE_ADMIN,
    val privilegeMode: PrivilegeMode = PrivilegeMode.STANDARD,
    // Device Admin Tab
    val allAdminApps: List<DeviceAdminApp> = emptyList(),
    val filteredAdminApps: List<DeviceAdminApp> = emptyList(),
    val selectedAdminFilter: AdminFilter = AdminFilter.ALL,
    val selectedAppForDetail: DeviceAdminApp? = null,
    val appPendingDeactivation: DeviceAdminApp? = null,
    // General Apps Tab
    val allGeneralApps: List<InstalledAppInfo> = emptyList(),
    val filteredGeneralApps: List<InstalledAppInfo> = emptyList(),
    val selectedGeneralFilter: GeneralAppFilter = GeneralAppFilter.ALL,
    val selectedGeneralAppForDetail: InstalledAppInfo? = null,
    // Search & Dialogs
    val searchQuery: String = "",
    val showSafeModeGuide: Boolean = false,
    val showShizukuGuide: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class ShowSnackbar(val message: String) : UiEvent
    data object RequestShizukuPermission : UiEvent
}

@HiltViewModel
class DeviceAdminViewModel @Inject constructor(
    private val getDeviceAdminsUseCase: GetDeviceAdminsUseCase,
    private val getGeneralAppsUseCase: GetGeneralAppsUseCase,
    private val forceDeactivateAdminUseCase: ForceDeactivateAdminUseCase,
    private val togglePermissionUseCase: TogglePermissionUseCase,
    private val checkPrivilegeModeUseCase: CheckPrivilegeModeUseCase,
    private val uninstallAppUseCase: UninstallAppUseCase,
    private val deviceAdminRepository: DeviceAdminRepository,
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val mode = checkPrivilegeModeUseCase()
            _uiState.update { it.copy(privilegeMode = mode) }

            // Load device admin apps
            launch {
                getDeviceAdminsUseCase()
                    .catch { e ->
                        _uiState.update { it.copy(errorMessage = "기기 관리자 로드 실패: ${e.localizedMessage}") }
                    }
                    .collect { list ->
                        _uiState.update { state ->
                            val filtered = filterAdminApps(list, state.searchQuery, state.selectedAdminFilter)
                            state.copy(
                                allAdminApps = list,
                                filteredAdminApps = filtered
                            )
                        }
                    }
            }

            // Load general apps
            launch {
                getGeneralAppsUseCase()
                    .catch { e ->
                        _uiState.update { it.copy(errorMessage = "일반 앱 로드 실패: ${e.localizedMessage}") }
                    }
                    .collect { list ->
                        _uiState.update { state ->
                            val filtered = filterGeneralApps(list, state.searchQuery, state.selectedGeneralFilter)
                            state.copy(
                                isLoading = false,
                                allGeneralApps = list,
                                filteredGeneralApps = filtered
                            )
                        }
                    }
            }
        }
    }

    fun selectMainTab(tab: MainTab) {
        _uiState.update { state ->
            val updated = state.copy(selectedMainTab = tab, searchQuery = "")
            if (tab == MainTab.DEVICE_ADMIN) {
                updated.copy(filteredAdminApps = filterAdminApps(state.allAdminApps, "", state.selectedAdminFilter))
            } else {
                updated.copy(filteredGeneralApps = filterGeneralApps(state.allGeneralApps, "", state.selectedGeneralFilter))
            }
        }
    }

    fun refreshPrivilegeMode() {
        viewModelScope.launch {
            val mode = checkPrivilegeModeUseCase()
            _uiState.update { it.copy(privilegeMode = mode) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            if (state.selectedMainTab == MainTab.DEVICE_ADMIN) {
                val filtered = filterAdminApps(state.allAdminApps, query, state.selectedAdminFilter)
                state.copy(searchQuery = query, filteredAdminApps = filtered)
            } else {
                val filtered = filterGeneralApps(state.allGeneralApps, query, state.selectedGeneralFilter)
                state.copy(searchQuery = query, filteredGeneralApps = filtered)
            }
        }
    }

    fun onAdminFilterSelected(filter: AdminFilter) {
        _uiState.update { state ->
            val filtered = filterAdminApps(state.allAdminApps, state.searchQuery, filter)
            state.copy(selectedAdminFilter = filter, filteredAdminApps = filtered)
        }
    }

    fun onGeneralFilterSelected(filter: GeneralAppFilter) {
        _uiState.update { state ->
            val filtered = filterGeneralApps(state.allGeneralApps, state.searchQuery, filter)
            state.copy(selectedGeneralFilter = filter, filteredGeneralApps = filtered)
        }
    }

    private fun filterAdminApps(
        list: List<DeviceAdminApp>,
        query: String,
        filter: AdminFilter
    ): List<DeviceAdminApp> {
        return list.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AdminFilter.ALL -> true
                AdminFilter.ACTIVE -> app.isActiveAdmin
                AdminFilter.INACTIVE -> !app.isActiveAdmin
                AdminFilter.USER_ONLY -> !app.isSystemApp
                AdminFilter.SYSTEM_ONLY -> app.isSystemApp
            }

            matchesQuery && matchesFilter
        }
    }

    private fun filterGeneralApps(
        list: List<InstalledAppInfo>,
        query: String,
        filter: GeneralAppFilter
    ): List<InstalledAppInfo> {
        return list.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                GeneralAppFilter.ALL -> true
                GeneralAppFilter.USER_ONLY -> !app.isSystemApp
                GeneralAppFilter.SYSTEM_ONLY -> app.isSystemApp
                GeneralAppFilter.DANGEROUS_ONLY -> app.dangerousPermissionCount > 0
                GeneralAppFilter.OVERLAY_ONLY -> app.hasOverlayPermission
            }

            matchesQuery && matchesFilter
        }
    }

    fun selectAppForDetail(app: DeviceAdminApp?) {
        _uiState.update { it.copy(selectedAppForDetail = app) }
        if (app != null) {
            refreshAppPermissions(app.packageName)
        }
    }

    fun selectGeneralAppForDetail(app: InstalledAppInfo?) {
        _uiState.update { it.copy(selectedGeneralAppForDetail = app) }
        if (app != null) {
            refreshGeneralAppPermissions(app.packageName)
        }
    }

    fun refreshAppPermissions(packageName: String) {
        viewModelScope.launch {
            val permissions = permissionRepository.getAppPermissions(packageName)
            _uiState.update { state ->
                val currentApp = state.selectedAppForDetail
                if (currentApp != null && currentApp.packageName == packageName) {
                    val updatedApp = currentApp.copy(permissions = permissions)
                    state.copy(selectedAppForDetail = updatedApp)
                } else {
                    state
                }
            }
        }
    }

    fun refreshGeneralAppPermissions(packageName: String) {
        viewModelScope.launch {
            val permissions = permissionRepository.getAppPermissions(packageName)
            _uiState.update { state ->
                val currentApp = state.selectedGeneralAppForDetail
                if (currentApp != null && currentApp.packageName == packageName) {
                    val updatedApp = currentApp.copy(permissions = permissions)
                    state.copy(selectedGeneralAppForDetail = updatedApp)
                } else {
                    state
                }
            }
        }
    }

    fun requestDeactivation(app: DeviceAdminApp) {
        if (app.isCriticalSystemAdmin) {
            _uiState.update { it.copy(appPendingDeactivation = app) }
        } else {
            performDeactivation(app)
        }
    }

    fun confirmPendingDeactivation() {
        val app = _uiState.value.appPendingDeactivation ?: return
        _uiState.update { it.copy(appPendingDeactivation = null) }
        performDeactivation(app)
    }

    fun dismissPendingDeactivation() {
        _uiState.update { it.copy(appPendingDeactivation = null) }
    }

    private fun performDeactivation(app: DeviceAdminApp) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = forceDeactivateAdminUseCase(app.componentName, app.packageName)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowToast("${app.appName} 기기 관리자 해제 완료"))
                loadData()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "해제 실패"
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                _eventFlow.emit(UiEvent.ShowSnackbar(errorMsg))
            }
        }
    }

    fun activateAdmin(app: DeviceAdminApp) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = deviceAdminRepository.activateAdmin(app.componentName)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowToast("${app.appName} 기기 관리자 활성화 완료"))
                loadData()
            } else {
                val err = result.exceptionOrNull()?.message ?: "활성화 실패"
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(UiEvent.ShowSnackbar(err))
            }
        }
    }

    fun togglePermission(permission: AppPermission, grant: Boolean) {
        val currentAdminApp = _uiState.value.selectedAppForDetail
        val currentGeneralApp = _uiState.value.selectedGeneralAppForDetail
        val packageName = currentAdminApp?.packageName ?: currentGeneralApp?.packageName ?: return

        viewModelScope.launch {
            val result = togglePermissionUseCase(packageName, permission, grant)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowToast("${permission.label} 변경 완료"))
                if (currentAdminApp != null) refreshAppPermissions(packageName)
                if (currentGeneralApp != null) refreshGeneralAppPermissions(packageName)
            } else {
                val err = result.exceptionOrNull()?.message ?: "권한 변경 실패"
                _eventFlow.emit(UiEvent.ShowSnackbar(err))
            }
        }
    }

    fun forceStopApp(packageName: String) {
        viewModelScope.launch {
            val result = permissionRepository.forceStopApp(packageName)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowToast("앱이 강제 종료되었습니다."))
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "종료 실패"))
            }
        }
    }

    fun revokeOverlay(packageName: String) {
        viewModelScope.launch {
            val result = permissionRepository.revokeOverlayPermission(packageName)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowToast("오버레이 권한이 차단되었습니다."))
                _uiState.value.selectedAppForDetail?.let { refreshAppPermissions(packageName) }
                _uiState.value.selectedGeneralAppForDetail?.let { refreshGeneralAppPermissions(packageName) }
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "오버레이 차단 실패"))
            }
        }
    }

    fun revokeAccessibility(packageName: String) {
        viewModelScope.launch {
            val result = permissionRepository.revokeAccessibilityPermission(packageName)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowToast("접근성 권한이 차단되었습니다."))
                _uiState.value.selectedAppForDetail?.let { refreshAppPermissions(packageName) }
                _uiState.value.selectedGeneralAppForDetail?.let { refreshGeneralAppPermissions(packageName) }
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "접근성 차단 실패"))
            }
        }
    }

    fun uninstallApp(packageName: String) {
        viewModelScope.launch {
            val result = uninstallAppUseCase(packageName)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowToast("앱 삭제가 요청되었습니다."))
                selectGeneralAppForDetail(null)
                loadData()
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "삭제 실패"))
            }
        }
    }

    fun showSafeModeGuide(show: Boolean) {
        _uiState.update { it.copy(showSafeModeGuide = show) }
    }

    fun showShizukuGuide(show: Boolean) {
        _uiState.update { it.copy(showShizukuGuide = show) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
