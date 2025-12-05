package com.example.voicereminder.presentation.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Message with long-press action support
 */
@Composable
fun MessageWithActions(
    message: String,
    isUser: Boolean,
    onRetry: (() -> Unit)? = null,
    onEdit: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        menuOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                        showMenu = true
                    }
                )
            }
    ) {
        content()
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = menuOffset
        ) {
            // Copy
            DropdownMenuItem(
                text = { Text("Copy") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp)) },
                onClick = {
                    copyToClipboard(context, message)
                    showMenu = false
                }
            )
            
            // Retry (AI messages only)
            if (!isUser && onRetry != null) {
                DropdownMenuItem(
                    text = { Text("Retry") },
                    leadingIcon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        onRetry()
                        showMenu = false
                    }
                )
            }
            
            // Edit & Resend (User messages only)
            if (isUser && onEdit != null) {
                DropdownMenuItem(
                    text = { Text("Edit & Resend") },
                    leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        onEdit(message)
                        showMenu = false
                    }
                )
            }
            
            // Share
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp)) },
                onClick = {
                    shareText(context, message)
                    showMenu = false
                }
            )
            
            // Delete
            if (onDelete != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Delete, 
                            null, 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        ) 
                    },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

/**
 * Copy text to clipboard
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

/**
 * Share text via system share sheet
 */
private fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share message")
    context.startActivity(shareIntent)
}

/**
 * Confirmation dialog for delete action
 */
@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Message") },
        text = { Text("Are you sure you want to delete this message?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Message status indicator
 */
@Composable
fun MessageStatusIndicator(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.SENT -> Icons.Default.Check to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to MaterialTheme.colorScheme.primary
        MessageStatus.ERROR -> Icons.Default.Error to MaterialTheme.colorScheme.error
        MessageStatus.QUEUED_OFFLINE -> Icons.Default.CloudOff to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = modifier.size(14.dp),
        tint = tint
    )
}

/**
 * Message status enum
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    ERROR,
    QUEUED_OFFLINE
}
