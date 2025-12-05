# Implementation Plan

- [x] 1. Set up Android project structure and dependencies





  - Create new Android project with Kotlin support and minimum SDK 26
  - Configure build.gradle.kts with Room, Coroutines, and AndroidX dependencies
  - Set up project package structure: data, domain, presentation, receivers
  - Configure AndroidManifest.xml with required permissions
  - _Requirements: 1.1, 1.4, 3.1, 5.4, 7.5_

- [x] 2. Implement data layer with Room database




- [x] 2.1 Create ReminderEntity and ReminderStatus enum


  - Write ReminderEntity data class with Room annotations
  - Define ReminderStatus enum (PENDING, COMPLETED, CANCELLED)
  - Add TypeConverters for timestamp handling
  - _Requirements: 2.2, 2.3, 3.1_

- [x] 2.2 Implement ReminderDao interface


  - Write DAO methods: insert, getActiveReminders, getReminderById, update, delete
  - Add appropriate Room query annotations
  - _Requirements: 3.1, 3.2, 3.3, 3.5_

- [x] 2.3 Create ReminderDatabase singleton


  - Implement Room database class with singleton pattern
  - Configure database builder with database name
  - _Requirements: 3.1, 3.4_


- [x] 3. Implement domain models and command parser




- [x] 3.1 Create Reminder domain model and mapping functions


  - Write Reminder data class for domain layer
  - Implement toEntity() and toDomain() extension functions
  - _Requirements: 2.1, 2.2_

- [x] 3.2 Implement CommandParser for natural language processing


  - Write regex patterns for time extraction ("at 2 PM", "at 14:00", "in 30 minutes")
  - Implement parseCommand() method to extract time and message
  - Handle invalid input and return null for unparseable commands
  - _Requirements: 1.5, 2.1, 2.4, 2.5_

- [x] 4. Implement notification scheduling system




- [x] 4.1 Create NotificationScheduler class


  - Implement scheduleReminder() using AlarmManager with exact alarms
  - Implement cancelReminder() to cancel scheduled alarms
  - Handle Android 12+ SCHEDULE_EXACT_ALARM permission
  - Create PendingIntent for ReminderReceiver
  - _Requirements: 5.1, 5.4_

- [x] 4.2 Implement ReminderReceiver broadcast receiver


  - Handle alarm broadcast and extract reminder ID from intent
  - Fetch reminder from database using reminder ID
  - Build and display notification with reminder message
  - Trigger TTS to speak reminder message
  - Update reminder status to COMPLETED in database
  - _Requirements: 5.2, 5.3, 5.5, 6.1, 6.4_


- [x] 5. Implement ReminderManager business logic




- [x] 5.1 Create ReminderManager class


  - Implement createReminder() to save reminder and schedule notification
  - Implement getActiveReminders() to fetch pending reminders
  - Implement markAsCompleted() to update reminder status
  - Implement deleteReminder() to remove reminder and cancel notification
  - Use coroutines for database operations
  - _Requirements: 2.2, 2.3, 3.1, 3.2, 3.3, 3.5, 5.1_

- [x] 6. Implement voice input with Speech-to-Text






- [x] 6.1 Create VoiceManager class

  - Initialize SpeechRecognizer with RecognizerIntent
  - Implement startListening() to begin voice capture
  - Implement RecognitionListener callbacks for results and errors
  - Handle STT errors with appropriate error messages
  - _Requirements: 1.1, 1.2, 1.3_


- [x] 6.2 Integrate VoiceManager into MainActivity

  - Add voice input button to activity_main.xml layout
  - Request RECORD_AUDIO permission at runtime
  - Implement onVoiceButtonClick() to start voice capture
  - Display listening indicator during audio capture
  - Handle speech result callback and pass to CommandParser
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 7. Implement Text-to-Speech functionality




