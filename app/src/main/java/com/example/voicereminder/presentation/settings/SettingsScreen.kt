package com.example.voicereminder.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.data.settings.AIProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.saveSettings()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Settings saved!")
                        }
                    }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider Selection
            Text(
                text = "AI Provider",
                style = MaterialTheme.typography.titleMedium
            )
            
            AIProvider.values().forEach { provider ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.provider == provider,
                        onClick = { viewModel.updateProvider(provider) }
                    )
                    Text(
                        text = provider.name,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            Divider()
            
            // Provider-specific settings
            when (settings.provider) {
                AIProvider.OLLAMA -> OllamaSettings(settings, viewModel) { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
                AIProvider.OPENAI -> OpenAISettings(settings, viewModel)
                AIProvider.ANTHROPIC -> AnthropicSettings(settings, viewModel)
                AIProvider.CUSTOM -> CustomSettings(settings, viewModel)
            }
            
            // Test Connection Button
            Button(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isTesting) "Testing..." else "Test Connection")
            }
            
            // Test Result
            testResult?.let { (success, message) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (success) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (success) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message)
                    }
                }
            }
            
            Divider()
            
            // System Prompt
            Text(
                text = "System Prompt (Advanced)",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = settings.systemPrompt,
                onValueChange = { viewModel.updateSystemPrompt(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                label = { Text("System Prompt") },
                maxLines = 10
            )
            
            // Reset Button
            OutlinedButton(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
fun OllamaSettings(
    settings: com.example.voicereminder.data.settings.AISettings,
    viewModel: SettingsViewModel,
    onShowMessage: (String) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Ollama Bridge Configuration",
            style = MaterialTheme.typography.titleMedium
        )
        
        // Connection Mode Selection
        Text(
            text = "Connection Mode",
            style = MaterialTheme.typography.titleSmall
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = settings.ollamaConnectionMode == com.example.voicereminder.data.settings.OllamaConnectionMode.WIFI,
                onClick = { 
                    viewModel.updateOllamaConnectionMode(com.example.voicereminder.data.settings.OllamaConnectionMode.WIFI)
                    viewModel.saveSettings()
                    onShowMessage("Switched to WiFi mode - Settings saved!")
                }
            )
            Text(
                text = "WiFi (Same Network)",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = settings.ollamaConnectionMode == com.example.voicereminder.data.settings.OllamaConnectionMode.ADB_REVERSE,
                onClick = { 
                    viewModel.updateOllamaConnectionMode(com.example.voicereminder.data.settings.OllamaConnectionMode.ADB_REVERSE)
                    viewModel.saveSettings()
                    onShowMessage("Switched to ADB Reverse mode - Settings saved!")
                }
            )
            Text(
                text = "ADB Reverse (USB)",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show WiFi IP field only in WiFi mode
        if (settings.ollamaConnectionMode == com.example.voicereminder.data.settings.OllamaConnectionMode.WIFI) {
            OutlinedTextField(
                value = settings.ollamaWifiIp,
                onValueChange = { viewModel.updateOllamaWifiIp(it) },
                label = { Text("PC IP Address") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("192.168.1.100") },
                supportingText = {
                    Text("Your PC's WiFi IP address (run 'ipconfig' on Windows)")
                }
            )
        }
        
        OutlinedTextField(
            value = settings.ollamaModel,
            onValueChange = { viewModel.updateOllamaModel(it) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("llama3.1:latest") }
        )
        
        // Connection instructions card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (settings.ollamaConnectionMode == com.example.voicereminder.data.settings.OllamaConnectionMode.WIFI) 
                        "WiFi Setup Instructions" 
                    else 
                        "ADB Reverse Setup Instructions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (settings.ollamaConnectionMode == com.example.voicereminder.data.settings.OllamaConnectionMode.WIFI) {
                    Text(
                        text = "1. Connect both PC and device to same WiFi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "2. Find PC IP: Run 'ipconfig' on Windows",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "3. Enter IP above (e.g., 192.168.1.100)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "4. Start Ollama Bridge on PC (port 8080)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "5. Allow port 8080 in Windows Firewall",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📱 URL: http://${settings.ollamaWifiIp}:8080/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "1. Connect device via USB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "2. Run: adb reverse tcp:8080 tcp:8080",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "3. Start Ollama Bridge on PC (port 8080)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "4. No firewall configuration needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📱 URL: http://localhost:8080/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "💡 ADB Reverse is easier (no WiFi/firewall issues)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OpenAISettings(
    settings: com.example.voicereminder.data.settings.AISettings,
    viewModel: SettingsViewModel
) {
    var showApiKey by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "OpenAI Configuration",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = settings.openaiApiKey,
            onValueChange = { viewModel.updateOpenAIApiKey(it) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showApiKey) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle visibility"
                    )
                }
            }
        )
        
        OutlinedTextField(
            value = settings.openaiModel,
            onValueChange = { viewModel.updateOpenAIModel(it) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("gpt-4") }
        )
    }
}

@Composable
fun AnthropicSettings(
    settings: com.example.voicereminder.data.settings.AISettings,
    viewModel: SettingsViewModel
) {
    var showApiKey by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Anthropic Configuration",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = settings.anthropicApiKey,
            onValueChange = { viewModel.updateAnthropicApiKey(it) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showApiKey) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle visibility"
                    )
                }
            }
        )
        
        OutlinedTextField(
            value = settings.anthropicModel,
            onValueChange = { viewModel.updateAnthropicModel(it) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("claude-3-5-sonnet-20241022") }
        )
    }
}

@Composable
fun CustomSettings(
    settings: com.example.voicereminder.data.settings.AISettings,
    viewModel: SettingsViewModel
) {
    var showApiKey by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Custom API Configuration",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = settings.customBaseUrl,
            onValueChange = { viewModel.updateCustomBaseUrl(it) },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://api.example.com") }
        )
        
        OutlinedTextField(
            value = settings.customApiKey,
            onValueChange = { viewModel.updateCustomApiKey(it) },
            label = { Text("API Key (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showApiKey) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle visibility"
                    )
                }
            }
        )
        
        OutlinedTextField(
            value = settings.customModel,
            onValueChange = { viewModel.updateCustomModel(it) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Custom API must be OpenAI-compatible (e.g., LM Studio, LocalAI)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
