# Voice-to-Calendar Integration Test Plan

## Overview
This document describes manual testing procedures to verify that creating reminders via voice input automatically updates the calendar UI in real-time.

## Prerequisites
- Android device or emulator with API level 26+
- Microphone permission granted
- App installed and running

## Test Cases

### Test Case 1: Create Reminder via Voice - Calendar Updates
**Objective:** Verify that creating a reminder via voice input updates the calendar view with event indicators

**Steps:**
1. Launch the app
2. Navigate to the Calendar tab (should be the default view)
3. Note the current month and dates with event indicators
4. Tap the FAB (Floating Action Button) with microphone icon
5. Speak a reminder command: "Remind me to call dentist tomorrow at 2 PM"
6. Wait for confirmation message
7. Navigate back to Calendar tab

**Expected Results:**
- Calendar view should automatically show an event indicator on tomorrow's date
- No manual refresh should be required
- Event indicator should appear within 1-2 seconds of reminder creation

**Status:** ⬜ Pass ⬜ Fail

---

### Test Case 2: Create Reminder - Event List Updates
**Objective:** Verify that the event list for a selected date updates when a reminder is created for that date

**Steps:**
1. Navigate to Calendar tab
2. Select tomorrow's date on the calendar
3. Note the current list of reminders for that date (may be empty)
4. Tap the FAB to open voice input
5. Speak: "Remind me to buy groceries tomorrow at 5 PM"
6. Wait for confirmation
7. Observe the event list below the calendar

**Expected Results:**
- Event list should automatically update to show the new reminder
- New reminder should appear with time "5:00 PM" and message "buy groceries"
- No manual refresh required
- Update should occur within 1-2 seconds

**Status:** ⬜ Pass ⬜ Fail

---

### Test Case 3: Create Reminder - All Events List Updates
**Objective:** Verify that the All Events list updates when a new reminder is created

**Steps:**
1. Navigate to All Events tab (middle tab in bottom navigation)
2. Note the current list of reminders
3. Tap the FAB to open voice input
4. Speak: "Remind me to submit report next Monday at 10 AM"
5. Wait for confirmation
6. Navigate back to All Events tab

**Expected Results:**
- All Events list should automatically include the new reminder
- Reminder should appear under the correct date header (next Monday)
- List should be sorted chronologically
- No manual refresh required

**Status:** ⬜ Pass ⬜ Fail

---

### Test Case 4: Multiple Reminders - Event Count Updates
**Objective:** Verify that event indicators show correct counts when multiple reminders are created

**Steps:**
1. Navigate to Calendar tab
2. Select a future date (e.g., 3 days from now)
3. Note the current event indicator (if any)
4. Create first reminder via voice: "Remind me to morning meeting [date] at 9 AM"
5. Create second reminder via voice: "Remind me to lunch with client [date] at 12 PM"
6. Create third reminder via voice: "Remind me to team standup [date] at 3 PM"
7. Observe the calendar

**Expected Results:**
- Event indicator should update after each reminder creation
- Visual indicator should reflect multiple events (different style for 3+ events)
- Event list for that date should show all 3 reminders
- All updates should happen automatically

**Status:** ⬜ Pass ⬜ Fail

---

### Test Case 5: Delete Reminder - Calendar Updates
**Objective:** Verify that deleting a reminder updates the calendar automatically

**Steps:**
1. Create a reminder for tomorrow via voice
2. Navigate to Calendar tab
3. Select tomorrow's date
4. Swipe left on the reminder card to delete it
5. Observe the calendar and event list

**Expected Results:**
- Reminder should be removed from the event list immediately
- If it was the only reminder for that date, event indicator should disappear from calendar
- All views should update automatically without manual refresh

**Status:** ⬜ Pass ⬜ Fail

---

### Test Case 6: Cross-Fragment State Consistency
**Objective:** Verify that all fragments show consistent data after voice reminder creation

**Steps:**
1. Navigate to Calendar tab and note the event count
2. Navigate to All Events tab and count total reminders
3. Tap FAB and create a new reminder via voice
4. Navigate to Calendar tab - verify event indicator appears
5. Navigate to All Events tab - verify reminder appears in list
6. Select the date in Calendar tab - verify reminder appears in event list

**Expected Results:**
- All three views (Calendar, Event List, All Events) should show the new reminder
- Data should be consistent across all views
- No discrepancies in reminder count or details

**Status:** ⬜ Pass ⬜ Fail

---

### Test Case 7: Real-time Flow Updates
**Objective:** Verify that Flow-based updates work in real-time without polling

**Steps:**
1. Navigate to Calendar tab
2. Keep the Calendar view visible
3. Tap FAB and create a reminder for today
4. Immediately observe the calendar (don't navigate away)

**Expected Results:**
- Event indicator should appear on today's date within 1-2 seconds
- Update should happen while staying on the same screen
- No screen refresh or navigation required

**Status:** ⬜ Pass ⬜ Fail

---

### Test Case 8: Filter Consistency After Voice Creation
**Objective:** Verify that filters work correctly after creating reminders via voice

**Steps:**
1. Navigate to All Events tab
2. Apply "PENDING" filter
3. Note the current list
4. Create a new reminder via voice
5. Return to All Events tab with PENDING filter still active

**Expected Results:**
- New reminder should appear in the filtered list (since it's pending)
- Filter should remain active
- List should update automatically

**Status:** ⬜ Pass ⬜ Fail

---

## Technical Verification

### Code Flow Verification
The following components work together for real-time updates:

1. **ReminderDao** - Returns `Flow<List<ReminderEntity>>` for reactive queries
2. **ReminderManager** - Exposes `getAllRemindersFlow()` that maps entities to domain models
3. **CalendarViewModel** - Observes the Flow in `observeReminders()` and updates StateFlows
4. **UI Fragments** - Collect StateFlows and update UI automatically

### Key Implementation Points
- ✅ ReminderDao has `getActiveRemindersFlow()` and `getAllRemindersFlow()` methods
- ✅ ReminderManager exposes Flow-based methods
- ✅ CalendarViewModel observes Flow in `init` block
- ✅ CalendarViewModel updates StateFlows when Flow emits
- ✅ Fragments collect StateFlows for UI updates
- ✅ No manual `loadReminders()` calls needed after database changes

## Test Results Summary

| Test Case | Status | Notes |
|-----------|--------|-------|
| TC1: Calendar Updates | ⬜ | |
| TC2: Event List Updates | ⬜ | |
| TC3: All Events Updates | ⬜ | |
| TC4: Event Count Updates | ⬜ | |
| TC5: Delete Updates | ⬜ | |
| TC6: Cross-Fragment Consistency | ⬜ | |
| TC7: Real-time Flow | ⬜ | |
| TC8: Filter Consistency | ⬜ | |

## Notes
- All tests should be performed on a physical device or emulator
- Voice recognition requires internet connection for some devices
- Tests should be repeated after app restart to verify persistence
- Performance should be monitored - updates should occur within 1-2 seconds

## Automated Test Coverage
The automated integration test `VoiceCalendarIntegrationTest.kt` covers:
- ✅ Reminder creation updates calendar automatically
- ✅ Event indicators show correct counts
- ✅ Selected date reminders update when date is selected
- ✅ Multiple reminders update all views
- ✅ Deletion updates calendar automatically
- ✅ Flow emits updates reactively
