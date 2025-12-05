# TalkBack Testing Guide for Calendar UI

## Prerequisites
1. Android device or emulator with TalkBack installed
2. Voice Reminder Assistant app installed
3. Some test reminders created

## Enabling TalkBack

### On Physical Device:
1. Go to **Settings** > **Accessibility**
2. Select **TalkBack**
3. Toggle **Use TalkBack** to ON
4. Confirm the dialog

### On Emulator:
1. Open **Settings** app
2. Navigate to **Accessibility** > **TalkBack**
3. Enable TalkBack
4. Use keyboard shortcuts: Alt+Shift+T (Windows) or Option+Shift+T (Mac)

## TalkBack Gestures
- **Swipe Right**: Move to next item
- **Swipe Left**: Move to previous item
- **Double Tap**: Activate selected item
- **Swipe Down then Right**: Read from top
- **Two Finger Swipe Up**: Read from current position
- **Two Finger Swipe Down**: Stop reading

## Test Cases

### 1. Calendar Fragment Navigation

#### Test 1.1: Calendar View Description
**Steps:**
1. Open the app (Calendar tab should be default)
2. Swipe right to navigate to calendar view

**Expected TalkBack Announcement:**
- "Calendar view November 2025" (or current month)

**Pass Criteria:**
- ✓ Calendar announces itself with current month/year
- ✓ Announcement is clear and understandable

---

#### Test 1.2: Navigation Buttons
**Steps:**
1. Navigate to Previous Month button
2. Double tap to activate
3. Navigate to Next Month button
4. Double tap to activate
5. Navigate to Today button
6. Double tap to activate

**Expected TalkBack Announcements:**
- "Previous month, button"
- "Next month, button"
- "Today, button"

**Pass Criteria:**
- ✓ All buttons announce their purpose
- ✓ Button type is announced
- ✓ Buttons are activatable with double tap

---

#### Test 1.3: Month/Year Display
**Steps:**
1. Navigate to the month/year text (e.g., "November 2025")

**Expected TalkBack Announcement:**
- "November 2025"

**Pass Criteria:**
- ✓ Current month and year are announced
- ✓ Text is clear and properly formatted

---

### 2. Reminder Cards Accessibility

#### Test 2.1: Pending Reminder Card
**Steps:**
1. Navigate to a pending reminder card in the event list
2. Listen to the announcement

**Expected TalkBack Announcement:**
- "Reminder at 2:00 PM: Team meeting, status: Pending"

**Pass Criteria:**
- ✓ Time is announced first
- ✓ Message is announced
- ✓ Status is announced last
- ✓ All information is in a single, coherent announcement

---

#### Test 2.2: Completed Reminder Card
**Steps:**
1. Navigate to a completed reminder card
2. Listen to the announcement

**Expected TalkBack Announcement:**
- "Reminder at 6:00 PM: Grocery shopping, status: Completed"

**Pass Criteria:**
- ✓ Completed status is clearly announced
- ✓ Format matches pending reminders

---

#### Test 2.3: Reminder Card Interaction
**Steps:**
1. Navigate to a reminder card
2. Double tap to activate

**Expected Behavior:**
- Card click action is triggered
- Reminder details are shown (if implemented)

**Pass Criteria:**
- ✓ Card is activatable
- ✓ Action provides feedback

---

### 3. All Events Fragment

#### Test 3.1: Filter Chips
**Steps:**
1. Navigate to All Events tab
2. Swipe right to reach filter chips
3. Navigate through each chip

**Expected TalkBack Announcements:**
- "All, selected, chip"
- "Pending, not selected, chip"
- "Completed, not selected, chip"

**Pass Criteria:**
- ✓ Each filter announces its name
- ✓ Selection state is announced
- ✓ Chip type is announced

---

#### Test 3.2: Date Headers
**Steps:**
1. Navigate through the all events list
2. Listen for date header announcements

**Expected TalkBack Announcements:**
- "Today - Nov 15"
- "Tomorrow - Nov 16"
- "Wednesday - Nov 20"

**Pass Criteria:**
- ✓ Date headers are announced
- ✓ Relative dates (Today/Tomorrow) are used when appropriate
- ✓ Headers are distinguishable from reminder cards

---

#### Test 3.3: Empty State
**Steps:**
1. Filter to show only completed reminders (if none exist)
2. Navigate to empty state view

**Expected TalkBack Announcement:**
- "No completed reminders" (or appropriate message)
- Empty state icon description

**Pass Criteria:**
- ✓ Empty state message is announced
- ✓ Icon has appropriate content description

---

### 4. Bottom Navigation

#### Test 4.1: Navigation Tabs
**Steps:**
1. Navigate to bottom navigation bar
2. Swipe through each tab

**Expected TalkBack Announcements:**
- "Calendar, tab 1 of 3, selected"
- "All Events, tab 2 of 3"
- "Voice, tab 3 of 3"

**Pass Criteria:**
- ✓ Tab names are announced
- ✓ Tab position is announced (1 of 3, etc.)
- ✓ Selection state is announced

---

#### Test 4.2: Floating Action Button
**Steps:**
1. Navigate to the FAB
2. Listen to announcement

