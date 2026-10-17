package com.gosms.gateway.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gosms.gateway.R
import com.gosms.gateway.data.db.SmsJob
import com.gosms.gateway.data.model.SimInfo
import com.gosms.gateway.service.GatewayConnectionState
import com.gosms.gateway.ui.components.DangerTextButton
import com.gosms.gateway.ui.components.EmptyStateView
import com.gosms.gateway.ui.components.LogItemView
import com.gosms.gateway.ui.components.SecondaryAppButton
import com.gosms.gateway.ui.components.SimCardView
import com.gosms.gateway.ui.components.StatTile
import com.gosms.gateway.ui.components.StatType
import com.gosms.gateway.ui.components.StatusPill
import com.gosms.gateway.ui.theme.AppTheme

@Composable
fun MainScreen(
    connectionState: GatewayConnectionState,
    lastHeartbeatTimestamp: Long,
    sentTodayCount: Int,
    failedTodayCount: Int,
    pendingCount: Int,
    sims: List<SimInfo>,
    recentJobs: List<SmsJob>,
    autoStart: Boolean,
    defaultSimSlot: Int?,
    pollIntervalMs: Long,
    onAutoStartChange: (Boolean) -> Unit,
    onDefaultSimSlotChange: (Int?) -> Unit,
    onPollIntervalChange: (Long) -> Unit,
    onBatteryGuideClick: () -> Unit,
    onSendTestSmsClick: () -> Unit,
    onUnpairClick: () -> Unit,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var defaultSimMenuExpanded by remember { mutableStateOf(false) }
    var pollMenuExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by if (connectionState == GatewayConnectionState.ACTIVE) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val (heroRingColor, heroIcon, heroTitleRes) = when (connectionState) {
        GatewayConnectionState.ACTIVE -> Triple(
            AppTheme.colors.success,
            Icons.Rounded.Check,
            R.string.status_active
        )
        GatewayConnectionState.RECONNECTING -> Triple(
            AppTheme.colors.warning,
            Icons.Rounded.Refresh,
            R.string.status_reconnecting
        )
        GatewayConnectionState.DISCONNECTED -> Triple(
            AppTheme.colors.error,
            Icons.Rounded.Close,
            R.string.status_disconnected
        )
    }

    val heartbeatText = if (lastHeartbeatTimestamp > 0) {
        val diffSec = ((System.currentTimeMillis() - lastHeartbeatTimestamp) / 1000).coerceAtLeast(0)
        if (diffSec < 5) {
            stringResource(R.string.time_just_now)
        } else if (diffSec < 60) {
            stringResource(R.string.time_seconds_ago, diffSec)
        } else {
            stringResource(R.string.time_minutes_ago, diffSec / 60)
        }
    } else {
        stringResource(R.string.time_never)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLogoClick() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = stringResource(R.string.logo_description),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = AppTheme.typography.title,
                        color = AppTheme.colors.onSurface
                    )
                }

                StatusPill(state = connectionState)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AppTheme.colors.outline),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .border(4.dp, heroRingColor, CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .background(heroRingColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = heroIcon,
                                contentDescription = stringResource(R.string.status_icon_description),
                                modifier = Modifier.size(56.dp),
                                tint = heroRingColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(heroTitleRes),
                        style = AppTheme.typography.title,
                        color = AppTheme.colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.last_heartbeat, heartbeatText),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.onSurfaceMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    title = stringResource(R.string.sent_today),
                    count = sentTodayCount,
                    type = StatType.SENT,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = stringResource(R.string.failed),
                    count = failedTodayCount,
                    type = StatType.FAILED,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = stringResource(R.string.pending),
                    count = pendingCount,
                    type = StatType.PENDING,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = stringResource(R.string.sim_cards),
                style = AppTheme.typography.subtitle,
                color = AppTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sims.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.outline),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    EmptyStateView(
                        icon = Icons.Rounded.SimCard,
                        title = stringResource(R.string.no_sim_detected)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            itemsIndexed(sims) { index, sim ->
                SimCardView(
                    sim = sim,
                    isDefault = defaultSimSlot == sim.simSlot
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = AppTheme.typography.subtitle,
                color = AppTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AppTheme.colors.outline),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = stringResource(R.string.settings_icon_description),
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.start_on_boot),
                                style = AppTheme.typography.subtitle,
                                color = AppTheme.colors.onSurface
                            )
                            Text(
                                text = stringResource(R.string.start_on_boot_desc),
                                style = AppTheme.typography.caption,
                                color = AppTheme.colors.onSurfaceMuted
                            )
                        }
                        Switch(
                            checked = autoStart,
                            onCheckedChange = onAutoStartChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppTheme.colors.onPrimary,
                                checkedTrackColor = AppTheme.colors.primary,
                                uncheckedBorderColor = AppTheme.colors.outline
                            )
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.outline)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { defaultSimMenuExpanded = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SimCard,
                                contentDescription = stringResource(R.string.settings_icon_description),
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.default_sim),
                                    style = AppTheme.typography.subtitle,
                                    color = AppTheme.colors.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.default_sim_desc),
                                    style = AppTheme.typography.caption,
                                    color = AppTheme.colors.onSurfaceMuted
                                )
                            }
                            Text(
                                text = if (defaultSimSlot != null) "SIM $defaultSimSlot" else stringResource(R.string.sim_not_specified),
                                style = AppTheme.typography.subtitle,
                                color = AppTheme.colors.primary
                            )
                        }

                        DropdownMenu(
                            expanded = defaultSimMenuExpanded,
                            onDismissRequest = { defaultSimMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sim_not_specified)) },
                                onClick = {
                                    onDefaultSimSlotChange(null)
                                    defaultSimMenuExpanded = false
                                }
                            )
                            sims.forEach { sim ->
                                DropdownMenuItem(
                                    text = { Text("SIM ${sim.simSlot} (${sim.carrierName})") },
                                    onClick = {
                                        onDefaultSimSlotChange(sim.simSlot)
                                        defaultSimMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.outline)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pollMenuExpanded = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = stringResource(R.string.settings_icon_description),
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.poll_interval),
                                    style = AppTheme.typography.subtitle,
                                    color = AppTheme.colors.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.poll_interval_desc),
                                    style = AppTheme.typography.caption,
                                    color = AppTheme.colors.onSurfaceMuted
                                )
                            }
                            Text(
                                text = "${pollIntervalMs / 1000}s",
                                style = AppTheme.typography.subtitle,
                                color = AppTheme.colors.primary
                            )
                        }

                        DropdownMenu(
                            expanded = pollMenuExpanded,
                            onDismissRequest = { pollMenuExpanded = false }
                        ) {
                            listOf(3000L, 5000L, 10000L, 30000L).forEach { ms ->
                                DropdownMenuItem(
                                    text = { Text("${ms / 1000}s") },
                                    onClick = {
                                        onPollIntervalChange(ms)
                                        pollMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.outline)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBatteryGuideClick() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryAlert,
                            contentDescription = stringResource(R.string.settings_icon_description),
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.battery_optimization),
                                style = AppTheme.typography.subtitle,
                                color = AppTheme.colors.onSurface
                            )
                            Text(
                                text = stringResource(R.string.battery_optimization_desc),
                                style = AppTheme.typography.caption,
                                color = AppTheme.colors.onSurfaceMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = stringResource(R.string.actions_title),
                style = AppTheme.typography.subtitle,
                color = AppTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryAppButton(
                    text = stringResource(R.string.send_test_sms),
                    onClick = onSendTestSmsClick,
                    modifier = Modifier.weight(1f)
                )
                DangerTextButton(
                    text = stringResource(R.string.unpair),
                    onClick = onUnpairClick,
                    modifier = Modifier.weight(0.6f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = stringResource(R.string.recent_activity),
                style = AppTheme.typography.subtitle,
                color = AppTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (recentJobs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.outline),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    EmptyStateView(
                        icon = Icons.Rounded.HourglassEmpty,
                        title = stringResource(R.string.no_messages_yet),
                        description = stringResource(R.string.no_messages_desc)
                    )
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.outline),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        recentJobs.forEachIndexed { index, job ->
                            LogItemView(job = job)
                            if (index < recentJobs.size - 1) {
                                HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.outline)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
