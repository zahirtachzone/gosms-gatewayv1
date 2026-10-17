package com.gosms.gateway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gosms.gateway.R
import com.gosms.gateway.service.GatewayConnectionState
import com.gosms.gateway.ui.theme.AppTheme

@Composable
fun StatusPill(
    state: GatewayConnectionState,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, textRes) = when (state) {
        GatewayConnectionState.ACTIVE -> Triple(
            AppTheme.colors.successContainer,
            AppTheme.colors.success,
            R.string.status_active
        )
        GatewayConnectionState.RECONNECTING -> Triple(
            AppTheme.colors.warningContainer,
            AppTheme.colors.warning,
            R.string.status_reconnecting
        )
        GatewayConnectionState.DISCONNECTED -> Triple(
            AppTheme.colors.errorContainer,
            AppTheme.colors.error,
            R.string.status_disconnected
        )
    }

    Box(
        modifier = modifier
            .height(28.dp)
            .background(containerColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(contentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(textRes),
                style = AppTheme.typography.caption,
                color = contentColor
            )
        }
    }
}
