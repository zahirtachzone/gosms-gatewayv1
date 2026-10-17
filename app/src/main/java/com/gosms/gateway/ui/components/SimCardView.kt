package com.gosms.gateway.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gosms.gateway.R
import com.gosms.gateway.data.model.SimInfo
import com.gosms.gateway.ui.theme.AppTheme

@Composable
fun SimCardView(
    sim: SimInfo,
    isDefault: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppTheme.colors.outline),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppTheme.colors.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sim.simSlot.toString(),
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sim.carrierName,
                    style = AppTheme.typography.subtitle,
                    color = AppTheme.colors.onSurface
                )
                if (!sim.phoneNumber.isNullOrBlank()) {
                    Text(
                        text = sim.phoneNumber,
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.onSurfaceMuted
                    )
                }
            }

            if (isDefault) {
                Box(
                    modifier = Modifier
                        .background(
                            AppTheme.colors.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sim_default),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.onPrimaryContainer
                    )
                }
            }
        }
    }
}
