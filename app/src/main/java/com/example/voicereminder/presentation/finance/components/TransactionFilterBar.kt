package com.example.voicereminder.presentation.finance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.voicereminder.domain.models.DateRange
import com.example.voicereminder.domain.models.TransactionCategory
import com.example.voicereminder.domain.models.TransactionFilter
import com.example.voicereminder.domain.models.TransactionType

/**
 * Filter bar for transactions with date range, category, type, and search filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterBar(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(filter.searchQuery) }
    var showDateRangeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search bar (expandable)
        AnimatedVisibility(
            visible = showSearch,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { 
                    searchText = it
                    onFilterChange(filter.copy(searchQuery = it))
                },
                placeholder = { Text("Search transactions...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        searchText = ""
                        onFilterChange(filter.copy(searchQuery = ""))
                        showSearch = false
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )
            
            LaunchedEffect(showSearch) {
                if (showSearch) {
                    focusRequester.requestFocus()
                }
            }
        }
        
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search toggle
            if (!showSearch) {
                IconButton(onClick = { showSearch = true }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (filter.searchQuery.isNotBlank()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Date Range Filter
            Box {
                FilterChip(
                    selected = filter.dateRange != DateRange.ALL,
                    onClick = { showDateRangeMenu = true },
                    label = { Text(filter.dateRange.getDisplayName()) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                DropdownMenu(
                    expanded = showDateRangeMenu,
                    onDismissRequest = { showDateRangeMenu = false }
                ) {
                    DateRange.entries.filter { it != DateRange.CUSTOM }.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range.getDisplayName()) },
                            onClick = {
                                onFilterChange(filter.copy(dateRange = range))
                                showDateRangeMenu = false
                            },
                            leadingIcon = if (filter.dateRange == range) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
            
            // Type Filter Chips
            FilterChip(
                selected = filter.types.contains(TransactionType.SPENT),
                onClick = {
                    val newTypes = if (filter.types.contains(TransactionType.SPENT)) {
                        filter.types - TransactionType.SPENT
                    } else {
                        filter.types + TransactionType.SPENT
                    }
                    onFilterChange(filter.copy(types = newTypes))
                },
                label = { Text("Expenses") },
                leadingIcon = {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
            
            FilterChip(
                selected = filter.types.contains(TransactionType.RECEIVED),
                onClick = {
                    val newTypes = if (filter.types.contains(TransactionType.RECEIVED)) {
                        filter.types - TransactionType.RECEIVED
                    } else {
                        filter.types + TransactionType.RECEIVED
                    }
                    onFilterChange(filter.copy(types = newTypes))
                },
                label = { Text("Income") },
                leadingIcon = {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            )
            
            // Category Filter
            Box {
                FilterChip(
                    selected = filter.categories.isNotEmpty(),
                    onClick = { showCategoryMenu = true },
                    label = { 
                        Text(
                            if (filter.categories.isEmpty()) "Category" 
                            else "${filter.categories.size} selected"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    // Clear all option
                    if (filter.categories.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Clear all", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                onFilterChange(filter.copy(categories = emptySet()))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        )
                        HorizontalDivider()
                    }
                    
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
                                val newCategories = if (filter.categories.contains(category)) {
                                    filter.categories - category
                                } else {
                                    filter.categories + category
                                }
                                onFilterChange(filter.copy(categories = newCategories))
                            },
                            trailingIcon = if (filter.categories.contains(category)) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
            
            // Clear all filters
            if (filter.hasActiveFilters()) {
                IconButton(
                    onClick = { onFilterChange(filter.reset()) }
                ) {
                    Icon(
                        Icons.Default.FilterListOff,
                        contentDescription = "Clear filters",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
