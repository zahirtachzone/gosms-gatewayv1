package com.gosms.gateway.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gosms.gateway.R
import com.gosms.gateway.data.model.SimInfo
import com.gosms.gateway.ui.theme.AppTheme

@Composable
fun TestSmsDialog(
    sims: List<SimInfo>,
    defaultSlot: Int?,
    onDismiss: () -> Unit,
    onSend: (recipient: String, message: String, slot: Int) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedSlot by remember { mutableIntStateOf(defaultSlot ?: sims.firstOrNull()?.simSlot ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = AppTheme.colors.surface,
        title = {
            Text(
                text = stringResource(R.string.send_test_sms),
                style = AppTheme.typography.title,
                color = AppTheme.colors.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(stringResource(R.string.test_sms_phone)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.test_sms_message)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.outline
                    )
                )

                if (sims.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.select_sim),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.onSurfaceMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sims.forEach { sim ->
                            val isSelected = sim.simSlot == selectedSlot
                            val bg = if (isSelected) AppTheme.colors.primaryContainer else AppTheme.colors.surface
                            val border = if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline
                            val textCol = if (isSelected) AppTheme.colors.onPrimaryContainer else AppTheme.colors.onSurface

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.dp, border, RoundedCornerShape(12.dp))
                                    .clickable { selectedSlot = sim.simSlot }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "SIM ${sim.simSlot} (${sim.carrierName})",
                                    style = AppTheme.typography.caption,
                                    color = textCol
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (recipient.isNotBlank() && message.isNotBlank()) {
                        onSend(recipient.trim(), message.trim(), selectedSlot)
                    }
                },
                enabled = recipient.isNotBlank() && message.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.test_sms_send),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.onSurfaceMuted
                )
            }
        }
    )
}

@Composable
fun UnpairConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = AppTheme.colors.surface,
        title = {
            Text(
                text = stringResource(R.string.unpair_confirm_title),
                style = AppTheme.typography.title,
                color = AppTheme.colors.onSurface
            )
        },
        text = {
            Text(
                text = stringResource(R.string.unpair_confirm_desc),
                style = AppTheme.typography.body,
                color = AppTheme.colors.onSurfaceMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.unpair),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.onSurfaceMuted
                )
            }
        }
    )
}

@Composable
fun BatteryGuideDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = AppTheme.colors.surface,
        title = {
            Text(
                text = stringResource(R.string.battery_guide_title),
                style = AppTheme.typography.title,
                color = AppTheme.colors.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.battery_guide_step1),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.battery_guide_step2),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.battery_guide_step3),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    openAppBatterySettings(context)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.open_settings),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.onSurfaceMuted
                )
            }
        }
    )
}

@Composable
fun RestrictedSettingsGuideDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = AppTheme.colors.surface,
        title = {
            Text(
                text = stringResource(R.string.restricted_guide_title),
                style = AppTheme.typography.title,
                color = AppTheme.colors.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.restricted_guide_step1),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.restricted_guide_step2),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.restricted_guide_step3),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.restricted_guide_step4),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    openAppDetailsSettings(context)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.open_app_info),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.onSurfaceMuted
                )
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = AppTheme.colors.surface,
        title = {
            Text(
                text = stringResource(R.string.about_title),
                style = AppTheme.typography.title,
                color = AppTheme.colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AppTheme.colors.primaryContainer, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = stringResource(R.string.logo_description),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.version),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.onSurfaceMuted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.primary
                )
            }
        }
    )
}

private fun openAppBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppDetailsSettings(context)
    }
}

private fun openAppDetailsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
