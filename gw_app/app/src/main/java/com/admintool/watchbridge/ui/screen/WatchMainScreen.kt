package com.admintool.watchbridge.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.WifiTetheringError
import androidx.compose.material.icons.filled.WifiTetheringOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.admintool.watchbridge.data.model.HotspotStatus
import com.admintool.watchbridge.ui.theme.SuccessGreen
import com.admintool.watchbridge.ui.theme.TealPrimary
import com.admintool.watchbridge.ui.theme.WarningRed
import com.admintool.watchbridge.ui.viewmodel.WatchBridgeViewModel

@Composable
fun WatchMainScreen(
    viewModel: WatchBridgeViewModel
) {
    val hotspotStatus by viewModel.hotspotStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        state = scrollState,
        contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title & Status Badge
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "워치 Shizuku 브릿지",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusIndicator(hotspotStatus)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Big Circular Hotspot Toggle Button
        item {
            val isRunning = hotspotStatus is HotspotStatus.Running
            val isStarting = hotspotStatus is HotspotStatus.Starting

            Button(
                onClick = { viewModel.toggleHotspot() },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isRunning -> WarningRed
                        isStarting -> Color(0xFFD97706)
                        else -> TealPrimary
                    }
                )
            ) {
                if (isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.WifiTetheringOff else Icons.Default.WifiTethering,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.Black
                        )
                        Text(
                            text = if (isRunning) "중지" else "시작",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Remaining Time Countdown Badge (If Running)
        if (hotspotStatus is HotspotStatus.Running && uiState.remainingSeconds != null) {
            item {
                val mins = (uiState.remainingSeconds ?: 0) / 60
                val secs = (uiState.remainingSeconds ?: 0) % 60
                val timeStr = String.format("%02d:%02d", mins, secs)

                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .background(Color(0xFF27272A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = SuccessGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "자동 종료까지 $timeStr",
                        fontSize = 10.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // SSID & Password Card (When Running)
        if (hotspotStatus is HotspotStatus.Running) {
            val running = hotspotStatus as HotspotStatus.Running
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    onClick = {},
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp), tint = TealPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wi-Fi 이름(SSID)", fontSize = 10.sp, color = Color.Gray)
                        }
                        Text(
                            text = running.ssid,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        if (running.password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = TealPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("비밀번호", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(
                                text = running.password,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Error message card (if failed)
        if (hotspotStatus is HotspotStatus.Failed) {
            val failed = hotspotStatus as HotspotStatus.Failed
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    onClick = {},
                    colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.WifiTetheringError, contentDescription = null, tint = WarningRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = failed.reason, fontSize = 10.sp, color = WarningRed, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Guide Instructions Card
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                onClick = {},
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "📱 폰 연결 방법", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "1. 폰 Wi-Fi에서 워치 핫스팟 연결", fontSize = 9.sp, color = Color.LightGray)
                    Text(text = "2. 폰 [무선 디버깅] 켜기", fontSize = 9.sp, color = Color.LightGray)
                    Text(text = "3. 폰 [Shizuku] 앱 [시작] 터치!", fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: HotspotStatus) {
    val (text, color) = when (status) {
        is HotspotStatus.Running -> "핫스팟 켜짐 (폰 연결 대기)" to SuccessGreen
        is HotspotStatus.Starting -> status.message to Color(0xFFD97706)
        is HotspotStatus.Failed -> "핫스팟 오류" to WarningRed
        is HotspotStatus.Stopped -> "핫스팟 꺼짐" to Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
