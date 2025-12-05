# Location Reminder Manual Test Plan

## Overview

This document provides a comprehensive manual testing plan for location-based reminders. These tests must be performed on a physical Android device to verify real-world functionality including geofence triggering, location monitoring, and notification delivery.

## Prerequisites

### Device Requirements
- Android device with API level 24+ (Android 7.0+)
- GPS/Location services enabled
- Google Play Services installed and updated
- Sufficient battery (>50% recommended)
- Mobile data or WiFi connection

### App Permissions
Before testing, ensure the following permissions are granted:
- ✅ Location (Fine Location)
- ✅ Background Location (Android 10+)
- ✅ Notifications
- ✅ Microphone (for voice commands)

### Test Environment Setup
1. Install the Voice Reminder Assistant app
2. Grant all required permissions
3. Enable location services on device
4. Ensure GPS has good signal (test outdoors or near windows)
5. Have a notebook or spreadsheet ready to record results

## Test Scenarios

### Scenario 1: Remind Me at Home

**Requirement**: 1.2, 1.3, 4.4  
**Objective**: Verify reminder triggers when arriving home

#### Setup
1. Open app and go to Location Settings
2. Add "home" as a saved location:
   - Option A: Use "Current Location" while at home
   - Option B: Enter home address manually
3. Set radius to 100 meters
4. Save location

#### Test Steps
1. **Create Reminder**:
   - Tap microphone button
   - Say: "Remind me to water plants when I get home"
   - Verify voice feedback confirms reminder creation
   - Check reminder list shows location reminder with home icon

2. **Leave Home**:
   - Travel at least 200 meters away from home
   - Wait 2-3 minutes for geofence to register exit
   - Verify no notification appears (should only trigger on entry)

3. **Return Home**:
   - Travel back to home location
   - Enter the 100-meter radius around home
   - **Expected**: Notification appears with message "water plants"
   - Verify notification sound/vibration
   - Verify notification content is correct

4. **Verify Cooldown**:
   - Leave home again (>100m)
   - Return home within 30 minutes
   - **Expected**: No duplicate notification (cooldown active)
   - Wait 30+ minutes, leave and return
   - **Expected**: Notification appears again

#### Success Criteria
- ✅ Reminder created successfully
- ✅ Notification appears when entering home radius
- ✅ Notification content is correct
- ✅ Cooldown prevents duplicate triggers
- ✅ No false triggers while away from home

#### Notes
- Record actual trigger distance
- Note any delays in notification delivery
- Check battery usage in device settings

---

### Scenario 2: Remind Me at Any Store

**Requirement**: 1.1, 1.2, 2.2, 2.4  
**Objective**: Verify reminder triggers at multiple store locations

#### Test Steps
1. **Create Reminder**:
   - Say: "Remind me to buy milk at any store"
   - Verify reminder is created with store category

2. **Visit First Store**:
   - Travel to a grocery store or supermarket
   - Approach within 200 meters
   - **Expected**: Notification appears with message "buy milk"
   - Verify notification timing (should trigger on entry)

3. **Visit Second Store**:
   - After cooldown period (30+ minutes)
   - Visit a different store
   - **Expected**: Notification appears again
   - Verify works at different store locations

4. **Test Different Store Types**:
   - Try convenience store
   - Try department store
   - Verify triggers at various store types

#### Success Criteria
- ✅ Reminder triggers at multiple store locations
- ✅ Works with different types of stores
- ✅ Notification appears within reasonable time of arrival
- ✅ Cooldown prevents rapid re-triggering

#### Notes
- Record which store types trigger the reminder
- Note if any stores are missed
- Check if radius (200m) is appropriate

---

### Scenario 3: Remind Me at Work

**Requirement**: 1.3, 4.1, 4.2, 4.4  
**Objective**: Verify reminder triggers when arriving at work

#### Setup
1. Add "work" as a saved location
2. Set radius to 150 meters

#### Test Steps
1. **Create Reminder**:
   - Say: "Remind me to check email when I arrive at work"
   - Verify reminder created with work location

2. **Morning Commute**:
   - Start from home (outside work radius)
   - Travel to work
   - Approach work location
   - **Expected**: Notification appears when entering 150m radius
   - Verify timing is appropriate for arrival

3. **Lunch Break**:
   - Leave work (>150m away)
   - Return after lunch
   - **Expected**: No notification (cooldown active if <30 min)

4. **Next Day**:
   - Arrive at work next morning
   - **Expected**: Notification appears (cooldown expired)

#### Success Criteria
- ✅ Triggers on arrival at work
- ✅ Appropriate timing for commute scenario
- ✅ Cooldown prevents lunch break re-triggers
- ✅ Works consistently across multiple days

---

### Scenario 4: Remind Me at Pharmacy

**Requirement**: 1.2, 2.2  
**Objective**: Verify reminder triggers at pharmacy locations

