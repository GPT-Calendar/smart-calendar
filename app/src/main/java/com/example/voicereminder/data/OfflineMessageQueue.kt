package com.example.voicereminder.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.voicereminder.data.entity.ChatMessageEntity
import com.example.voicereminder.presentation.chat.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages offline message queuing and network connectivity monitoring
 */
class OfflineMessageQueue(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val chatHistoryRepository = ChatHistoryRepository.getInstance(context)
    
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val _queuedMessageCount = MutableStateFlow(0)
    val queuedMessageCount: StateFlow<Int> = _queuedMessageCount.asStateFlow()
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var onMessageReadyToSend: (suspend (ChatMessageEntity) -> Boolean)? = null
    
    companion object {
        @Volatile
        private var INSTANCE: OfflineMessageQueue? = null
        
        fun getInstance(context: Context): OfflineMessageQueue {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineMessageQueue(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    init {
        checkInitialConnectivity()
        registerNetworkCallback()
        updateQueuedCount()
    }
    
    /**
     * Check initial network connectivity
     */
    private fun checkInitialConnectivity() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    /**
     * Register network callback for connectivity changes
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                // Try to send queued messages when network becomes available
                scope.launch {
                    processQueuedMessages()
                }
            }
            
            override fun onLost(network: Network) {
                // Check if there's still an active network
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            android.util.Log.e("OfflineMessageQueue", "Failed to register network callback", e)
        }
    }
    
    /**
     * Set callback for when messages are ready to send
     */
    fun setOnMessageReadyToSend(callback: suspend (ChatMessageEntity) -> Boolean) {
        onMessageReadyToSend = callback
    }
    
    /**
     * Queue a message for later sending
     */
    suspend fun queueMessage(message: ChatMessage): Long {
        val messageId = chatHistoryRepository.saveMessage(message, status = "QUEUED_OFFLINE")
        updateQueuedCount()
        return messageId
    }
    
    /**
     * Process queued messages when online
     */
    suspend fun processQueuedMessages() {
        if (!_isOnline.value) return
        
        val queuedMessages = chatHistoryRepository.getQueuedMessages()
        
        for (message in queuedMessages) {
            try {
                val success = onMessageReadyToSend?.invoke(message) ?: false
                if (success) {
                    chatHistoryRepository.updateMessageStatus(message.id, "SENT")
                } else {
                    // Keep in queue if sending failed
                    android.util.Log.w("OfflineMessageQueue", "Failed to send queued message: ${message.id}")
                }
            } catch (e: Exception) {
                android.util.Log.e("OfflineMessageQueue", "Error processing queued message", e)
            }
        }
        
        updateQueuedCount()
    }
    
    /**
     * Update queued message count
     */
    private fun updateQueuedCount() {
        scope.launch {
            val count = chatHistoryRepository.getQueuedMessages().size
            _queuedMessageCount.value = count
        }
    }
    
    /**
     * Check if device is currently online
     */
    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                android.util.Log.e("OfflineMessageQueue", "Failed to unregister network callback", e)
            }
        }
    }
}
