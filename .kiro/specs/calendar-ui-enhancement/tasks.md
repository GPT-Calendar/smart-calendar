# Implementation Plan

- [x] 1. Set up project dependencies and navigation infrastructure





  - Add Material Calendar View library dependency to build.gradle.kts
  - Add Navigation Component dependencies if not present
  - Add RecyclerView and Material Design 3 dependencies
  - Create navigation graph XML file with three destinations (calendar, all events, voice)
  - Create bottom navigation menu XML with three items
  - _Requirements: 8.1, 8.3_
-

- [x] 2. Update MainActivity for fragment-based navigation



  - [x] 2.1 Refactor MainActivity layout to include NavHostFragment and BottomNavigationView


    - Replace existing content with FragmentContainerView for navigation
    - Add BottomNavigationView at bottom of layout
    - Add FloatingActionButton for voice input access
    - _Requirements: 8.1, 8.5_
  
  - [x] 2.2 Implement navigation setup in MainActivity


    - Initialize NavController and link with BottomNavigationView
    - Implement FAB click listener to navigate to voice fragment
    - Add destination change listener to show/hide FAB based on current fragment
    - Move existing voice input UI to VoiceFragment
    - _Requirements: 8.1, 8.2, 8.5_
-

- [x] 3. Create CalendarViewModel for data management




  - [x] 3.1 Implement CalendarViewModel class


    - Create StateFlow for all reminders
    - Create StateFlow for selected date reminders
    - Create StateFlow for selected date
    - Inject ReminderManager dependency
    - _Requirements: 1.1, 2.1, 3.1_
  

  - [x] 3.2 Implement reminder loading and grouping logic

    - Create function to load all reminders from ReminderManager
    - Implement groupRemindersByDate function to organize reminders by LocalDate
    - Create getRemindersForMonth function returning Map<LocalDate, Int> for event counts
    - Implement selectDate function to filter reminders for specific date
    - _Requirements: 1.3, 2.1, 3.2, 6.1_
  


  - [x] 3.3 Implement reminder deletion and filtering

    - Create deleteReminder function calling ReminderManager
    - Implement applyFilter function for ReminderFilter enum (ALL, PENDING, COMPLETED)
    - Add error handling with try-catch blocks
    - _Requirements: 4.2, 4.3, 7.5_

-

- [-] 4. Create calendar decorators for event indicators



  - [x] 4.1 Implement EventDecorator class

    - Create DayViewDecorator implementation for single event indicator
    - Implement shouldDecorate to check if date has events
    - Implement decorate to add dot span to calendar dates
    - _Requirements: 1.3, 6.1_
  
  - [x] 4.2 Implement MultiEventDecorator class






    - Create decorator that shows different indicators based on event count
    - Add logic for different visual styles (1-2 events, 3-5 events, 5+ events)
    - Implement background drawable for dates with many events
    - _Requirements: 6.1, 6.2, 6.3_

- [x] 5. Implement CalendarFragment



  - [x] 5.1 Create CalendarFragment layout and basic setup


    - Create fragment_calendar.xml with MaterialCalendarView
    - Add container for event list below calendar
    - Initialize CalendarViewModel
    - Set up calendar view configuration (first day of week, date range)
    - _Requirements: 1.1, 1.2_
  
  - [x] 5.2 Implement calendar event observation and decoration


    - Observe reminders StateFlow from ViewModel
    - Calculate event counts per date for current month
    - Apply EventDecorator and MultiEventDecorator to calendar
    - Update decorators when month changes
    - _Requirements: 1.3, 1.5, 6.1, 6.2_
  
  - [x] 5.3 Implement date selection and navigation


    - Set up OnDateSelectedListener for calendar
    - Call ViewModel.selectDate when date is tapped
    - Implement month navigation (previous/next buttons)
    - Add "Today" button to quickly return to current date
    - Highlight selected date on calendar
    - _Requirements: 2.1, 2.2, 2.5, 1.4_
  
  - [x] 5.4 Integrate EventListFragment for selected date


    - Embed or show EventListFragment below calendar
    - Pass selected date to EventListFragment
    - Update event list when date selection changes
    - _Requirements: 2.2, 3.1_


- [x] 6. Create EventAdapter for reminder cards




-

  - [x] 6.1 Create reminder card layout








    - Design item_reminder_card.xml with MaterialCardView
    - Add TextViews for time and message
    - Add icon for reminder status
    - Create different styles for pending and completed reminders
    - _Requirements: 3.4, 4.4, 4.5_
  

  - [x] 6.2 Implement EventAdapter class




    - Create RecyclerView.Adapter with ReminderViewHolder
    - Implement onCreateViewHolder and onBindViewHolder
    - Add submitList function using DiffUtil for efficient updates
    - Bind reminder data to card views (time, message, status)
    - Apply different styling based on remind

er status

  - [x] 6.3 Add click and delete interactions




  - _Requirements: 3.2, 3.4, 4.5_
  
  - [x] 6.3 Add click and delete interactions

    - Implement click listener for reminder cards


    - Add swipe-to-delete functionality using ItemTouchHelper
    - Create SwipeToDeleteCallback with red background and delete icon
    - Trigger onReminderDelete callback when swiped

    - _Requirements: 4.1, 4.2, 4.3_


