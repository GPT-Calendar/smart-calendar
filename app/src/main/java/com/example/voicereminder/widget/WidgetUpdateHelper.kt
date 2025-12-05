package com.example.voicereminder.widget

import android.content.Context

/**
 * Helper class to manage widget updates across the app
 * Call this when reminders/tasks are created, updated, or deleted
 */
object WidgetUpdateHelper {
    
    /**
     * Refresh all widgets with latest data
     * Call this after any reminder/task changes
     */
    fun refreshAllWidgets(context: Context) {
        // Refresh the simple widgets (most important)
        NextUpWidget.refresh(context)
        
        // Refresh chat widget
        ChatWidgetProvider.resetWidget(context)
    }
    
    /**
     * Update widgets with AI response
     */
    fun updateAIResponse(context: Context, response: String) {
        ChatWidgetProvider.updateWidgetResponse(context, response)
    }
    
    /**
     * Show processing status on widgets
     */
    fun showProcessing(context: Context, message: String = "Processing...") {
        ChatWidgetProvider.updateWidgetResponse(context, "⏳ $message")
    }
    
    /**
     * Show error on widgets
     */
    fun showError(context: Context, error: String) {
        ChatWidgetProvider.updateWidgetResponse(context, "❌ $error")
    }
}
