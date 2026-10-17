package com.gosms.gateway.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gosms.gateway.R
import com.gosms.gateway.ui.components.PrimaryAppButton
import com.gosms.gateway.ui.components.SecondaryAppButton
import com.gosms.gateway.ui.scanner.QrScannerView
import com.gosms.gateway.ui.theme.AppTheme
import com.gosms.gateway.ui.viewmodel.ConnectUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    uiState: ConnectUiState,
    onScanQrClick: () -> Unit,
    onEnterManuallyClick: () -> Unit,
    onManualSheetDismiss: () -> Unit,
    onScannerBack: () -> Unit,
    onConnectSubmit: (serverUrl: String, pairingCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isScannerOpen) {
        QrScannerView(
            onQrDetected = { url, code ->
                onConnectSubmit(url, code)
            },
            onBack = onScannerBack
        )
        return
    }

    var serverUrl by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppTheme.colors.primaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = stringResource(R.string.logo_description),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.connect_title),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.connect_subtitle),
                style = AppTheme.typography.body,
                color = AppTheme.colors.onSurfaceMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            PrimaryAppButton(
                text = stringResource(R.string.scan_qr),
                onClick = onScanQrClick,
                icon = Icons.Rounded.QrCodeScanner
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryAppButton(
                text = stringResource(R.string.enter_manually),
                onClick = onEnterManuallyClick,
                icon = Icons.Rounded.Keyboard
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.qr_hint),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.onSurfaceMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (uiState.isManualSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = onManualSheetDismiss,
                sheetState = sheetState,
                containerColor = AppTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enter_manually),
                        style = AppTheme.typography.title,
                        color = AppTheme.colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text(stringResource(R.string.server_url)) },
                        placeholder = { Text(stringResource(R.string.server_url_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { if (it.length <= 6) pairingCode = it },
                        label = { Text(stringResource(R.string.pairing_code)) },
                        placeholder = { Text(stringResource(R.string.pairing_code_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.outline
                        )
                    )

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage,
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.error
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PrimaryAppButton(
                        text = stringResource(R.string.connect_button),
                        onClick = { onConnectSubmit(serverUrl, pairingCode) },
                        loading = uiState.isConnecting,
                        enabled = serverUrl.isNotBlank() && pairingCode.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
