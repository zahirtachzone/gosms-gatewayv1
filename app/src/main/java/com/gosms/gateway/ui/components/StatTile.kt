package com.gosms.gateway.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gosms.gateway.ui.theme.AppTheme

enum class StatType {
    SENT,
    FAILED,
    PENDING
}

@Composable
fun StatTile(
    title: String,
    count: Int,
    type: StatType,
    modifier: Modifier = Modifier
) {
    val numberColor: Color = when (type) {
        StatType.SENT -> AppTheme.colors.onSurface
        StatType.FAILED -> if (count > 0) AppTheme.colors.error else AppTheme.colors.onSurfaceMuted
        StatType.PENDING -> if (count > 0) AppTheme.colors.warning else AppTheme.colors.onSurfaceMuted
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppTheme.colors.outline),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.onSurfaceMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = AppTheme.typography.number,
                color = numberColor
            )
        }
    }
}
