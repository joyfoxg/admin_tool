package com.admintool.deviceadmin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.admintool.deviceadmin.ui.theme.PrimaryBlue

@Composable
fun SafeModeGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PrimaryBlue
            )
        },
        title = {
            Text(
                text = "안전 모드(Safe Mode) 해제 가이드",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Shizuku(ADB) 또는 Root 권한이 없는 경우, 악성 앱의 방해(오버레이, 터치 가로채기)를 무력화하려면 '안전 모드'로 부팅하여 해제해야 합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        StepItem(step = "1단계", desc = "스마트폰 전원 버튼을 길게 누릅니다.")
                        Spacer(modifier = Modifier.height(6.dp))
                        StepItem(step = "2단계", desc = "화면의 [전원 끄기] 또는 [다시 시작] 아이콘을 2~3초간 길게 터치합니다.")
                        Spacer(modifier = Modifier.height(6.dp))
                        StepItem(step = "3단계", desc = "[안전 모드] 아이콘이 나타나면 터치하여 재부팅합니다.")
                        Spacer(modifier = Modifier.height(6.dp))
                        StepItem(step = "4단계", desc = "재부팅 후 [설정 > 보안 및 개인정보보호 > 기타 보안 설정 > 기기 관리자 앱]으로 이동하여 해당 앱을 비활성화하고 삭제합니다.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun StepItem(step: String, desc: String) {
    Column {
        Text(
            text = step,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = PrimaryBlue
        )
        Text(
            text = desc,
            fontSize = 12.sp,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
