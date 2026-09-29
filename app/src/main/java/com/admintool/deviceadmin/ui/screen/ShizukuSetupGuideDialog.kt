package com.admintool.deviceadmin.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admintool.deviceadmin.ui.theme.PrimaryBlue

@Composable
fun ShizukuSetupGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = PrimaryBlue
            )
        },
        title = {
            Text(
                text = "📱 PC 없이 폰 단독 실행 안내 (무선 디버깅)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PC 연결이나 케이블 없이, 안드로이드의 '무선 디버깅' 기능과 'Shizuku' 앱을 이용해 스마트폰 단독으로 모든 강력한 기능을 즉시 활성화할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SetupStepItem(step = "1. Shizuku 앱 설치", desc = "Google Play 스토어에서 무료 앱 'Shizuku'를 설치합니다.")
                        Spacer(modifier = Modifier.height(6.dp))
                        SetupStepItem(step = "2. 무선 디버깅 켜기", desc = "[휴대폰 설정 > 개발자 옵션 > 무선 디버깅]을 켭니다 (Wi-Fi 연결 필요).")
                        Spacer(modifier = Modifier.height(6.dp))
                        SetupStepItem(step = "3. 폰 안에서 페어링", desc = "Shizuku 앱의 [페어링]을 누르고, 알림창에 뜬 6자리 페어링 코드를 입력합니다.")
                        Spacer(modifier = Modifier.height(6.dp))
                        SetupStepItem(step = "4. 서비스 시작 & 본 앱 연동", desc = "Shizuku에서 [시작]을 누른 뒤, 본 앱 상단의 [Shizuku 연동]을 터치하면 끝!")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("이해했습니다", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SetupStepItem(step: String, desc: String) {
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
