package com.example.voicereminder.domain

import com.example.voicereminder.data.LocationType
import com.example.voicereminder.data.PlaceCategory
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Enum representing the type of command
 */
enum class CommandType {
    REMINDER,  // Reminder with a message
    ALARM,     // Simple alarm without message
    TASK,      // To-do task
    RECURRING_REMINDER, // Recurring reminder
    RECURRING_ALARM,    // Recurring alarm
    SNOOZE,    // Snooze command
    COMPLETE_TASK // Mark task as complete
}

/**
 * Data class representing a parsed voice command
 */
data class ParsedCommand(
    val scheduledTime: LocalDateTime,
    val message: String,
    val type: CommandType = CommandType.REMINDER
)

/**
 * Data class representing a parsed location-based command
 */
data class ParsedLocationCommand(
    val message: String,
    val locationType: LocationType,
    val placeName: String? = null,
    val placeCategory: PlaceCategory? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Enhanced features
    val triggerOnExit: Boolean = false, // "when I leave"
    val isRecurring: Boolean = false, // "every time"
    val timeConstraintStart: Int? = null, // Start hour for hybrid reminders
    val timeConstraintEnd: Int? = null, // End hour for hybrid reminders
    val daysOfWeek: List<Int>? = null, // Days constraint (1=Mon, 7=Sun)
    val customRadius: Float? = null // User-specified radius
)

/**
 * Data class representing a parsed finance transaction command
 */
data class ParsedFinanceCommand(
    val amount: Double,
    val currency: String,
    val transactionType: String, // "DEBIT" or "CREDIT"
    val description: String
)

/**
 * Data class representing a parsed task command
 */
data class ParsedTaskCommand(
    val title: String,
    val description: String? = null,
    val dueDate: LocalDateTime? = null,
    val priority: String? = null, // "HIGH", "MEDIUM", "LOW"
    val category: String? = null
)

/**
 * Data class representing a parsed recurring command
 */
data class ParsedRecurringCommand(
    val scheduledTime: LocalDateTime,
    val message: String,
    val type: CommandType,
    val repeatDays: List<Int>? = null, // 1=Monday, 7=Sunday
    val recurrenceType: String? = null // "DAILY", "WEEKLY", "MONTHLY"
)

/**
 * Data class representing a parsed snooze command
 */
data class ParsedSnoozeCommand(
    val durationMinutes: Int
)

/**
 * Sealed class representing parsing result with specific error types
 */
sealed class ParseResult {
    data class Success(val command: ParsedCommand) : ParseResult()
    data class Error(val errorType: ParseErrorType, val message: String) : ParseResult()
}

/**
 * Enum representing different types of parsing errors
 */
enum class ParseErrorType {
    INVALID_TIME_FORMAT,
    PAST_TIME,
    NO_MESSAGE,
    INVALID_COMMAND
}

/**
 * Parser for natural language voice commands to extract reminder time and message
 */
class CommandParser {