- [ ] 7. Implement EventListFragment






  - [x] 7.1 Create EventListFragment layout and setup


    - Create fragment_event_list.xml with RecyclerView
    - Add empty state view for when no reminders exist
    - Initialize EventAdapter
    - Set up RecyclerView with LinearLayoutManager
    - _Requirements: 3.1, 3.3_
  
  - [x] 7.2 Implement reminder observation and display

    - Observe selectedDateReminders StateFlow from ViewModel
    - Submit reminder list to adapter when data changes
    - Show/hide empty state based on reminder count
    - Display "No events for this date" message when empty
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [x] 7.3 Handle reminder interactions


    - Implement onReminderClick to show reminder details (simple dialog or toast)

    - Implement onReminderDelete to call ViewModel.deleteReminder
    - Attach SwipeToDeleteCallback 
to RecyclerView
    - Show confirmation snackbar after deletion with undo option
    - _Requirements: 4.1, 4.2, 4.3_


- [x] 8. Create AllEventsAdapter with date headers





  - [x] 8.1 Create date header layout


    - Design item_date_header.xml with date text and styling
    - Use Material Design 3 typography for headers
    - Add divider or spacing for visual separation
    - _Requirements: 7.3_
  
  - [x] 8.2 Implement AllEventsAdapter with multiple view types


    - Create sealed class EventListItem with Header and ReminderItem types
    - Implement getItemViewType to distinguish headers from reminders
    - Create separate ViewHolders for headers and reminder items
    - Implement onCreateViewHolder for both view types
    - _Requirements: 7.3_
  
  - [x] 8.3 Implement list submission with date grouping


    - Create submitList function that accepts List<Reminder>
    - Group reminders by date and create EventListItem list with headers
    - Use DiffUtil for efficient updates
    - Sort reminders chronologically
    - _Requirements: 7.1, 7.2, 7.3_

- [x] 9. Implement AllEventsFragment





  - [x] 9.1 Create AllEventsFragment layout and setup


    - Create fragment_all_events.xml with RecyclerView
    - Add filter dropdown/chip group for ALL/PENDING/COMPLETED
    - Add empty state view
    - Initialize AllEventsAdapter
    - _Requirements: 7.1, 7.5_
  
  - [x] 9.2 Implement reminder observation and filtering

    - Observe allReminders StateFlow from ViewModel
    - Submit reminder list to adapter when data changes
    - Implement filter selection listener
    - Call ViewModel.applyFilter when filter changes
    - Show/hide empty state based on filtered results
    - _Requirements: 7.1, 7.2, 7.4, 7.5_
  
  - [x] 9.3 Handle reminder interactions

    - Implement onReminderClick for reminder details
    - Implement onReminderDelete to call ViewModel.deleteReminder
    - Attach SwipeToDeleteCallback to RecyclerView
    - Show confirmation snackbar after deletion
    - _Requirements: 7.1_


- [x] 10. Implement real-time calendar updates from voice input






  - [x] 10.1 Ensure ReminderManager uses Flow for reactive updates

    - Verify ReminderDao returns Flow<List<ReminderEntity>> for queries
    - Ensure ViewModel observes Flow and updates StateFlow
    - Test that database changes trigger UI updates
    - _Requirements: 5.1, 5.3_
  

  - [x] 10.2 Test voice-to-calendar integration

    - Create reminder via voice input
    - Verify calendar view updates with event indicator
    - Verify event list updates if date is selected
    - Verify all events list updates with new reminder
    - _Requirements: 5.1, 5.2, 5.4_

- [x] 11. Create drawable resources and icons




  - Create ic_calendar.xml vector drawable for calendar tab
  - Create ic_list.xml vector drawable for all events tab
  - Create ic_mic.xml vector drawable for voice tab and FAB
  - Create ic_delete.xml vector drawable for swipe-to-delete
  - Add event indicator drawables (dots, backgrounds)
  - _Requirements: 8.1_
- [x] 12. Update string resources and styling









- [ ] 12. Update string resources and styling

  - Add all UI strings to strings.xml (tab labels, empty states, error messages)
  - Add calendar-specific colors to colors.xml
  - Define Material Design 3 text styles if needed
  - Add dimension resources for spacing and sizes
  - _Requirements: 1.1, 3.3, 7.1_
- [x] 13. Implement error handling and edge cases









- [ ] 13. Implement error handling and edge cases

  - Add try-catch blocks in ViewModel for database operations
  - Display error snackbars for deletion failures
  - Handle empty reminder lists with appropriate messages
  - Test calendar behavior with no reminders
  - Test calendar behavior with many reminders on single date
  - Handle date selection for dates without reminders
  - _Requirements: 3.3, 4.3, 6.5_

-

- [x] 14. Add accessibility support




  - Add content descriptions to calendar dates with event counts
  - Add content descriptions to reminder cards
  - Add content descriptions to navigation buttons
  - Test with TalkBack screen reader
  - Ensure sufficient color contrast for event indicators
  - _Requirements: 1.1, 2.1, 3.4_

- [x] 15. Write unit tests for CalendarViewModel






  - Test reminder grouping by date logic
  - Test date selection updates selectedDateReminders
  - Test filter application (ALL, PENDING, COMPLETED)
  - Test deleteReminder calls ReminderManager correctly
  - Mock ReminderManager for isolated testing
  - _Requirements: 2.1, 3.2, 7.5_

- [x] 16. Write UI tests for calendar functionality






  - Test date selection updates event list
  - Test month navigation updates calendar
  - Test event indicators display on correct dates
  - Test swipe-to-delete removes reminders
  - Test filter changes update all events list
  - Test FAB navigation to voice input
  - _Requirements: 1.4, 2.1, 4.3, 7.5, 8.5_

- [x] 17. Write integration tests for voice-calendar flow






  - Test creating reminder via voice updates calendar immediately
  - Test reminder completion updates calendar status
  - Test deletion from calendar removes from all views
  - Test navigation preserves state between fragments
  - _Requirements: 5.1, 5.2, 5.3, 8.2, 8.4_