- [x] 7.1 Create TTSManager class


  - Initialize TextToSpeech engine with OnInitListener
  - Implement speak() method to queue TTS output
  - Implement stop() and shutdown() for lifecycle management
  - Handle TTS initialization errors gracefully
  - _Requirements: 4.1, 4.3, 6.1, 6.5_


- [x] 7.2 Integrate TTSManager for voice confirmations


  - Initialize TTSManager in MainActivity
  - Speak confirmation message after reminder creation
  - Include scheduled time and message in confirmation
  - Display visual indicator during TTS output
  - Handle TTS unavailability with text fallback
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 7.3 Implement TTS for notification reminders


  - Initialize TTSManager in ReminderReceiver
  - Check device silent mode before speaking
  - Speak reminder message when notification triggers
  - Ensure audible volume level for TTS output
  - _Requirements: 6.1, 6.2, 6.3_

- [x] 8. Implement MainActivity UI and flow integration




- [x] 8.1 Create MainActivity layout and UI components

  - Design activity_main.xml with voice button and status text
  - Add listening indicator (animated icon or progress bar)
  - Add error message display area
  - Style UI with Material Design components
  - _Requirements: 1.4, 1.3_


- [x] 8.2 Wire up complete reminder creation flow





  - Initialize ReminderManager, VoiceManager, and TTSManager
  - Connect voice result to CommandParser
  - Call ReminderManager.createReminder() with parsed data
  - Trigger TTS confirmation on success
  - Handle parsing errors with user prompts
  - _Requirements: 1.5, 2.1, 2.2, 2.3, 4.1_


- [x] 9. Implement boot receiver for reminder persistence




- [x] 9.1 Create BootReceiver class


  - Implement BroadcastReceiver for BOOT_COMPLETED action
  - Fetch all active reminders from database on boot
  - Reschedule all pending reminders using NotificationScheduler
  - Register receiver in AndroidManifest.xml
  - _Requirements: 7.1, 7.2, 7.4_

- [x] 10. Handle runtime permissions




- [x] 10.1 Implement permission request flow


  - Request RECORD_AUDIO permission before voice input
  - Request POST_NOTIFICATIONS permission (Android 13+)
  - Request SCHEDULE_EXACT_ALARM permission (Android 12+)
  - Handle permission denial with user-friendly messages
  - Provide rationale for each permission request
  - _Requirements: 1.1, 5.4, 7.5_

- [x] 11. Configure AndroidManifest.xml




- [x] 11.1 Add permissions and components


  - Add all required permissions (RECORD_AUDIO, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE)
  - Register ReminderReceiver with intent filter
  - Register BootReceiver with BOOT_COMPLETED intent filter
  - Configure application name and theme
  - _Requirements: 1.1, 5.4, 7.2, 7.5_

- [x] 12. Implement error handling and edge cases




- [x] 12.1 Add comprehensive error handling


  - Handle STT service unavailable errors
  - Handle TTS initialization failures with text fallback
  - Handle database operation failures
  - Handle invalid time parsing (past times, invalid formats)
  - Display user-friendly error messages for all failure scenarios
  - _Requirements: 1.3, 2.4, 4.3, 6.5_


- [x] 13. Write unit tests for core components








- [x] 13.1 Write CommandParser tests






  - Test time format parsing ("at 2 PM", "at 14:00", "in 30 minutes")
  - Test message extraction from various command formats
  - Test invalid input handling
  - Test edge cases (midnight, next day times)


  - _Requirements: 2.1, 2.4, 2.5_

- [x] 13.2 Write ReminderManager tests






  - Test reminder creation with mocked database
  - Test status update operations


  - Test notification scheduling calls
  - Test error handling scenarios
  - _Requirements: 2.2, 2.3, 3.3, 5.1_

- [x] 13.3 Write Room database tests






  - Test CRUD operations on ReminderDao
  - Test query correctness for active reminders
  - Test data persistence across operations
  - Use in-memory database for testing
  - _Requirements: 3.1, 3.2, 3.5_
