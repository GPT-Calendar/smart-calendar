package com.example.voicereminder.presentation.finance.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.voicereminder.data.entity.SmsSource
import com.example.voicereminder.presentation.finance.ResponsiveUtils

private const val TAG = "SmsSourceManagement"

/**
 * UI component for managing SMS sources for automatic finance tracking.
 * Users can add phone numbers from banks/services to auto-track transactions.
 * AI extracts transaction data from SMS - no regex patterns needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSourceManagement(
    sources: List<SmsSource>,
    onAddSource: (String, String, String?) -> Unit,
    onUpdateSource: (SmsSource) -> Unit,
    onDeleteSource: (SmsSource) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "SmsSourceManagement composable entered with ${sources.size} sources")

    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val horizontalPadding = ResponsiveUtils.getHorizontalPadding()
    val majorSpacing = ResponsiveUtils.getMajorSpacing()
    val minorSpacing = ResponsiveUtils.getMinorSpacing()
    
    // Check if SMS permission is granted
    val hasSmsPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        hasSmsPermission.value = receiveGranted
        Log.d(TAG, "SMS permissions result: RECEIVE=$receiveGranted, READ=$readGranted")
        
        if (receiveGranted) {
            showAddDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    ) {
        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = minorSpacing)
        ) {
            Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Bank SMS Sources",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Permission warning if not granted
        if (!hasSmsPermission.value) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = minorSpacing),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SMS permission required to auto-track transactions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            smsPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                            )
                        }
                    ) {
                        Text("Grant", fontSize = 12.sp)
                    }
                }
            }
        }
        
        // Description
        Text(
            text = "Add phone numbers from banks or financial services. When SMS arrives from these numbers, AI will automatically extract and save transaction data.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = majorSpacing)
        )

        // Add Source Button
        Button(
            onClick = { 
                if (hasSmsPermission.value) {
                    showAddDialog = true
                } else {
                    // Request permission first
                    smsPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS
                        )
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = majorSpacing)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add Bank SMS Source")
        }

        // List of existing sources
        if (sources.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "No SMS sources configured yet.\nAdd your bank's SMS number to start auto-tracking transactions.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(minorSpacing)
            ) {
                items(sources) { source ->
                    SmsSourceItem(
                        source = source,
                        onUpdate = onUpdateSource,
                        onDelete = onDeleteSource
                    )
                }
            }
        }
    }

    // Add Source Dialog
    if (showAddDialog) {
        AddSmsSourceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phoneNumber, description ->
                onAddSource(name, phoneNumber, description)
                showAddDialog = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSmsSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bank SMS Source") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bank/Service Name") },
                    placeholder = { Text("e.g., Afghanistan International Bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("SMS Sender Number") },
                    placeholder = { Text("e.g., +93700123456 or AIB-BANK") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Main bank account alerts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                        onConfirm(name, phoneNumber, description.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank() && phoneNumber.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SmsSourceItem(
    source: SmsSource,
    onUpdate: (SmsSource) -> Unit,
    onDelete: (SmsSource) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = source.phoneNumber,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                source.description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Active status indicator
                Text(
                    text = if (source.isActive) "Active" else "Inactive",
                    fontSize = 11.sp,
                    color = if (source.isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        EditSmsSourceDialog(
            source = source,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedSource ->
                onUpdate(updatedSource)
                showEditDialog = false
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete SMS Source") },
            text = { Text("Are you sure you want to delete '${source.name}'? This will stop auto-tracking SMS from this number.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(source)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSmsSourceDialog(
    source: SmsSource,
    onDismiss: () -> Unit,
    onConfirm: (SmsSource) -> Unit
) {
    var name by remember { mutableStateOf(source.name) }
    var phoneNumber by remember { mutableStateOf(source.phoneNumber) }
    var description by remember { mutableStateOf(source.description ?: "") }
    var isActive by remember { mutableStateOf(source.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit SMS Source") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bank/Service Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("SMS Sender Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                        onConfirm(
                            source.copy(
                                name = name,
                                phoneNumber = phoneNumber,
                                description = description.ifBlank { null },
                                isActive = isActive
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && phoneNumber.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
