# Implementation Plan

## Phase 1: Data Layer Foundation

- [x] 1. Create Priority and TaskCategory enums
  - Create Priority enum (HIGH, MEDIUM, LOW) in data package
  - Create TaskCategory enum (WORK, PERSONAL, SHOPPING, HEALTH, FINANCE, HOME, CUSTOM)
  - Add color mappings for priority levels
  - Add icon mappings for categories
  - _Requirements: 1.1, 1.2, 2.1_

- [x] 2. Create Task data model and database
  - Create TaskEntity with all fields (title, description, dueDate, priority, category, isCompleted, etc.)
  - Create TaskDao with CRUD operations and query methods
  - Create Task domain model with conversion extensions
  - Add RecurrenceRule data class for recurring tasks
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 3. Create Alarm data model and database
  - Create AlarmEntity with fields (label, hour, minute, isEnabled, repeatDays, vibrate, etc.)
  - Create AlarmDao with CRUD operations
  - Create Alarm domain model with conversion extensions
  - _Requirements: 3.1, 3.2, 3.5, 3.6_

- [x] 4. Enhance ReminderEntity with new fields
  - Add priority field to ReminderEntity
  - Add category field to ReminderEntity
  - Add recurrenceRule field to ReminderEntity
  - Add snoozeCount and originalScheduledTime fields
  - Add notes field for additional details
  - Create database migration for new columns
  - _Requirements: 1.1, 1.2, 1.3, 4.4_

- [x] 5. Update ReminderDatabase with migrations
  - Create tasks table migration
  - Create alarms table migration
  - Add new columns to reminders table migration
  - Test migration from previous version
  - _Requirements: 8.2_

## Phase 2: Domain Layer Implementation

- [x] 6. Implement TaskManager
  - Create TaskManager class with database dependency
  - Implement createTask() with all parameters
  - Implement completeTask() and uncompleteTask()
  - Implement deleteTask() and updateTask()
  - Implement getTasksFlow() and filtered queries
  - Implement getTaskProgress() for statistics
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.6_

- [x] 7. Implement EnhancedAlarmManager
  - Create EnhancedAlarmManager class
  - Implement createAlarm() with repeat days support
  - Implement toggleAlarm() for enable/disable
  - Implement deleteAlarm()
  - Implement snoozeAlarm() with duration
  - Implement getAlarmsFlow() and getNextAlarm()
  - Implement rescheduleAllAlarms() for boot recovery
  - _Requirements: 3.1, 3.2, 3.4, 3.5, 3.6, 8.1_

- [x] 8. Implement RecurrenceScheduler
  - Create RecurrenceScheduler class
  - Implement calculateNextOccurrence() for all recurrence types
  - Implement getOccurrencesInRange() for calendar display
  - Implement scheduleNextRecurrence() after trigger
  - Handle edge cases (month end, leap year, etc.)
  - _Requirements: 1.3, 1.4, 6.5_

- [x] 9. Implement SnoozeManager
  - Create SnoozeManager class
  - Implement snoozeReminder() with rescheduling
  - Implement snoozeAlarm() with rescheduling
  - Implement postponeReminder() for custom time
  - Define snooze options (5, 10, 15, 30 min, 1 hour)
  - Track snooze count for items
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 10. Enhance CommandParser for new commands
  - Add TASK_PATTERN for "add task [description]"
  - Add COMPLETE_TASK_PATTERN for "mark [task] as done"
  - Add RECURRING_ALARM_PATTERN for "alarm every [day]"
  - Add RECURRING_REMINDER_PATTERN for "remind me every [day]"
  - Add SNOOZE_PATTERN for "snooze for [duration]"
  - Add SHOW_TASKS_PATTERN for "show my tasks"
  - Implement parseTaskCommand() method
  - Implement parseRecurringAlarmCommand() method
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

## Phase 3: Task UI Implementation

- [x] 11. Create TaskViewModel
  - Create TaskViewModel with TaskManager dependency
  - Expose tasks flow with filtering support
  - Expose task progress state
  - Implement task CRUD operations
  - Implement category and priority filtering
  - Implement sorting options
  - _Requirements: 2.2, 2.4, 2.6_

- [x] 12. Create TaskListScreen
  - Create TaskListScreen composable
  - Implement task list with LazyColumn
  - Add swipe-to-complete gesture
  - Add priority color indicators (left border)
  - Add category filter chips
  - Add progress bar showing completion stats
  - Add FAB for creating new task
  - Style according to design system (white background, accent colors)
  - _Requirements: 2.2, 2.4, 2.6_

- [x] 13. Create TaskDetailScreen
  - Create TaskDetailScreen composable
  - Add title and description input fields
  - Add due date picker
  - Add PrioritySelector component
  - Add CategorySelector component
  - Add RecurrenceSelector component
  - Add save and delete buttons
  - _Requirements: 2.1, 1.1, 1.2, 1.3_

- [x] 14. Create task-related UI components
  - Create PrioritySelector with color indicators
  - Create CategorySelector with icons
  - Create RecurrenceSelector with options
  - Create TaskItem composable for list display
  - Add accessibility support to all components
  - _Requirements: 1.5, 1.6_

## Phase 4: Alarm UI Implementation

- [x] 15. Create AlarmViewModel
  - Create AlarmViewModel with EnhancedAlarmManager dependency
  - Expose alarms flow
  - Expose next alarm state
  - Implement alarm CRUD operations
  - Implement toggle functionality
  - Implement snooze functionality
  - _Requirements: 3.5, 3.6_