**Expected TalkBack Announcement:**
- "Voice, button" or "Add reminder via voice, button"

**Pass Criteria:**
- ✓ FAB purpose is clear
- ✓ Button type is announced

---

### 5. Event List Fragment

#### Test 5.1: Event List for Selected Date
**Steps:**
1. Select a date on calendar
2. Navigate to event list below calendar

**Expected TalkBack Announcement:**
- LazyColumn announces its content
- Individual reminders are announced with full details

**Pass Criteria:**
- ✓ List is navigable
- ✓ Each reminder is announced completely

---

#### Test 5.2: Empty Event List
**Steps:**
1. Select a date with no reminders
2. Navigate to empty state

**Expected TalkBack Announcement:**
- "No events for this date"
- Empty state icon description

**Pass Criteria:**
- ✓ Empty message is clear
- ✓ Icon has content description

---

## Color Contrast Verification

### Visual Inspection Tests

#### Test 6.1: Event Indicators on Calendar
**Check:**
- Event dots are visible on calendar dates
- Purple color (#6750A4) is distinguishable from white background

**Pass Criteria:**
- ✓ Contrast ratio ≥ 3:1 for UI components
- ✓ Indicators are visible to users with normal vision
- ✓ Indicators are visible to users with color blindness

---

#### Test 6.2: Reminder Card Text
**Check:**
- Pending reminder text (black on white)
- Completed reminder text (gray on light gray)

**Pass Criteria:**
- ✓ Pending text: Contrast ratio ≥ 4.5:1 (WCAG AA)
- ✓ Completed text: Contrast ratio ≥ 4.5:1 (WCAG AA)
- ✓ All text is readable

---

#### Test 6.3: Navigation Elements
**Check:**
- Bottom navigation icons and labels
- Navigation buttons (Previous/Next/Today)

**Pass Criteria:**
- ✓ All navigation elements are clearly visible
- ✓ Icons have sufficient contrast
- ✓ Text labels are readable

---

## Automated Accessibility Testing

### Using Android Accessibility Scanner

1. Install **Accessibility Scanner** from Google Play Store
2. Enable the scanner in Accessibility settings
3. Open Voice Reminder Assistant
4. Tap the Accessibility Scanner floating button
5. Review suggestions for:
   - Touch target size (minimum 48x48dp)
   - Content descriptions
   - Color contrast
   - Text size

**Expected Results:**
- No critical issues
- All interactive elements have appropriate touch targets
- All images have content descriptions
- Text meets minimum size requirements

---

## Common Issues and Solutions

### Issue 1: TalkBack Not Announcing Content
**Solution:**
- Verify `android:importantForAccessibility="yes"` is set
- Check that `contentDescription` is not empty
- Ensure views are not hidden or have 0 alpha

### Issue 2: Redundant Announcements
**Solution:**
- Use `android:importantForAccessibility="no"` on decorative elements
- Consolidate content descriptions at parent level

### Issue 3: Incorrect Reading Order
**Solution:**
- Use `android:accessibilityTraversalBefore` and `android:accessibilityTraversalAfter`
- Restructure layout hierarchy if needed

---

## Test Results Template

```
Test Date: _______________
Tester: _______________
Device: _______________
Android Version: _______________
TalkBack Version: _______________

| Test Case | Pass | Fail | Notes |
|-----------|------|------|-------|
| 1.1 Calendar View Description | [ ] | [ ] | |
| 1.2 Navigation Buttons | [ ] | [ ] | |
| 1.3 Month/Year Display | [ ] | [ ] | |
| 2.1 Pending Reminder Card | [ ] | [ ] | |
| 2.2 Completed Reminder Card | [ ] | [ ] | |
| 2.3 Reminder Card Interaction | [ ] | [ ] | |
| 3.1 Filter Chips | [ ] | [ ] | |
| 3.2 Date Headers | [ ] | [ ] | |
| 3.3 Empty State | [ ] | [ ] | |
| 4.1 Navigation Tabs | [ ] | [ ] | |
| 4.2 Floating Action Button | [ ] | [ ] | |
| 5.1 Event List for Selected Date | [ ] | [ ] | |
| 5.2 Empty Event List | [ ] | [ ] | |
| 6.1 Event Indicators | [ ] | [ ] | |
| 6.2 Reminder Card Text | [ ] | [ ] | |
| 6.3 Navigation Elements | [ ] | [ ] | |

Overall Result: PASS / FAIL

Additional Comments:
_________________________________
_________________________________
_________________________________
```

---

## Accessibility Best Practices Checklist

- [x] All interactive elements have content descriptions
- [x] Content descriptions are concise and meaningful
- [x] Decorative elements are marked as not important for accessibility
- [x] Color is not the only means of conveying information
- [x] Text has sufficient contrast (WCAG AA minimum)
- [x] Touch targets are at least 48x48dp
- [x] Navigation is logical and predictable
- [x] Focus order follows visual layout
- [x] Error messages are announced
- [x] State changes are announced

---

## Resources

- [Android Accessibility Documentation](https://developer.android.com/guide/topics/ui/accessibility)
- [Material Design Accessibility](https://material.io/design/usability/accessibility.html)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
