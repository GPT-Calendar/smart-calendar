# Implementation Plan

- [x] 1. Create global navigation components
  - Implement MinimalHeader: deep blue (#305CDE) background, 64dp height, 4dp elevation, white title (Roboto Medium 18sp), white icons (24dp)
  - Implement MinimalBottomNavigation: deep blue background, 64dp height, 8dp upward elevation
  - Navigation items: Assistant, Calendar, Finance, All Events, Map with icons
  - Inactive items: white with 0.7 opacity, Roboto Regular 11sp, 56dp × 56dp touch targets
  - Active item: light blue (#5D83FF), Roboto Medium 11sp, 3dp indicator line above icon
  - Update MainActivity to integrate new navigation components
  - Implement navigation state management and screen switching
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8_

- [x] 2. Redesign Assistant (Chat) screen
  - Update ChatScreen layout with white (#FFFFFF) background
  - Redesign MinimalAIBubble: left-aligned, very light grey (#F0F0F0) background, 16dp corner radius (4dp top-left pointer), max 75% width
  - Redesign MinimalUserBubble: right-aligned, plain white background, 16dp corner radius (4dp top-right pointer), max 75% width
  - Both bubbles: Roboto Regular 15sp dark grey (#333333) text, 12dp horizontal + 10dp vertical padding
  - Update MinimalInputField: white background, 24dp corner radius (pill), 48dp min height
  - Voice button: circular, light blue (#5D83FF) accent, 48dp × 48dp, white mic icon 24dp
  - Add embedded chart support: light blue (#5D83FF) lines on white background
  - Add calendar date summary display with light blue accent for dates
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [x] 3. Redesign Calendar screen
  - Update CalendarScreen layout with white (#FFFFFF) background
  - Redesign calendar grid: white background, dark grey date text (Roboto Regular 14sp)
  - Selected date: circle with light blue (#5D83FF) accent, 48dp × 48dp touch targets
  - Event indicators: light blue (#5D83FF) dot for single event, numbered badge for multiple
  - Redesign event cards: white background, 8dp corner radius, 12-16dp padding
  - Event timestamps: light blue (#5D83FF), Roboto Regular 14sp
  - Event message: dark grey, Roboto Regular 15sp
  - Add floating action button: light blue (#5D83FF), 56dp × 56dp, white icon 24dp, 6dp elevation
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4. Redesign Finance screen
  - Update FinanceScreen layout with white (#FFFFFF) background
  - Redesign summary card: deep blue (#305CDE) background, 12dp corner radius, 20dp padding, 4dp elevation
  - Summary text: white, Roboto Medium 18sp (amount), Roboto Regular 14sp (period)
  - Redesign chart: white background, light blue (#5D83FF) data lines 2dp width, light grey (#E0E0E0) grid lines 1dp
  - Chart labels: dark grey, Roboto Regular 12sp
  - Redesign transaction items: white background, 12dp vertical padding
  - Transaction description: dark grey, Roboto Regular 15sp
  - Positive amounts: light blue (#5D83FF), Roboto Medium 15sp
  - Negative amounts: dark red (#D32F2F), Roboto Medium 15sp
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

- [x] 5. Redesign All Events (Timeline) screen
  - Update AllEventsScreen layout with white (#FFFFFF) background
  - Redesign timeline items: white background, 8dp corner radius, 12-16dp padding
  - Timeline connector: vertical light blue (#5D83FF) line, 2dp width
  - Timestamps: light blue (#5D83FF), Roboto Regular 14sp
  - Event messages: dark grey, Roboto Regular 15sp
  - Ensure proper spacing between timeline items (8-12dp)
  - Handle last item styling (timeline line ends at middle)
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 6. Redesign Map screen
  - Update MapScreen layout with light-themed map (white/grey streets)
  - Add floating search bar: deep blue (#305CDE) background, 24dp corner radius (pill), 48dp height, 4dp elevation
  - Search bar text: white, Roboto Regular 15sp
  - Search bar icons: white, 24dp
  - Add floating navigate button: circular, light blue (#5D83FF), 56dp × 56dp, white icon 24dp, 6dp elevation
  - Position navigate button in bottom corner with 16dp margin
  - Update map markers and info cards to match color scheme
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 7. Implement typography and spacing consistency
  - Audit all screens for typography: headers (Roboto Medium 18sp), body (Roboto Regular 15sp), labels (Roboto Regular 14sp), navigation (Roboto Regular/Medium 11sp), timestamps (Roboto Regular 12-14sp)
  - Verify spacing: 16dp horizontal padding on content areas, 8-12dp vertical spacing between items, 8-12dp corner radius on cards, 12-16dp internal card padding
  - Verify floating button positioning: 16dp margin from screen edges
  - Update any inconsistent components to match specifications
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 8. Verify color strategy compliance and accessibility
  - Audit all screens for 60-30-10 color allocation: ~60% white backgrounds, ~30% deep blue elements, ~10% light blue accents
  - Verify primary CTAs use light blue (#5D83FF) accent color
  - Verify indicators use light blue (#5D83FF) accent color
  - Test contrast ratios: minimum 4.5:1 for text on white, minimum 8.6:1 for text on deep blue
  - Verify touch targets: minimum 48dp × 48dp for interactive elements, minimum 56dp × 56dp for navigation items
  - Add content descriptions to all icons for screen readers
  - Test navigation flow between all screens
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 9. Final testing and polish
  - Test navigation between all 5 screens with state preservation
  - Test each screen's functionality (chat, calendar selection, finance data, timeline, map)
  - Verify visual consistency across all screens
  - Test on different screen sizes and orientations
  - Test accessibility with TalkBack
  - Fix any visual inconsistencies or bugs
  - Verify all animations run smoothly at 60fps
  - Conduct final visual review against design specifications
