package com.admintool.deviceadmin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admintool.deviceadmin.data.model.DeviceAdminApp
import com.admintool.deviceadmin.ui.theme.WarningRed

@Composable
fun SafetyWarningDialog(
    app: DeviceAdminApp,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningRed
            )
        },
        title = {
            Text(
                text = "⚠️ 중요 시스템 기기 관리자 해제 경고",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WarningRed
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "[${app.appName}]은(는) 구글 또는 제조사의 핵심 시스템/보안 기기 관리자(예: 내 기기 찾기, 분실 보호 등)입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "이 관리자를 해제하면 기기 분실 시 원격 잠금 및 위치 추적이 불가능해지며, 일부 시스템 기능이 정상 동작하지 않을 수 있습니다. 정말 강제 해제하시겠습니까?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            ElevatedButton(
                onClick = onConfirm,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = WarningRed,
                    contentColor = Color.White
                )
            ) {
                Text("위험 감수하고 해제", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