- [x] 16. Create AlarmListScreen
  - Create AlarmListScreen composable
  - Implement alarm list with toggle switches
  - Display repeat days as chips
  - Highlight next alarm
  - Add swipe-to-delete gesture
  - Add FAB for creating new alarm
  - Style according to design system
  - _Requirements: 3.5, 3.6_

- [x] 17. Create AlarmDetailScreen
  - Create AlarmDetailScreen composable
  - Add time picker (hour/minute wheels)
  - Add label input field
  - Add repeat days selector (weekday chips)
  - Add vibration toggle
  - Add save and delete buttons
  - _Requirements: 3.1, 3.2_

- [x] 18. Create AlarmTriggerScreen
  - Create full-screen AlarmTriggerScreen composable
  - Display large time and alarm label
  - Add slide-to-dismiss gesture
  - Add snooze button with duration options
  - Add animated background
  - Handle screen wake and lock
  - _Requirements: 3.3, 3.4_

- [x] 19. Create SnoozeDialog component
  - Create SnoozeDialog composable
  - Add preset duration options (5, 10, 15, 30 min, 1 hour)
  - Add custom time option
  - Style according to design system
  - _Requirements: 4.1, 4.2_

## Phase 5: Integration and Notifications

- [x] 20. Update Calendar integration
  - Modify CalendarScreen to fetch tasks with due dates
  - Modify CalendarScreen to fetch alarms
  - Add different indicators for reminders, tasks, alarms
  - Update event list to show all item types
  - Add item type icons/colors for differentiation
  - _Requirements: 6.1, 6.2, 6.4, 6.5_

- [x] 21. Create notification channels and handlers
  - Create TASKS notification channel
  - Create ALARMS notification channel with high priority
  - Create ALARMS_HIGH channel for full-screen intent
  - Update notification builder for rich notifications
  - _Requirements: 7.1, 7.2, 7.3_

- [x] 22. Implement notification actions
  - Add Done action to reminder notifications
  - Add Snooze action to reminder notifications
  - Add Complete action to task notifications
  - Add Dismiss and Snooze to alarm notifications
  - Handle action intents in receivers
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 23. Create AlarmReceiver
  - Create AlarmReceiver broadcast receiver
  - Implement full-screen notification display
  - Implement alarm sound playback
  - Implement vibration pattern
  - Handle snooze and dismiss actions
  - _Requirements: 3.3, 3.4_

- [x] 24. Enhance SnoozeActionReceiver
  - Update to handle both reminders and alarms
  - Implement rescheduling logic
  - Update snooze count tracking
  - Send confirmation notification
  - _Requirements: 4.2, 4.4_

- [x] 25. Update BootReceiver
  - Reschedule all pending reminders on boot
  - Reschedule all enabled alarms on boot
  - Reschedule recurring items
  - Handle missed items appropriately
  - _Requirements: 8.1, 8.3_

## Phase 6: Navigation and Voice Commands

- [x] 26. Update navigation structure
  - Add Tasks destination to navigation
  - Add Alarms destination to navigation
  - Update bottom navigation bar (consider tab arrangement)
  - Implement navigation to task/alarm detail screens
  - Handle deep links from notifications
  - _Requirements: 6.3, 7.4_

- [x] 27. Integrate voice commands in ChatViewModel
  - Handle "add task" commands
  - Handle "mark task as done" commands
  - Handle recurring alarm commands
  - Handle recurring reminder commands
  - Handle "snooze" commands
  - Handle "show my tasks" commands
  - Provide voice feedback for actions
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

## Phase 7: Polish and Testing (Future Enhancements)

- [ ] 28. Add animations and transitions (Optional)
  - Add task completion animation
  - Add alarm toggle animation
  - Add screen transition animations
  - Add list item animations
  - Ensure 60fps performance
  - _Requirements: UI polish_

- [ ] 29. Implement data backup/export (Optional)
  - Create backup data format (JSON)
  - Implement export functionality
  - Implement import functionality
  - Add backup settings in app
  - _Requirements: 8.5_

- [ ] 30. Final testing and bug fixes (Ongoing)
  - Test all voice commands
  - Test notification actions
  - Test boot recovery
  - Test recurring item scheduling
  - Test snooze functionality
  - Test calendar integration
  - Fix any discovered issues
  - Verify accessibility compliance
  - _Requirements: All_

---

## Implementation Summary

### Completed Features:
1. **Data Layer**: Priority, TaskCategory, RecurrenceRule enums; TaskEntity, AlarmEntity with DAOs; Database migration to v3
2. **Domain Layer**: TaskManager, EnhancedAlarmManager, RecurrenceScheduler, SnoozeManager; Enhanced CommandParser with new voice commands
3. **Task UI**: TaskListScreen with filters/sorting, TaskDetailScreen with date picker, PrioritySelector, CategorySelector
4. **Alarm UI**: AlarmListScreen with toggle switches, AlarmDetailScreen with time picker, AlarmTriggerScreen with slide-to-dismiss
5. **Integration**: Calendar shows tasks/alarms, SnoozeActionReceiver handles both types, BootReceiver reschedules alarms
6. **Navigation**: Tasks and Alarms tabs in bottom nav, detail screens for create/edit
7. **Voice Commands**: create_task, complete_task, create_recurring_alarm, create_recurring_reminder, snooze

### New Voice Commands Supported:
- "add task [description]" - Creates a new task
- "mark [task] as done" - Completes a task
- "alarm at 7am every weekday" - Creates recurring alarm
- "remind me every day at 9am to [message]" - Creates recurring reminder
- "snooze for 10 minutes" - Snoozes active notification