#### Test Steps
1. **Create Reminder**:
   - Say: "Remind me to pick up prescription at the pharmacy"
   - Verify reminder created with pharmacy category

2. **Visit Pharmacy**:
   - Travel to a pharmacy
   - Approach within 200 meters
   - **Expected**: Notification appears

3. **Test Multiple Pharmacies**:
   - After cooldown, visit different pharmacy
   - Verify triggers at different locations

#### Success Criteria
- ✅ Triggers at pharmacy locations
- ✅ Works with different pharmacy chains
- ✅ Appropriate trigger radius

---

### Scenario 5: Remind Me at Gas Station

**Requirement**: 1.2, 2.2  
**Objective**: Verify reminder triggers at gas stations

#### Test Steps
1. **Create Reminder**:
   - Say: "Remind me to check tire pressure at a gas station"
   - Verify reminder created with gas station category

2. **Visit Gas Station**:
   - Drive to a gas station
   - Enter the location
   - **Expected**: Notification appears

3. **Test While Driving**:
   - Drive past gas station without stopping
   - Verify if notification still appears
   - Note timing relative to location

#### Success Criteria
- ✅ Triggers at gas station locations
- ✅ Works while driving (if within radius long enough)
- ✅ Appropriate for driving scenario

---

### Scenario 6: Multiple Saved Locations

**Requirement**: 4.1, 4.3, 4.4, 4.5  
**Objective**: Verify multiple saved locations work correctly

#### Setup
1. Add multiple saved locations:
   - Home (100m radius)
   - Work (150m radius)
   - Gym (100m radius)
   - Favorite restaurant (100m radius)

#### Test Steps
1. **Create Multiple Reminders**:
   - "Remind me to lock door when I get home"
   - "Remind me to check calendar at work"
   - "Remind me to bring water bottle at the gym"
   - "Remind me to order dessert at [restaurant name]"

2. **Visit Each Location**:
   - Travel to each location over several days
   - Verify correct reminder triggers at each location
   - Verify no cross-triggering (wrong reminder at wrong location)

3. **Edit Saved Location**:
   - Edit home location radius to 150m
   - Test if updated radius works correctly

4. **Delete Saved Location**:
   - Delete gym location
   - Verify gym reminder no longer triggers
   - Verify other reminders still work

#### Success Criteria
- ✅ All reminders trigger at correct locations
- ✅ No cross-triggering between locations
- ✅ Editing location updates behavior
- ✅ Deleting location disables associated reminders

---

### Scenario 7: Permission Handling

**Requirement**: 3.1, 3.2, 3.3, 7.1  
**Objective**: Verify app handles permission changes gracefully

#### Test Steps
1. **Deny Location Permission**:
   - Revoke location permission in device settings
   - Try to create location reminder
   - **Expected**: Error message explaining permission needed
   - **Expected**: Prompt to grant permission

2. **Deny Background Location**:
   - Grant foreground location only
   - Deny background location
   - Create location reminder
   - **Expected**: Warning about limited functionality
   - Test if reminder still works in foreground

3. **Re-grant Permissions**:
   - Grant all permissions
   - Verify existing reminders start working again
   - Create new reminder
   - Verify works normally

#### Success Criteria
- ✅ Clear error messages when permissions denied
- ✅ Graceful degradation with partial permissions
- ✅ Automatic recovery when permissions granted
- ✅ No crashes due to permission issues

---

### Scenario 8: Location Services Disabled

**Requirement**: 7.1, 7.2  
**Objective**: Verify app handles disabled location services

#### Test Steps
1. **Disable Location Services**:
   - Turn off location services in device settings
   - Try to create location reminder
   - **Expected**: Error message prompting to enable location
   - **Expected**: Link to settings

2. **Create Reminder While Disabled**:
   - Attempt to create reminder
   - **Expected**: Reminder saved but not active
   - **Expected**: Warning notification

3. **Re-enable Location Services**:
   - Turn location services back on
   - **Expected**: Reminders become active automatically
   - Test if geofences are registered

#### Success Criteria
- ✅ Clear guidance when location services disabled
- ✅ Reminders saved but inactive
- ✅ Automatic activation when services enabled
- ✅ No data loss

---

### Scenario 9: Device Restart

**Requirement**: 2.5, 7.5  
**Objective**: Verify reminders persist and reactivate after restart

#### Test Steps
1. **Create Multiple Reminders**:
   - Create 3-5 location reminders
   - Verify all are active

2. **Restart Device**:
   - Power off device completely
   - Wait 30 seconds
   - Power on device

3. **Verify Reminders**:
   - Open app
   - Check reminder list - all should still exist
   - Visit a reminder location
   - **Expected**: Notification appears (geofences re-registered)

4. **Check Background Service**:
   - Verify location monitoring is active
   - Check battery usage is reasonable

#### Success Criteria
- ✅ All reminders persist after restart
- ✅ Geofences automatically re-registered
- ✅ Location monitoring resumes
- ✅ No manual intervention required

---

