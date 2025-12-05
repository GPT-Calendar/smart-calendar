package com.example.voicereminder.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Connection status enum
 */
enum class ConnectionStatus {
    ONLINE,
    OFFLINE,
    CONNECTING,
    SYNCING
}

/**
 * Offline indicator banner
 */
@Composable
fun OfflineIndicatorBanner(
    isOffline: Boolean,
    queuedMessageCount: Int = 0,
    onRetryClick: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFF3E0) // Light orange background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "You're offline",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFFE65100)
                    )
                    if (queuedMessageCount > 0) {
                        Text(
                            text = "$queuedMessageCount message${if (queuedMessageCount > 1) "s" else ""} queued",
                            fontSize = 12.sp,
                            color = Color(0xFFBF360C)
                        )
                    }
                }
                
                TextButton(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFE65100)
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Syncing indicator
 */
@Composable
fun SyncingIndicator(
    isSyncing: Boolean,
    syncProgress: Float = 0f
) {
    AnimatedVisibility(
        visible = isSyncing,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE3F2FD) // Light blue background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1976D2)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Syncing messages...",
                    fontSize = 13.sp,
                    color = Color(0xFF1976D2)
                )
                
                if (syncProgress > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(syncProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}

/**
 * Connection status chip
 */
@Composable
fun ConnectionStatusChip(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (icon, text, backgroundColor, contentColor) = when (status) {
        ConnectionStatus.ONLINE -> Quadruple(
            Icons.Default.Cloud,
            "Online",
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32)
        )
        ConnectionStatus.OFFLINE -> Quadruple(
            Icons.Default.CloudOff,
            "Offline",
            Color(0xFFFFF3E0),
            Color(0xFFE65100)
        )
        ConnectionStatus.CONNECTING -> Quadruple(
            Icons.Default.CloudSync,
            "Connecting...",
            Color(0xFFE3F2FD),
            Color(0xFF1976D2)
        )
        ConnectionStatus.SYNCING -> Quadruple(
            Icons.Default.Sync,
            "Syncing...",
            Color(0xFFE3F2FD),
            Color(0xFF1976D2)
        )
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                color = contentColor
            )
        }
    }
}

/**
 * Queued message indicator badge
 */
@Composable
fun QueuedMessageBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF9800)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count queued",
                fontSize = 10.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Helper data class for quadruple values
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
