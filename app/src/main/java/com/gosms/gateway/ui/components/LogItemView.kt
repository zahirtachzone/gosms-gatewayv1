package com.gosms.gateway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gosms.gateway.R
import com.gosms.gateway.data.db.SmsJob
import com.gosms.gateway.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogItemView(
    job: SmsJob,
    modifier: Modifier = Modifier
) {
    val (chipBg, chipText) = when (job.state) {
        "SENT", "DELIVERED" -> Pair(AppTheme.colors.successContainer, AppTheme.colors.success)
        "FAILED" -> Pair(AppTheme.colors.errorContainer, AppTheme.colors.error)
        else -> Pair(AppTheme.colors.warningContainer, AppTheme.colors.warning)
    }

    val previewText = if (job.body.length > 40) {
        job.body.take(40) + "…"
    } else {
        job.body
    }

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(job.createdAt))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.recipient,
                style = AppTheme.typography.subtitle,
                color = AppTheme.colors.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = previewText,
                style = AppTheme.typography.body,
                color = AppTheme.colors.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .background(chipBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = job.state,
                    style = AppTheme.typography.caption,
                    color = chipText
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedTime,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.onSurfaceMuted
            )
        }
    }
}