### Scenario 10: App Force-Stop

**Requirement**: 7.5  
**Objective**: Verify recovery from force-stop

#### Test Steps
1. **Create Reminders**:
   - Create several location reminders

2. **Force-Stop App**:
   - Go to device Settings > Apps > Voice Reminder Assistant
   - Tap "Force Stop"

3. **Test Triggering**:
   - Visit a reminder location
   - **Expected**: Notification may not appear (geofences removed)

4. **Reopen App**:
   - Open app manually
   - **Expected**: Geofences re-registered
   - Visit reminder location again
   - **Expected**: Notification appears

#### Success Criteria
- ✅ App recovers from force-stop
- ✅ Geofences re-registered on app open
- ✅ Reminders work after recovery
- ✅ No data corruption

---

### Scenario 11: User Already at Location

**Requirement**: 7.3  
**Objective**: Verify immediate trigger when already at location

#### Test Steps
1. **At Home**:
   - Ensure you are currently at home location
   - Create reminder: "Remind me to lock door when I get home"
   - **Expected**: Notification appears immediately
   - OR: Notification appears within 1-2 minutes

2. **At Store**:
   - While at a store
   - Create reminder: "Remind me to buy bread at any store"
   - **Expected**: Immediate or near-immediate notification

#### Success Criteria
- ✅ Reminder triggers when already at location
- ✅ Minimal delay (< 2 minutes)
- ✅ User doesn't need to leave and return

---

### Scenario 12: Battery Impact

**Requirement**: 5.1  
**Objective**: Verify reasonable battery consumption

#### Test Steps
1. **Baseline Measurement**:
   - Fully charge device
   - Note battery percentage
   - Use device normally without location reminders for 24 hours
   - Record battery usage

2. **With Location Reminders**:
   - Fully charge device
   - Create 5-10 active location reminders
   - Use device normally for 24 hours
   - Record battery usage

3. **Compare**:
   - Calculate difference in battery consumption
   - Check battery usage breakdown in device settings
   - Verify location reminder app usage is reasonable

#### Success Criteria
- ✅ Battery impact < 5% additional drain per day
- ✅ No excessive background activity
- ✅ Geofencing API used efficiently
- ✅ No battery drain when no active reminders

---

## Test Results Template

### Test Execution Record

| Scenario | Date | Device | Android Version | Result | Notes |
|----------|------|--------|-----------------|--------|-------|
| 1. Home | | | | ☐ Pass ☐ Fail | |
| 2. Store | | | | ☐ Pass ☐ Fail | |
| 3. Work | | | | ☐ Pass ☐ Fail | |
| 4. Pharmacy | | | | ☐ Pass ☐ Fail | |
| 5. Gas Station | | | | ☐ Pass ☐ Fail | |
| 6. Multiple Locations | | | | ☐ Pass ☐ Fail | |
| 7. Permissions | | | | ☐ Pass ☐ Fail | |
| 8. Location Services | | | | ☐ Pass ☐ Fail | |
| 9. Device Restart | | | | ☐ Pass ☐ Fail | |
| 10. Force-Stop | | | | ☐ Pass ☐ Fail | |
| 11. Already at Location | | | | ☐ Pass ☐ Fail | |
| 12. Battery Impact | | | | ☐ Pass ☐ Fail | |

### Issues Found

| Issue # | Scenario | Description | Severity | Status |
|---------|----------|-------------|----------|--------|
| | | | ☐ Critical ☐ High ☐ Medium ☐ Low | ☐ Open ☐ Fixed |

### Performance Metrics

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Notification Delay | < 30 seconds | | ☐ Pass ☐ Fail |
| Battery Impact | < 5% per day | | ☐ Pass ☐ Fail |
| Geofence Accuracy | ± 50 meters | | ☐ Pass ☐ Fail |
| False Positives | 0 | | ☐ Pass ☐ Fail |
| Missed Triggers | 0 | | ☐ Pass ☐ Fail |

## Troubleshooting

### Common Issues

**Notification Not Appearing**:
- Check location permissions granted
- Verify location services enabled
- Check notification permissions
- Verify geofence radius is appropriate
- Check if cooldown period is active
- Verify GPS signal is strong

**Delayed Notifications**:
- GPS signal may be weak
- Device may be in battery saver mode
- Background restrictions may be active
- Check Google Play Services is updated

**False Triggers**:
- Geofence radius may be too large
- GPS accuracy may be poor
- Check for nearby locations with similar names

**Battery Drain**:
- Check number of active geofences
- Verify geofence radius is not too small
- Check for other location-using apps
- Verify background restrictions are appropriate

## Conclusion

Complete all scenarios in this test plan to ensure the location-based reminder feature works reliably in real-world conditions. Document all results, issues, and observations for future reference and improvement.

### Sign-off

- Tester Name: _______________
- Date: _______________
- Overall Result: ☐ Pass ☐ Fail ☐ Pass with Issues
- Signature: _______________
