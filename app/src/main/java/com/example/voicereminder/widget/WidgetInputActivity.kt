package com.example.voicereminder.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.voicereminder.R
import java.util.Locale

/**
 * Transparent overlay activity for widget text/voice input
 * Shows a minimal input dialog without fully opening the app
 */
class WidgetInputActivity : Activity() {

    companion object {
        private const val VOICE_REQUEST_CODE = 100
        
        // Quick suggestion templates
        private val QUICK_SUGGESTIONS = listOf(
            "⏰ Remind me in 1 hour" to "Remind me in 1 hour to check this",
            "🌅 Morning alarm" to "Set alarm for tomorrow 7 AM",
            "📝 Add task" to "Add task: ",
            "💰 Finance" to "Show my spending summary"
        )
    }

    private lateinit var inputField: EditText
    private lateinit var suggestionsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_input)

        // Make it look like a dialog overlay
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Show keyboard automatically
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        inputField = findViewById(R.id.widget_input_field)
        suggestionsContainer = findViewById(R.id.widget_suggestions_container)
        val sendButton = findViewById<ImageButton>(R.id.widget_send_button)
        val voiceButton = findViewById<ImageButton>(R.id.widget_voice_input_button)
        val closeButton = findViewById<ImageButton>(R.id.widget_close_button)

        // Setup quick suggestions
        setupQuickSuggestions()

        // Check if started with voice input
        if (intent.getBooleanExtra("start_voice", false)) {
            startVoiceInput()
        } else {
            // Focus input field and show keyboard
            inputField.requestFocus()
        }

        // Handle keyboard send action
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val message = inputField.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessageToWidget(message)
                }
                true
            } else {
                false
            }
        }

        sendButton.setOnClickListener {
            val message = inputField.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessageToWidget(message)
            }
        }

        voiceButton.setOnClickListener {
            startVoiceInput()
        }

        closeButton.setOnClickListener {
            finish()
        }

        // Close when clicking outside
        findViewById<View>(R.id.widget_input_overlay).setOnClickListener {
            finish()
        }
        
        // Prevent clicks on the dialog from closing it
        findViewById<View>(R.id.widget_input_dialog)?.setOnClickListener { 
            // Do nothing - consume the click
        }
    }

    private fun setupQuickSuggestions() {
        suggestionsContainer.removeAllViews()
        
        for ((label, command) in QUICK_SUGGESTIONS) {
            val chip = TextView(this).apply {
                text = label
                setTextColor(0xFF5D83FF.toInt())
                textSize = 12f
                setPadding(24, 16, 24, 16)
                setBackgroundResource(R.drawable.widget_suggestion_chip)
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16
                }
                layoutParams = params
                
                setOnClickListener {
                    if (command.endsWith(": ")) {
                        // Partial command - put in input field
                        inputField.setText(command)
                        inputField.setSelection(command.length)
                        inputField.requestFocus()
                    } else {
                        // Complete command - send directly
                        sendMessageToWidget(command)
                    }
                }
            }
            suggestionsContainer.addView(chip)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your command...")
        }
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                sendMessageToWidget(spokenText)
            }
        }
    }

    private fun sendMessageToWidget(message: String) {
        // Send to service for processing
        ChatWidgetService.sendMessage(this, message)
        Toast.makeText(this, "Processing: $message", Toast.LENGTH_SHORT).show()
        finish()
    }
}
