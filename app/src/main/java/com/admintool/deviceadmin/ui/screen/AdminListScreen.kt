package com.admintool.deviceadmin.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admintool.deviceadmin.data.model.PrivilegeMode
import com.admintool.deviceadmin.ui.theme.PrimaryBlue
import com.admintool.deviceadmin.ui.theme.WarningRed
import com.admintool.deviceadmin.ui.viewmodel.AdminFilter
import com.admintool.deviceadmin.ui.viewmodel.DeviceAdminViewModel
import com.admintool.deviceadmin.ui.viewmodel.GeneralAppFilter
import com.admintool.deviceadmin.ui.viewmodel.MainTab
import com.admintool.deviceadmin.ui.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminListScreen(
    viewModel: DeviceAdminViewModel,
    onRequestShizukuPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val adminSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val generalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.RequestShizukuPermission -> onRequestShizukuPermission()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "기기 관리자 & 권한 관리 툴",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    }
                },
                actions = {
                    // PC-Free Standalone Setup Guide Button
                    IconButton(onClick = { viewModel.showShizukuGuide(true) }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "PC 없이 단독 설정 가이드",
                            tint = PrimaryBlue
                        )
                    }

                    // Refresh Button
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Tabs: Device Admin vs General Apps
            TabRow(
                selectedTabIndex = if (uiState.selectedMainTab == MainTab.DEVICE_ADMIN) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                Tab(
                    selected = uiState.selectedMainTab == MainTab.DEVICE_ADMIN,
                    onClick = { viewModel.selectMainTab(MainTab.DEVICE_ADMIN) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("기기 관리자 앱 (${uiState.allAdminApps.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedMainTab == MainTab.GENERAL_APPS,
                    onClick = { viewModel.selectMainTab(MainTab.GENERAL_APPS) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("일반 앱 권한 분석 (${uiState.allGeneralApps.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Privilege Mode Banner Card
            PrivilegeModeBanner(
                mode = uiState.privilegeMode,
                onRequestShizuku = onRequestShizukuPermission
            )

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        if (uiState.selectedMainTab == MainTab.DEVICE_ADMIN) "관리자 앱 이름 또는 패키지명 검색" else "일반 앱 이름 또는 패키지명 검색",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Filter Chips based on selected tab
            if (uiState.selectedMainTab == MainTab.DEVICE_ADMIN) {
                AdminFilterChipsRow(
                    selectedFilter = uiState.selectedAdminFilter,
                    onFilterSelected = { viewModel.onAdminFilterSelected(it) },
                    totalCount = uiState.allAdminApps.size,
                    activeCount = uiState.allAdminApps.count { it.isActiveAdmin }
                )
            } else {
                GeneralFilterChipsRow(
                    selectedFilter = uiState.selectedGeneralFilter,
                    onFilterSelected = { viewModel.onGeneralFilterSelected(it) },
                    totalCount = uiState.allGeneralApps.size,
                    userCount = uiState.allGeneralApps.count { !it.isSystemApp },
                    dangerousCount = uiState.allGeneralApps.count { it.dangerousPermissionCount > 0 }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main List or Loading / Empty States
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (uiState.selectedMainTab == MainTab.DEVICE_ADMIN) {
                if (uiState.filteredAdminApps.isEmpty()) {
                    EmptyStateView("조건에 일치하는 기기 관리자 앱이 없습니다.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredAdminApps, key = { it.packageName + it.className }) { app ->
                            AdminItemCard(
                                app = app,
                                privilegeMode = uiState.privilegeMode,
                                onCardClick = { viewModel.selectAppForDetail(app) },
                                onToggleActive = { isChecked ->
                                    if (isChecked) {
                                        viewModel.activateAdmin(app)
                                    } else {
                                        viewModel.requestDeactivation(app)
                                    }
                                },
                                onForceDeactivate = {
                                    viewModel.requestDeactivation(app)
                                },
                                onActivateAdmin = {
                                    viewModel.activateAdmin(app)
                                }
                            )
                        }
                    }
                }
            } else {
                // General Apps Tab List
                if (uiState.filteredGeneralApps.isEmpty()) {
                    EmptyStateView("조건에 일치하는 일반 앱이 없습니다.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredGeneralApps, key = { it.packageName }) { app ->
                            GeneralAppItemCard(
                                app = app,
                                onCardClick = { viewModel.selectGeneralAppForDetail(app) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Device Admin Detail Bottom Sheet
    uiState.selectedAppForDetail?.let { selectedApp ->
        AdminDetailBottomSheet(
            app = selectedApp,
            privilegeMode = uiState.privilegeMode,
            sheetState = adminSheetState,
            onDismiss = { viewModel.selectAppForDetail(null) },
            onTogglePermission = { permission, grant ->
                viewModel.togglePermission(permission, grant)
            },
            onForceStop = { pkg -> viewModel.forceStopApp(pkg) },
            onRevokeOverlay = { pkg -> viewModel.revokeOverlay(pkg) },
            onRevokeAccessibility = { pkg -> viewModel.revokeAccessibility(pkg) },
            onForceDeactivate = { app ->
                viewModel.selectAppForDetail(null)
                viewModel.requestDeactivation(app)
            },
            onActivateAdmin = { app ->
                viewModel.selectAppForDetail(null)
                viewModel.activateAdmin(app)
            }
        )
    }

    // General App Detail Bottom Sheet
    uiState.selectedGeneralAppForDetail?.let { selectedGeneralApp ->
        GeneralAppDetailBottomSheet(
            app = selectedGeneralApp,
            privilegeMode = uiState.privilegeMode,
            sheetState = generalSheetState,
            onDismiss = { viewModel.selectGeneralAppForDetail(null) },
            onTogglePermission = { permission, grant ->
                viewModel.togglePermission(permission, grant)
            },
            onForceStop = { pkg -> viewModel.forceStopApp(pkg) },
            onRevokeOverlay = { pkg -> viewModel.revokeOverlay(pkg) },
            onRevokeAccessibility = { pkg -> viewModel.revokeAccessibility(pkg) },
            onUninstall = { pkg -> viewModel.uninstallApp(pkg) }
        )
    }

    // Safety Warning Dialog for Critical System Admins
    uiState.appPendingDeactivation?.let { pendingApp ->
        SafetyWarningDialog(
            app = pendingApp,
            onConfirm = { viewModel.confirmPendingDeactivation() },
            onDismiss = { viewModel.dismissPendingDeactivation() }
        )
    }

    // Safe Mode Guide Dialog
    if (uiState.showSafeModeGuide) {
        SafeModeGuideDialog(
            onDismiss = { viewModel.showSafeModeGuide(false) }
        )
    }

    // Shizuku Standalone Setup Guide Dialog
    if (uiState.showShizukuGuide) {
        ShizukuSetupGuideDialog(
            onDismiss = { viewModel.showShizukuGuide(false) }
        )
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PrivilegeModeBanner(
    mode: PrivilegeMode,
    onRequestShizuku: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (mode) {
                PrivilegeMode.SHIZUKU -> PrimaryBlue.copy(alpha = 0.12f)
                PrivilegeMode.STANDARD -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color(mode.badgeColorHex),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "실행 모드: ${mode.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(mode.badgeColorHex)
                    )
                    Text(
                        text = when (mode) {
                            PrivilegeMode.SHIZUKU -> "무선 ADB 권한으로 설정 잠김 앱 해제 및 권한 즉시 변경 가능"
                            PrivilegeMode.STANDARD -> "설정에서 잠긴 앱 해제를 위해 Shizuku(무선 ADB) 연동을 권장합니다"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (mode == PrivilegeMode.STANDARD) {
                Surface(
                    color = PrimaryBlue,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(onClick = onRequestShizuku)
                ) {
                    Text(
                        text = "Shizuku 연동",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminFilterChipsRow(
    selectedFilter: AdminFilter,
    onFilterSelected: (AdminFilter) -> Unit,
    totalCount: Int,
    activeCount: Int
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == AdminFilter.ALL,
            onClick = { onFilterSelected(AdminFilter.ALL) },
            label = { Text("전체 ($totalCount)") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == AdminFilter.ACTIVE,
            onClick = { onFilterSelected(AdminFilter.ACTIVE) },
            label = { Text("활성화됨 ($activeCount)") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == AdminFilter.INACTIVE,
            onClick = { onFilterSelected(AdminFilter.INACTIVE) },
            label = { Text("비활성화됨 (${totalCount - activeCount})") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == AdminFilter.USER_ONLY,
            onClick = { onFilterSelected(AdminFilter.USER_ONLY) },
            label = { Text("사용자 앱") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == AdminFilter.SYSTEM_ONLY,
            onClick = { onFilterSelected(AdminFilter.SYSTEM_ONLY) },
            label = { Text("시스템 앱") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun GeneralFilterChipsRow(
    selectedFilter: GeneralAppFilter,
    onFilterSelected: (GeneralAppFilter) -> Unit,
    totalCount: Int,
    userCount: Int,
    dangerousCount: Int
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == GeneralAppFilter.ALL,
            onClick = { onFilterSelected(GeneralAppFilter.ALL) },
            label = { Text("전체 ($totalCount)") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == GeneralAppFilter.USER_ONLY,
            onClick = { onFilterSelected(GeneralAppFilter.USER_ONLY) },
            label = { Text("사용자 설치 앱 ($userCount)") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == GeneralAppFilter.DANGEROUS_ONLY,
            onClick = { onFilterSelected(GeneralAppFilter.DANGEROUS_ONLY) },
            label = { Text("위험 권한 보유 ($dangerousCount)") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == GeneralAppFilter.OVERLAY_ONLY,
            onClick = { onFilterSelected(GeneralAppFilter.OVERLAY_ONLY) },
            label = { Text("오버레이 보유") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WarningRed.copy(alpha = 0.2f))
        )
        FilterChip(
            selected = selectedFilter == GeneralAppFilter.SYSTEM_ONLY,
            onClick = { onFilterSelected(GeneralAppFilter.SYSTEM_ONLY) },
            label = { Text("시스템 앱 (${totalCount - userCount})") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f))
        )
    }
}
