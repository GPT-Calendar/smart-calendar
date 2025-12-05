package com.example.voicereminder.presentation.finance.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.models.Budget
import com.example.voicereminder.domain.models.TransactionCategory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for creating or editing a budget
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    existingBudget: Budget? = null,
    onDismiss: () -> Unit,
    onConfirm: (category: TransactionCategory, limit: Double, month: String) -> Unit,
    onDelete: ((Budget) -> Unit)? = null
) {
    val isEditing = existingBudget != null
    
    var selectedCategory by remember { 
        mutableStateOf(existingBudget?.category ?: TransactionCategory.FOOD) 
    }
    var limitAmount by remember { 
        mutableStateOf(existingBudget?.monthlyLimit?.toString() ?: "") 
    }
    var selectedMonth by remember { 
        mutableStateOf(existingBudget?.month ?: getCurrentMonth()) 
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Validation
    var limitError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Budget" else "Set Budget",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = {
                            Icon(selectedCategory.getIcon(), contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isEditing // Can't change category when editing
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        TransactionCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            category.getIcon(), 
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(category.name.lowercase().replaceFirstChar { it.uppercase() })
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Budget Limit Input
                OutlinedTextField(
                    value = limitAmount,
                    onValueChange = { 
                        limitAmount = it.filter { char -> char.isDigit() || char == '.' }
                        limitError = null
                    },
                    label = { Text("Monthly Limit (AFN)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = limitError != null,
                    supportingText = limitError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Month Selector
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formatMonthDisplay(selectedMonth),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Month") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        getAvailableMonths().forEach { month ->
                            DropdownMenuItem(
                                text = { Text(formatMonthDisplay(month)) },
                                onClick = {
                                    selectedMonth = month
                                    monthExpanded = false
                                },
                                leadingIcon = if (selectedMonth == month) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
                
                // Delete button for editing
                if (isEditing && onDelete != null && existingBudget != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Budget")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitAmount.toDoubleOrNull()
                    if (limit == null || limit <= 0) {
                        limitError = "Please enter a valid amount"
                    } else {
                        onConfirm(selectedCategory, limit, selectedMonth)
                    }
                }
            ) {
                Text(if (isEditing) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Delete confirmation dialog
    if (showDeleteConfirm && existingBudget != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Budget?") },
            text = { 
                Text("Are you sure you want to delete the ${existingBudget.category.name.lowercase()} budget?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(existingBudget)
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

private fun getCurrentMonth(): String {
    val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    return formatter.format(Date())
}

private fun formatMonthDisplay(month: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(month)
        date?.let { outputFormat.format(it) } ?: month
    } catch (e: Exception) {
        month
    }
}

private fun getAvailableMonths(): List<String> {
    val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val months = mutableListOf<String>()
    
    // Current month and next 11 months
    repeat(12) {
        months.add(formatter.format(calendar.time))
        calendar.add(Calendar.MONTH, 1)
    }
    
    return months
}