    companion object {
        // Regex patterns for time extraction
        private val TIME_12HR_PATTERN = Regex(
            """(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)""",
            RegexOption.IGNORE_CASE
        )
        private val TIME_24HR_PATTERN = Regex(
            """(?:at\s+)?(\d{1,2}):(\d{2})"""
        )
        private val RELATIVE_TIME_PATTERN = Regex(
            """in\s+(\d+)\s+(minute|minutes|hour|hours)""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern to extract day of week
        private val DAY_OF_WEEK_PATTERN = Regex(
            """(?:on\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern to detect alarm commands
        private val ALARM_PATTERN = Regex(
            """(?:set\s+)?alarm(?:\s+at|\s+for)?""",
            RegexOption.IGNORE_CASE
        )
        
        // Pattern to extract the reminder message
        private val MESSAGE_PATTERN = Regex(
            """(?:remind me |reminder )?(?:at .+? |in .+? |on .+? )?to (.+)""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Task command patterns
        private val TASK_PATTERN = Regex(
            """(?:add|create|new)\s+task\s+(.+)""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Complete task pattern
        private val COMPLETE_TASK_PATTERN = Regex(
            """(?:mark|complete|finish|done)\s+(?:task\s+)?(.+?)(?:\s+as\s+done)?$""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Recurring alarm pattern - "alarm at 7am every weekday"
        private val RECURRING_ALARM_PATTERN = Regex(
            """(?:set\s+)?alarm(?:\s+at|\s+for)?\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\s+(?:every|on)\s+(weekdays?|weekends?|daily|every day|monday|tuesday|wednesday|thursday|friday|saturday|sunday)""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Recurring reminder pattern - "remind me every day at 9am to take medicine"
        private val RECURRING_REMINDER_PATTERN = Regex(
            """remind me\s+(.+?)\s+every\s+(day|week|month|weekday|weekend|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\s+at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Alternative recurring reminder - "remind me every day at 9am to..."
        private val RECURRING_REMINDER_ALT_PATTERN = Regex(
            """remind me\s+every\s+(day|week|month|weekday|weekend|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\s+at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\s+to\s+(.+)""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Snooze command pattern
        private val SNOOZE_PATTERN = Regex(
            """snooze(?:\s+for)?\s+(\d+)\s*(minutes?|mins?|hours?|hrs?)""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Show tasks pattern
        private val SHOW_TASKS_PATTERN = Regex(
            """(?:show|list|display|what are)\s+(?:my\s+)?tasks?""",
            RegexOption.IGNORE_CASE
        )
        
        // NEW: Priority pattern
        private val PRIORITY_PATTERN = Regex(
            """(?:with\s+)?(?:priority\s+)?(high|medium|low)(?:\s+priority)?""",
            RegexOption.IGNORE_CASE
        )
        
        // Location-based command patterns
        private val LOCATION_TRIGGER_PATTERN = Regex(
            """(?:when I (?:reach|get to|arrive at)|at (?:the |a )?|near (?:the |a )?|when I'm at)""",
            RegexOption.IGNORE_CASE
        )
        
        // Exit trigger patterns - "when I leave", "after I leave"
        private val EXIT_TRIGGER_PATTERN = Regex(
            """(?:when I (?:leave|exit|depart from)|after I (?:leave|exit)|leaving)""",
            RegexOption.IGNORE_CASE
        )
        
        // Recurring patterns - "every time", "always", "each time"
        private val RECURRING_PATTERN = Regex(
            """(?:every time|always|each time|whenever)""",
            RegexOption.IGNORE_CASE
        )
        
        // Time constraint patterns - "after 5 PM", "between 9 AM and 6 PM"
        private val TIME_CONSTRAINT_PATTERN = Regex(
            """(?:after|before|between)\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?(?:\s+and\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?)?""",
            RegexOption.IGNORE_CASE
        )
        
        // Day constraint patterns - "on weekdays", "on weekends"
        private val DAY_CONSTRAINT_PATTERN = Regex(
            """(?:on\s+)?(weekdays|weekends|monday|tuesday|wednesday|thursday|friday|saturday|sunday)""",
            RegexOption.IGNORE_CASE
        )
        
        // Radius pattern - "within 50 meters", "100m radius"
        private val RADIUS_PATTERN = Regex(
            """(?:within\s+)?(\d+)\s*(?:m|meters?|metre?s?)(?:\s+radius)?""",
            RegexOption.IGNORE_CASE
        )
        
        // Generic place categories - made more flexible to match with or without articles
        private val GENERIC_PLACE_PATTERN = Regex(
            """\b(?:a |any |the )?(store|shop|grocery|supermarket|pharmacy|gas station|restaurant|mall|market)s?\b""",
            RegexOption.IGNORE_CASE
        )
        
        // Specific named places
        private val NAMED_PLACE_PATTERN = Regex(
            """(home|work|office|gym)""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * Parse a voice command text to extract scheduled time and reminder message
     * 
     * @param text The transcribed voice command text
     * @return ParsedCommand if parsing succeeds, null if the command cannot be parsed
     */
    fun parseCommand(text: String): ParsedCommand? {
        val result = parseCommandWithError(text)
        return if (result is ParseResult.Success) result.command else null
    }

    /**
     * Parse a voice command text with detailed error information
     * 
     * @param text The transcribed voice command text
     * @return ParseResult with either success or specific error type
     */
    fun parseCommandWithError(text: String): ParseResult {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Check if this is an alarm command
        val isAlarm = ALARM_PATTERN.find(normalizedText) != null
        
        // Extract time
        val scheduledTime = extractTime(normalizedText)
        if (scheduledTime == null) {
            return ParseResult.Error(
                ParseErrorType.INVALID_TIME_FORMAT,
                "Invalid time format. Try saying \"at 2 PM\", \"at 14:00\", or \"in 30 minutes\"."
            )
        }
        
        // Validate that the scheduled time is in the future
        if (scheduledTime.isBefore(LocalDateTime.now()) || scheduledTime.isEqual(LocalDateTime.now())) {
            return ParseResult.Error(
                ParseErrorType.PAST_TIME,
                "The specified time has already passed. Please choose a future time."
            )
        }
        
        // For alarms, message is optional (use default)
        if (isAlarm) {
            val message = "Alarm"
            return ParseResult.Success(ParsedCommand(scheduledTime, message, CommandType.ALARM))
        }
        
        // For reminders, extract message
        val message = extractMessage(text)
        if (message == null) {
            return ParseResult.Error(
                ParseErrorType.NO_MESSAGE,
                "Could not extract reminder message. Try saying \"remind me to [your message]\"."
            )
        }
        
        return ParseResult.Success(ParsedCommand(scheduledTime, message, CommandType.REMINDER))
    }

    /**
     * Extract time from the command text
     * Supports formats: "at 2 PM", "at 14:00", "in 30 minutes", "on friday at 10 PM"
     */
    private fun extractTime(text: String): LocalDateTime? {
        // Extract day of week if specified
        val dayOfWeek = extractDayOfWeek(text)
        
        // Try 12-hour format (e.g., "at 2 PM", "2:30 pm")
        TIME_12HR_PATTERN.find(text)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase()
            
            // Validate hour and minute ranges
            if (hour !in 1..12 || minute !in 0..59) return null
            
            // Convert to 24-hour format
            val hour24 = when {
                amPm == "am" && hour == 12 -> 0
                amPm == "pm" && hour != 12 -> hour + 12
                else -> hour
            }
            
            return calculateScheduledTime(hour24, minute, dayOfWeek)
        }
        
        // Try 24-hour format (e.g., "at 14:00", "14:30")
        TIME_24HR_PATTERN.find(text)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            
            // Validate hour and minute ranges
            if (hour !in 0..23 || minute !in 0..59) return null
            
            return calculateScheduledTime(hour, minute, dayOfWeek)
        }
        
        // Try relative time format (e.g., "in 30 minutes", "in 2 hours")
        RELATIVE_TIME_PATTERN.find(text)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: return null
            val unit = match.groupValues[2].lowercase()
            
            return when {
                unit.startsWith("minute") -> LocalDateTime.now().plusMinutes(amount.toLong())
                unit.startsWith("hour") -> LocalDateTime.now().plusHours(amount.toLong())
                else -> null
            }
        }
        
        return null
    }

    /**
     * Extract day of week from the command text
     * Returns the day of week value (1=Monday, 7=Sunday) or null if not specified
     */
    private fun extractDayOfWeek(text: String): Int? {
        DAY_OF_WEEK_PATTERN.find(text)?.let { match ->
            val dayName = match.groupValues[1].lowercase()
            return when (dayName) {
                "monday" -> 1
                "tuesday" -> 2
                "wednesday" -> 3
                "thursday" -> 4
                "friday" -> 5
                "saturday" -> 6
                "sunday" -> 7
                else -> null
            }
        }
        return null
    }

    /**
     * Calculate the scheduled time for a given hour and minute
     * If dayOfWeek is specified, schedule for that day
     * Otherwise, if the time has already passed today, schedule for tomorrow
     */
    private fun calculateScheduledTime(hour: Int, minute: Int, dayOfWeek: Int? = null): LocalDateTime {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(hour, minute)
        var scheduledDateTime = now.with(targetTime)
        
        if (dayOfWeek != null) {
            // Calculate the target day
            val currentDayOfWeek = now.dayOfWeek.value
            var daysToAdd = dayOfWeek - currentDayOfWeek
            
            // If the target day is today but the time has passed, or target day is in the past this week
            if (daysToAdd < 0 || (daysToAdd == 0 && (scheduledDateTime.isBefore(now) || scheduledDateTime.isEqual(now)))) {
                daysToAdd += 7 // Schedule for next week
            }
            
            scheduledDateTime = scheduledDateTime.plusDays(daysToAdd.toLong())
        } else {
            // If the time has already passed today, schedule for tomorrow
            if (scheduledDateTime.isBefore(now) || scheduledDateTime.isEqual(now)) {
                scheduledDateTime = scheduledDateTime.plusDays(1)
            }
        }
        
        return scheduledDateTime
    }

    /**
     * Extract the reminder message from the command text
     */
    private fun extractMessage(text: String): String? {
        MESSAGE_PATTERN.find(text)?.let { match ->
            val message = match.groupValues[1].trim()
            return if (message.isNotEmpty()) message else null
        }
        
        return null
    }
    
    /**
     * Check if the command looks like a time-based command (reminder or alarm)
     * 
     * @param text The transcribed voice command text
     * @return true if the command contains time patterns or reminder keywords
     */
    fun looksLikeTimeCommand(text: String): Boolean {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Check for time patterns
        val hasTimePattern = TIME_12HR_PATTERN.find(normalizedText) != null ||
                            TIME_24HR_PATTERN.find(normalizedText) != null ||
                            RELATIVE_TIME_PATTERN.find(normalizedText) != null
        
        // Check for reminder/alarm keywords
        val hasReminderKeyword = normalizedText.contains("remind") ||
                                normalizedText.contains("reminder") ||
                                normalizedText.contains("alarm") ||
                                normalizedText.contains("schedule") ||
                                normalizedText.contains("set")
        
        return hasTimePattern || hasReminderKeyword
    }
    
    /**
     * Check if the command is a finance transaction command
     * 
     * @param text The transcribed voice command text
     * @return true if the command contains finance keywords
     */
    fun isFinanceCommand(text: String): Boolean {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Keywords for expenses (debit)
        val expenseKeywords = listOf(
            "spent", "paid", "bought", "purchased", "cost", "expense",
            "withdraw", "withdrawal", "cash out"
        )
        
        // Keywords for income (credit)
        val incomeKeywords = listOf(
            "received", "earned", "got", "income", "salary", "deposit",
            "credited", "payment received"
        )
        
        // Check for amount patterns (numbers with currency)
        val hasAmount = normalizedText.contains(Regex("""\d+\.?\d*\s*(birr|etb|dollar|usd)"""))
        
        // Check for finance keywords
        val hasFinanceKeyword = expenseKeywords.any { normalizedText.contains(it) } ||
                                incomeKeywords.any { normalizedText.contains(it) }
        
        return hasAmount && hasFinanceKeyword
    }
    
    /**
     * Parse a finance transaction command
     * 
     * @param text The transcribed voice command text
     * @return ParsedFinanceCommand if parsing succeeds, null otherwise
     */
    fun parseFinanceCommand(text: String): ParsedFinanceCommand? {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Extract amount
        val amountPattern = Regex("""(\d+\.?\d*)\s*(birr|etb|dollar|usd)""", RegexOption.IGNORE_CASE)
        val amountMatch = amountPattern.find(normalizedText) ?: return null
        val amount = amountMatch.groupValues[1].toDoubleOrNull() ?: return null
        val currency = amountMatch.groupValues[2].uppercase()
        
        // Determine transaction type
        val expenseKeywords = listOf("spent", "paid", "bought", "purchased", "cost", "expense", "withdraw", "withdrawal", "cash out")
        val incomeKeywords = listOf("received", "earned", "got", "income", "salary", "deposit", "credited", "payment received")
        
        val isExpense = expenseKeywords.any { normalizedText.contains(it) }
        val isIncome = incomeKeywords.any { normalizedText.contains(it) }
        
        val transactionType = when {
            isExpense -> "DEBIT"
            isIncome -> "CREDIT"
            else -> "DEBIT" // Default to expense if unclear
        }
        
        // Extract description
        val description = extractFinanceDescription(normalizedText, amountMatch.value)
        
        return ParsedFinanceCommand(
            amount = amount,
            currency = if (currency == "BIRR") "ETB" else currency,
            transactionType = transactionType,
            description = description
        )
    }
    
    /**
     * Extract description from finance command
     */
    private fun extractFinanceDescription(text: String, amountText: String): String {
        // Remove the amount part
        var description = text.replace(amountText, "").trim()
        
        // Remove common prefixes
        description = description
            .replace(Regex("""^(i |i've |i have )?"""), "")
            .replace(Regex("""(spent|paid|bought|purchased|received|earned|got|withdraw|withdrawal|deposit|credited)"""), "")
            .replace(Regex("""(on|for|from|to)"""), "")
            .trim()
        
        return if (description.isNotEmpty()) description else "Transaction"
    }
    
    /**
     * Check if the command is a location-based command
     * 
     * @param text The transcribed voice command text
     * @return true if the command contains location triggers, false otherwise
     */
    fun isLocationCommand(text: String): Boolean {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Check for location trigger words (enter)
        val hasLocationTrigger = LOCATION_TRIGGER_PATTERN.find(normalizedText) != null
        
        // Check for exit trigger words
        val hasExitTrigger = EXIT_TRIGGER_PATTERN.find(normalizedText) != null
        
        // Check for place keywords
        val hasGenericPlace = GENERIC_PLACE_PATTERN.find(normalizedText) != null
        val hasNamedPlace = NAMED_PLACE_PATTERN.find(normalizedText) != null
        val hasPlaceKeyword = hasGenericPlace || hasNamedPlace
        
        // Also check for common location phrases that might not match the strict pattern
        val hasLocationPhrase = normalizedText.contains("when i reach") ||
                                normalizedText.contains("when i get to") ||
                                normalizedText.contains("when i arrive") ||
                                normalizedText.contains("when i leave") ||
                                normalizedText.contains("after i leave") ||
                                normalizedText.contains("at the") ||
                                normalizedText.contains("at a ") ||
                                normalizedText.contains("near the") ||
                                normalizedText.contains("near a ")
        
        val isLocation = (hasLocationTrigger || hasLocationPhrase || hasExitTrigger) && hasPlaceKeyword
        
        // Log only the result for debugging
        if (isLocation) {
            android.util.Log.d("CommandParser", "✅ Location command detected: '$text'")
        }
        
        return isLocation
    }
    
    /**
     * Parse a location-based voice command to extract location details and reminder message
     * 
     * @param text The transcribed voice command text
     * @return ParsedLocationCommand if parsing succeeds, null if the command cannot be parsed
     */
    fun parseLocationCommand(text: String): ParsedLocationCommand? {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Check if this is a location command
        if (!isLocationCommand(text)) {
            return null
        }
        
        // Extract enhanced features
        val triggerOnExit = EXIT_TRIGGER_PATTERN.find(normalizedText) != null
        val isRecurring = RECURRING_PATTERN.find(normalizedText) != null
        val customRadius = extractRadius(normalizedText)
        val timeConstraints = extractTimeConstraints(normalizedText)
        val daysOfWeek = extractDaysOfWeek(normalizedText)
        
        // Try to extract generic place category
        GENERIC_PLACE_PATTERN.find(normalizedText)?.let { match ->
            val categoryText = match.groupValues[1].lowercase()
            val placeCategory = when (categoryText) {
                "store", "shop", "mall", "market" -> PlaceCategory.STORE
                "grocery", "supermarket" -> PlaceCategory.GROCERY
                "pharmacy" -> PlaceCategory.PHARMACY
                "gas station" -> PlaceCategory.GAS_STATION
                "restaurant" -> PlaceCategory.RESTAURANT
                else -> PlaceCategory.CUSTOM
            }
            
            // Extract the reminder message
            val message = extractLocationMessage(text, categoryText) ?: return null
            
            return ParsedLocationCommand(
                message = message,
                locationType = LocationType.GENERIC_CATEGORY,
                placeCategory = placeCategory,
                triggerOnExit = triggerOnExit,
                isRecurring = isRecurring,
                timeConstraintStart = timeConstraints?.first,
                timeConstraintEnd = timeConstraints?.second,
                daysOfWeek = daysOfWeek,
                customRadius = customRadius
            )
        }
        
        // Try to extract specific named place
        NAMED_PLACE_PATTERN.find(normalizedText)?.let { match ->
            val placeName = match.groupValues[1].lowercase()
            
            // Extract the reminder message
            val message = extractLocationMessage(text, placeName) ?: return null
            
            return ParsedLocationCommand(
                message = message,
                locationType = LocationType.SPECIFIC_PLACE,
                placeName = placeName,
                triggerOnExit = triggerOnExit,
                isRecurring = isRecurring,
                timeConstraintStart = timeConstraints?.first,
                timeConstraintEnd = timeConstraints?.second,
                daysOfWeek = daysOfWeek,
                customRadius = customRadius
            )
        }
        
        return null
    }
    
    /**
     * Extract custom radius from command text
     */
    private fun extractRadius(text: String): Float? {
        RADIUS_PATTERN.find(text)?.let { match ->
            val radius = match.groupValues[1].toFloatOrNull()
            if (radius != null && radius > 0 && radius <= 5000) {
                return radius
            }
        }
        return null
    }
    
    /**
     * Extract time constraints from command text
     * Returns Pair of (startHour, endHour) in 24-hour format
     */
    private fun extractTimeConstraints(text: String): Pair<Int, Int>? {
        TIME_CONSTRAINT_PATTERN.find(text)?.let { match ->
            val hour1 = match.groupValues[1].toIntOrNull() ?: return null
            val amPm1 = match.groupValues[3].lowercase()
            val hour2 = match.groupValues[4].toIntOrNull()
            val amPm2 = match.groupValues[6].lowercase()
            
            // Convert to 24-hour format
            val startHour = convertTo24Hour(hour1, amPm1)
            val endHour = if (hour2 != null) convertTo24Hour(hour2, amPm2) else null
            
            return if (endHour != null) {
                Pair(startHour, endHour)
            } else {
                // "after X PM" means from X to midnight
                Pair(startHour, 23)
            }
        }
        return null
    }
    
    /**
     * Convert 12-hour time to 24-hour format
     */
    private fun convertTo24Hour(hour: Int, amPm: String): Int {
        return when {
            amPm == "am" && hour == 12 -> 0
            amPm == "pm" && hour != 12 -> hour + 12
            amPm.isEmpty() && hour < 12 -> hour // Assume PM for afternoon hours
            else -> hour
        }
    }
    
    /**
     * Extract days of week constraint from command text
     */
    private fun extractDaysOfWeek(text: String): List<Int>? {
        DAY_CONSTRAINT_PATTERN.find(text)?.let { match ->
            val dayText = match.groupValues[1].lowercase()
            return when (dayText) {
                "weekdays" -> listOf(1, 2, 3, 4, 5) // Monday to Friday
                "weekends" -> listOf(6, 7) // Saturday and Sunday
                "monday" -> listOf(1)
                "tuesday" -> listOf(2)
                "wednesday" -> listOf(3)
                "thursday" -> listOf(4)
                "friday" -> listOf(5)
                "saturday" -> listOf(6)
                "sunday" -> listOf(7)
                else -> null
            }
        }
        return null
    }
    
    /**
     * Extract the reminder message from a location-based command
     * Handles patterns like "remind me to [message] when I reach [location]"
     * or "remind me to [message] at [location]"
     */
    private fun extractLocationMessage(text: String, locationKeyword: String): String? {
        val normalizedText = text.trim()
        
        // Pattern 1: "remind me to [message] when I reach/get to/arrive at [location]"
        val pattern1 = Regex(
            """(?:remind me |reminder )?to (.+?)(?:\s+when I (?:reach|get to|arrive at)|at (?:the |a )?|near (?:the |a )?)""",
            RegexOption.IGNORE_CASE
        )
        pattern1.find(normalizedText)?.let { match ->
            val message = match.groupValues[1].trim()
            if (message.isNotEmpty() && !message.equals(locationKeyword, ignoreCase = true)) {
                return message
            }
        }
        
        // Pattern 2: "when I reach/get to [location] remind me to [message]"
        val pattern2 = Regex(
            """(?:when I (?:reach|get to|arrive at)|at (?:the |a )?|near (?:the |a )?).+?(?:remind me |reminder )?to (.+)""",
            RegexOption.IGNORE_CASE
        )
        pattern2.find(normalizedText)?.let { match ->
            val message = match.groupValues[1].trim()
            if (message.isNotEmpty() && !message.equals(locationKeyword, ignoreCase = true)) {
                return message
            }
        }
        
        // Pattern 3: "remind me [message] when I reach [location]" (without "to")
        val pattern3 = Regex(
            """(?:remind me |reminder )(.+?)(?:\s+when I (?:reach|get to|arrive at)|at (?:the |a )?|near (?:the |a )?)""",
            RegexOption.IGNORE_CASE
        )
        pattern3.find(normalizedText)?.let { match ->
            val message = match.groupValues[1].trim()
            // Remove "to" if it's at the start
            val cleanMessage = message.removePrefix("to").trim()
            if (cleanMessage.isNotEmpty() && !cleanMessage.equals(locationKeyword, ignoreCase = true)) {
                return cleanMessage
            }
        }
        
        // Pattern 4: Simple extraction - everything before or after the location keyword
        // Remove common prefixes and the location part
        var cleanedText = normalizedText
            .replace(Regex("""(?:remind me |reminder )""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:when I (?:reach|get to|arrive at)|at (?:the |a )?|near (?:the |a )?)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b$locationKeyword\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b(?:a |any |the )\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\bto\b""", RegexOption.IGNORE_CASE), "")
            .trim()
        
        return if (cleanedText.isNotEmpty()) cleanedText else null
    }
    
    // ==================== NEW METHODS FOR TASKS, RECURRING, AND SNOOZE ====================
    
    /**
     * Check if the command is a task command
     */
    fun isTaskCommand(text: String): Boolean {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        return TASK_PATTERN.find(normalizedText) != null ||
               COMPLETE_TASK_PATTERN.find(normalizedText) != null ||
               SHOW_TASKS_PATTERN.find(normalizedText) != null
    }
    
    /**
     * Parse a task command
     * 
     * @param text The transcribed voice command text
     * @return ParsedTaskCommand if parsing succeeds, null otherwise
     */
    fun parseTaskCommand(text: String): ParsedTaskCommand? {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Try to match "add task [title]"
        TASK_PATTERN.find(normalizedText)?.let { match ->
            var title = match.groupValues[1].trim()
            
            // Extract priority if mentioned
            val priority = PRIORITY_PATTERN.find(title)?.let { priorityMatch ->
                title = title.replace(priorityMatch.value, "").trim()
                priorityMatch.groupValues[1].uppercase()
            }
            
            // Extract due date if mentioned (e.g., "by tomorrow", "due friday")
            val dueDate = extractTaskDueDate(title)
            if (dueDate != null) {
                title = removeTaskDueDateText(title)
            }
            
            if (title.isNotEmpty()) {
                return ParsedTaskCommand(
                    title = title.replaceFirstChar { it.uppercase() },
                    priority = priority,
                    dueDate = dueDate
                )
            }
        }
        
        return null
    }
    
    /**
     * Parse a complete task command
     * 
     * @param text The transcribed voice command text
     * @return The task title to mark as complete, or null
     */
    fun parseCompleteTaskCommand(text: String): String? {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        COMPLETE_TASK_PATTERN.find(normalizedText)?.let { match ->
            val taskTitle = match.groupValues[1].trim()
            if (taskTitle.isNotEmpty()) {
                return taskTitle
            }
        }
        
        return null
    }
    
    /**
     * Check if the command is a snooze command
     */
    fun isSnoozeCommand(text: String): Boolean {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        return SNOOZE_PATTERN.find(normalizedText) != null
    }
    
    /**
     * Parse a snooze command
     * 
     * @param text The transcribed voice command text
     * @return ParsedSnoozeCommand if parsing succeeds, null otherwise
     */
    fun parseSnoozeCommand(text: String): ParsedSnoozeCommand? {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        SNOOZE_PATTERN.find(normalizedText)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: return null
            val unit = match.groupValues[2].lowercase()
            
            val minutes = when {
                unit.startsWith("hour") || unit.startsWith("hr") -> amount * 60
                else -> amount // minutes
            }
            
            return ParsedSnoozeCommand(durationMinutes = minutes)
        }
        
        return null
    }
    
    /**
     * Check if the command is a recurring command
     */
    fun isRecurringCommand(text: String): Boolean {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        return RECURRING_ALARM_PATTERN.find(normalizedText) != null ||
               RECURRING_REMINDER_PATTERN.find(normalizedText) != null ||
               RECURRING_REMINDER_ALT_PATTERN.find(normalizedText) != null ||
               (normalizedText.contains("every") && 
                (normalizedText.contains("remind") || normalizedText.contains("alarm")))
    }
    
    /**
     * Parse a recurring alarm command
     * 
     * @param text The transcribed voice command text
     * @return ParsedRecurringCommand if parsing succeeds, null otherwise
     */
    fun parseRecurringAlarmCommand(text: String): ParsedRecurringCommand? {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        RECURRING_ALARM_PATTERN.find(normalizedText)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase()
            val repeatText = match.groupValues[4].lowercase()
            
            // Convert to 24-hour format
            val hour24 = when {
                amPm == "am" && hour == 12 -> 0
                amPm == "pm" && hour != 12 -> hour + 12
                amPm.isEmpty() && hour < 7 -> hour + 12 // Assume PM for early hours
                else -> hour
            }
            
            // Parse repeat days
            val repeatDays = parseRepeatDays(repeatText)
            
            // Calculate scheduled time
            val scheduledTime = calculateScheduledTime(hour24, minute, null)
            
            return ParsedRecurringCommand(
                scheduledTime = scheduledTime,
                message = "Alarm",
                type = CommandType.RECURRING_ALARM,
                repeatDays = repeatDays,
                recurrenceType = if (repeatDays?.size == 7) "DAILY" else "WEEKLY"
            )
        }
        
        return null
    }
    
    /**
     * Parse a recurring reminder command
     * 
     * @param text The transcribed voice command text
     * @return ParsedRecurringCommand if parsing succeeds, null otherwise
     */
    fun parseRecurringReminderCommand(text: String): ParsedRecurringCommand? {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        
        // Try alternative pattern first: "remind me every day at 9am to take medicine"
        RECURRING_REMINDER_ALT_PATTERN.find(normalizedText)?.let { match ->
            val repeatText = match.groupValues[1].lowercase()
            val hour = match.groupValues[2].toIntOrNull() ?: return null
            val minute = match.groupValues[3].toIntOrNull() ?: 0
            val amPm = match.groupValues[4].lowercase()
            val message = match.groupValues[5].trim()
            
            val hour24 = when {
                amPm == "am" && hour == 12 -> 0
                amPm == "pm" && hour != 12 -> hour + 12
                amPm.isEmpty() && hour < 7 -> hour + 12
                else -> hour
            }
            
            val repeatDays = parseRepeatDays(repeatText)
            val scheduledTime = calculateScheduledTime(hour24, minute, null)
            
            return ParsedRecurringCommand(
                scheduledTime = scheduledTime,
                message = message.replaceFirstChar { it.uppercase() },
                type = CommandType.RECURRING_REMINDER,
                repeatDays = repeatDays,
                recurrenceType = getRecurrenceType(repeatText)
            )
        }
        
        // Try original pattern: "remind me to take medicine every day at 9am"
        RECURRING_REMINDER_PATTERN.find(normalizedText)?.let { match ->
            val message = match.groupValues[1].trim()
            val repeatText = match.groupValues[2].lowercase()
            val hour = match.groupValues[3].toIntOrNull() ?: return null
            val minute = match.groupValues[4].toIntOrNull() ?: 0
            val amPm = match.groupValues[5].lowercase()
            
            val hour24 = when {
                amPm == "am" && hour == 12 -> 0
                amPm == "pm" && hour != 12 -> hour + 12
                amPm.isEmpty() && hour < 7 -> hour + 12
                else -> hour
            }
            
            val repeatDays = parseRepeatDays(repeatText)
            val scheduledTime = calculateScheduledTime(hour24, minute, null)
            
            return ParsedRecurringCommand(
                scheduledTime = scheduledTime,
                message = message.replaceFirstChar { it.uppercase() },
                type = CommandType.RECURRING_REMINDER,
                repeatDays = repeatDays,
                recurrenceType = getRecurrenceType(repeatText)
            )
        }
        
        return null
    }
    
    /**
     * Parse repeat days from text
     */
    private fun parseRepeatDays(text: String): List<Int>? {
        return when (text.lowercase()) {
            "daily", "every day", "day" -> listOf(1, 2, 3, 4, 5, 6, 7)
            "weekday", "weekdays" -> listOf(1, 2, 3, 4, 5)
            "weekend", "weekends" -> listOf(6, 7)
            "monday" -> listOf(1)
            "tuesday" -> listOf(2)
            "wednesday" -> listOf(3)
            "thursday" -> listOf(4)
            "friday" -> listOf(5)
            "saturday" -> listOf(6)
            "sunday" -> listOf(7)
            else -> null
        }
    }
    
    /**
     * Get recurrence type from text
     */
    private fun getRecurrenceType(text: String): String {
        return when (text.lowercase()) {
            "daily", "every day", "day" -> "DAILY"
            "week" -> "WEEKLY"
            "month" -> "MONTHLY"
            else -> "WEEKLY"
        }
    }
    
    /**
     * Extract due date from task text
     */
    private fun extractTaskDueDate(text: String): LocalDateTime? {
        val normalizedText = text.lowercase()
        
        // Check for "tomorrow"
        if (normalizedText.contains("tomorrow") || normalizedText.contains("by tomorrow")) {
            return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
        }
        
        // Check for "today"
        if (normalizedText.contains("today") || normalizedText.contains("by today")) {
            val now = LocalDateTime.now()
            return if (now.hour < 18) {
                now.withHour(18).withMinute(0)
            } else {
                now.plusHours(2)
            }
        }
        
        // Check for day of week
        val dayPattern = Regex("""(?:by|due|on)\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)""", RegexOption.IGNORE_CASE)
        dayPattern.find(normalizedText)?.let { match ->
            val dayName = match.groupValues[1].lowercase()
            val targetDay = when (dayName) {
                "monday" -> 1
                "tuesday" -> 2
                "wednesday" -> 3
                "thursday" -> 4
                "friday" -> 5
                "saturday" -> 6
                "sunday" -> 7
                else -> return null
            }
            return calculateScheduledTime(9, 0, targetDay)
        }
        
        return null
    }
    
    /**
     * Remove due date text from task title
     */
    private fun removeTaskDueDateText(text: String): String {
        return text
            .replace(Regex("""(?:by|due|on)\s+(?:tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday)""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
    
    /**
     * Check if the command is a "show tasks" command
     */
    fun isShowTasksCommand(text: String): Boolean {
        val normalizedText = text.trim().lowercase(Locale.getDefault())
        return SHOW_TASKS_PATTERN.find(normalizedText) != null
    }
}
