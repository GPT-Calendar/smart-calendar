# Release Notes

## v0.0.1 — Beta Testing 🎉

**Release Date:** December 5, 2025

**Status:** Beta — Expect bugs and breaking changes

**Kiroween Hackathon Entry — Frankenstein Category**

---

## 🆕 What's New

### Core Features

#### 🎤 Voice-First Chat Interface
- Natural language processing for commands
- AI-powered conversation with context awareness
- Support for Ollama (local) and OpenAI backends
- Text-to-speech responses

#### 🔊 "Kiro" Wake Word Detection
- Custom wake word implementation using AudioRecord API
- Supports variants: "Kiro", "Kyro", "Kero", "Cairo", "Kira"
- Background listening with foreground service
- Smart pausing during phone calls

#### ✅ Reminders & Tasks
- Time-based reminders with natural language parsing
- Priority levels: High, Medium, Low
- Categories: Work, Personal, Shopping, Health, Custom
- Recurring schedules: Daily, Weekly, Monthly, Custom days
- Snooze options: 5, 15, 30 minutes, 1 hour
- Rich notifications with action buttons

#### ⏰ Alarms
- Full alarm management system
- Recurring alarm support
- Custom alarm sounds
- Snooze functionality

#### 📍 Location-Based Reminders
- Geofencing with Android Geofencing API
- Enter/Exit triggers
- Named places: Home, Work, Office, Gym
- Generic categories: Store, Pharmacy, Gas station
- Custom radius configuration
- Time constraints support

#### 💰 Finance Tracking
- AI-powered SMS parsing for bank transactions
- Manual transaction entry
- Budget management with category limits
- 80% budget warning alerts
- Spending insights and analytics
- Transaction filtering by date, category, type

#### 📅 Calendar & Planner
- Unified calendar view
- Visual indicators for reminders, tasks, alarms
- Create items directly from calendar
- Recurring item display

#### 📱 Home Screen Widgets
- **Voice Button (2x2):** One-tap voice input
- **Next Up (3x1):** Upcoming reminder display
- **Chat Widget (4x3):** Full chat interface on home screen

#### 💬 Floating Chat Bubble
- Persistent overlay accessible from any app
- Quick voice/text input
- Messenger-style chat heads

#### ⚙️ Settings
- Theme options: Light, Dark, System
- AI provider configuration
- Backup & Restore functionality
- Notification preferences
- Wake word toggle

---

## 📊 Technical Details

| Metric | Value |
|--------|-------|
| Kotlin Files | 100+ |
| Database Tables | 8 |
| Lines of Code | ~15,000 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Kotlin Version | 1.9.20 |
| Compose BOM | 2024.02.00 |

### Database Schema

| Table | Purpose |
|-------|---------|
| `reminders` | Time-based reminders |
| `tasks` | Task management |
| `alarms` | Alarm scheduling |
| `locations` | Saved locations |
| `transactions` | Finance transactions |
| `budgets` | Budget limits |
| `savings_goals` | Savings targets |
| `chat_messages` | Chat history |

---

## 📦 Downloads

| File | Description |
|------|-------------|
| `smart-calendar-v0.0.1-beta.apk` | Beta APK |
| `smart-calendar-v0.0.1-debug.apk` | Debug APK |
| `Source code (zip)` | Source archive |
| `Source code (tar.gz)` | Source archive |

---

## 🔧 Installation

### From APK

1. Download `smart-calendar-v0.0.1-beta.apk`
2. Enable "Install from unknown sources" in Settings
3. Open the APK file
4. Tap "Install"
5. Grant permissions when prompted

> ⚠️ **Beta Notice:** This is a beta release for testing purposes. Please report any bugs or issues you encounter.

### From Source

```bash
git clone https://github.com/yourusername/smart-calendar.git
cd smart-calendar
./gradlew installDebug
```

---

## ⚠️ Known Issues

- Wake word detection may have higher battery usage on some devices
- Location reminders require background location permission on Android 10+
- SMS parsing patterns optimized for Ethiopian banks (CBE, Awash, etc.)
- Floating bubble requires "Display over other apps" permission

---

## 🔮 Roadmap

### v1.1.0 (Planned)
- [ ] Fully offline AI with embedded LLM
- [ ] Multi-language support
- [ ] Wear OS companion app
- [ ] Widget customization options

### v1.2.0 (Planned)
- [ ] Calendar sync with Google Calendar
- [ ] Shared reminders
- [ ] Voice note attachments
- [ ] Export finance data to CSV

---

## 🙏 Acknowledgments

- Built with [Kiro IDE](https://kiro.dev) using vibe coding
- AI backends: [Ollama](https://ollama.ai/), [OpenAI](https://openai.com/)
- Maps: [OSMDroid](https://github.com/osmdroid/osmdroid) / OpenStreetMap
- Icons: Material Design Icons

---

## 📄 License

MIT License

---

## 🐛 Bug Reports

Found a bug? Please open an issue on GitHub with:
- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots/logs if available

---

<p align="center">
  <strong><em>"Speak it. Track it. Never forget it."</em></strong>
</p>
