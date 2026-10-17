package com.gosms.gateway

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.gosms.gateway.ui.dialogs.AboutDialog
import com.gosms.gateway.ui.dialogs.BatteryGuideDialog
import com.gosms.gateway.ui.dialogs.RestrictedSettingsGuideDialog
import com.gosms.gateway.ui.dialogs.TestSmsDialog
import com.gosms.gateway.ui.dialogs.UnpairConfirmDialog
import com.gosms.gateway.ui.screens.ConnectScreen
import com.gosms.gateway.ui.screens.MainScreen
import com.gosms.gateway.ui.theme.AppTheme
import com.gosms.gateway.ui.theme.GoSMSGatewayTheme
import com.gosms.gateway.ui.viewmodel.GatewayViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GatewayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GoSMSGatewayTheme {
                GatewayApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GatewayApp(viewModel: GatewayViewModel) {
    val isPaired by viewModel.isPaired.collectAsState()
    val connectUiState by viewModel.connectUiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val lastHeartbeat by viewModel.lastHeartbeatTimestamp.collectAsState()
    val sentToday by viewModel.sentTodayCount.collectAsState()
    val failedToday by viewModel.failedTodayCount.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()
    val sims by viewModel.sims.collectAsState()
    val recentJobs by viewModel.recentJobs.collectAsState()
    val autoStart by viewModel.autoStart.collectAsState()
    val defaultSimSlot by viewModel.defaultSimSlot.collectAsState()
    val pollIntervalMs by viewModel.pollIntervalMs.collectAsState()

    var showTestSmsDialog by remember { mutableStateOf(false) }
    var showUnpairDialog by remember { mutableStateOf(false) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }
    var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setScannerOpen(true)
        }
    }

    val generalPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val smsGranted = results[Manifest.permission.SEND_SMS] ?: false
        if (!smsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showRestrictedSettingsDialog = true
        }
        viewModel.refreshSims()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        generalPermissionsLauncher.launch(permissions.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
    ) {
        Crossfade(
            targetState = isPaired,
            animationSpec = tween(200),
            label = "screen_crossfade"
        ) { paired ->
            if (paired) {
                MainScreen(
                    connectionState = connectionState,
                    lastHeartbeatTimestamp = lastHeartbeat,
                    sentTodayCount = sentToday,
                    failedTodayCount = failedToday,
                    pendingCount = pending,
                    sims = sims,
                    recentJobs = recentJobs,
                    autoStart = autoStart,
                    defaultSimSlot = defaultSimSlot,
                    pollIntervalMs = pollIntervalMs,
                    onAutoStartChange = { viewModel.updateAutoStart(it) },
                    onDefaultSimSlotChange = { viewModel.updateDefaultSimSlot(it) },
                    onPollIntervalChange = { viewModel.updatePollInterval(it) },
                    onBatteryGuideClick = { showBatteryGuideDialog = true },
                    onSendTestSmsClick = { showTestSmsDialog = true },
                    onUnpairClick = { showUnpairDialog = true },
                    onLogoClick = { showAboutDialog = true }
                )
            } else {
                ConnectScreen(
                    uiState = connectUiState,
                    onScanQrClick = {
                        val hasCamera = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCamera) {
                            viewModel.setScannerOpen(true)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onEnterManuallyClick = { viewModel.setManualSheetOpen(true) },
                    onManualSheetDismiss = { viewModel.setManualSheetOpen(false) },
                    onScannerBack = { viewModel.setScannerOpen(false) },
                    onConnectSubmit = { url, code -> viewModel.register(url, code) }
                )
            }
        }

        if (showTestSmsDialog) {
            TestSmsDialog(
                sims = sims,
                defaultSlot = defaultSimSlot,
                onDismiss = { showTestSmsDialog = false },
                onSend = { recipient, message, slot ->
                    showTestSmsDialog = false
                    viewModel.sendTestSms(recipient, message, slot) {}
                }
            )
        }

        if (showUnpairDialog) {
            UnpairConfirmDialog(
                onDismiss = { showUnpairDialog = false },
                onConfirm = {
                    showUnpairDialog = false
                    viewModel.unpair()
                }
            )
        }

        if (showBatteryGuideDialog) {
            BatteryGuideDialog(onDismiss = { showBatteryGuideDialog = false })
        }

        if (showRestrictedSettingsDialog) {
            RestrictedSettingsGuideDialog(onDismiss = { showRestrictedSettingsDialog = false })
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}
