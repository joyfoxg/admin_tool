package com.admintool.deviceadmin.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.admintool.deviceadmin.data.model.DeviceAdminApp
import com.admintool.deviceadmin.data.model.PrivilegeMode
import com.admintool.deviceadmin.ui.theme.AccentOrange
import com.admintool.deviceadmin.ui.theme.PrimaryBlue
import com.admintool.deviceadmin.ui.theme.SuccessGreen
import com.admintool.deviceadmin.ui.theme.WarningRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminItemCard(
    app: DeviceAdminApp,
    privilegeMode: PrivilegeMode,
    onCardClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onForceDeactivate: () -> Unit,
    onActivateAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isGreyedOutOrLocked) WarningRed.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                if (app.icon != null) {
                    val bitmap = app.icon.toBitmap(120, 120)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App Name & Package
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isCriticalSystemAdmin) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Critical System Admin",
                                tint = AccentOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Active Switch
                Switch(
                    checked = app.isActiveAdmin,
                    onCheckedChange = { isChecked ->
                        onToggleActive(isChecked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (app.isActiveAdmin) PrimaryBlue else Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges & Status Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status Badge
                StatusChip(
                    text = if (app.isActiveAdmin) "기기 관리자 활성" else "비활성화됨",
                    backgroundColor = if (app.isActiveAdmin) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                    textColor = if (app.isActiveAdmin) SuccessGreen else Color.Gray,
                    icon = if (app.isActiveAdmin) Icons.Default.CheckCircle else Icons.Default.Block
                )

                // Greyed Out / Locked in Settings Badge
                if (app.isGreyedOutOrLocked) {
                    StatusChip(
                        text = "🔒 설정에서 비활성화(잠김)",
                        backgroundColor = WarningRed.copy(alpha = 0.15f),
                        textColor = WarningRed,
                        icon = Icons.Default.Lock
                    )
                }

                // System/User Badge
                StatusChip(
                    text = if (app.isSystemApp) "시스템 앱" else "사용자 앱",
                    backgroundColor = if (app.isSystemApp) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    textColor = if (app.isSystemApp) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )

                // Overlay Warning Tag
                if (app.hasOverlayPermission) {
                    StatusChip(
                        text = "🚨 오버레이(화면 방해)",
                        backgroundColor = WarningRed.copy(alpha = 0.15f),
                        textColor = WarningRed
                    )
                }

                // Accessibility Warning Tag
                if (app.hasAccessibilityPermission) {
                    StatusChip(
                        text = "♿ 접근성 권한",
                        backgroundColor = AccentOrange.copy(alpha = 0.15f),
                        textColor = AccentOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCardClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("상세 & 권한", fontSize = 13.sp)
                }

                if (app.isActiveAdmin) {
                    ElevatedButton(
                        onClick = onForceDeactivate,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (app.isGreyedOutOrLocked) WarningRed else if (privilegeMode == PrivilegeMode.SHIZUKU) PrimaryBlue else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (app.isGreyedOutOrLocked || privilegeMode == PrivilegeMode.SHIZUKU) Color.White else MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (app.isGreyedOutOrLocked) Icons.Default.Lock else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (app.isGreyedOutOrLocked) "잠김 강제 해제" else if (privilegeMode == PrivilegeMode.SHIZUKU) "강제 해제" else "해제 시도",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    ElevatedButton(
                        onClick = onActivateAdmin,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "관리자 설정",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
