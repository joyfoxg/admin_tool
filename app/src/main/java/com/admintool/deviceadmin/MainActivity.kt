package com.admintool.deviceadmin

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.admintool.deviceadmin.ui.screen.AdminListScreen
import com.admintool.deviceadmin.ui.theme.DeviceAdminToolTheme
import com.admintool.deviceadmin.ui.viewmodel.DeviceAdminViewModel
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: DeviceAdminViewModel by viewModels()

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku (ADB) 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show()
            viewModel.refreshPrivilegeMode()
            viewModel.loadData()
        } else {
            Toast.makeText(this, "Shizuku 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModel.refreshPrivilegeMode()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        viewModel.refreshPrivilegeMode()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            // Shizuku might not be installed or available
        }

        setContent {
            DeviceAdminToolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminListScreen(
                        viewModel = viewModel,
                        onRequestShizukuPermission = { requestShizukuPermission() }
                    )
                }
            }
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (Shizuku.getVersion() < 11) {
                Toast.makeText(this, "Shizuku v11 이상이 필요합니다.", Toast.LENGTH_SHORT).show()
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "이미 Shizuku 권한이 허용되어 있습니다.", Toast.LENGTH_SHORT).show()
                viewModel.refreshPrivilegeMode()
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(1001)
            } else {
                Shizuku.requestPermission(1001)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Shizuku 앱이 실행 중이지 않거나 설치되지 않았습니다. Shizuku를 먼저 실행해주세요.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
