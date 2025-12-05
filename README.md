<p align="center">
  <img src="icons/icon.jpg" alt="Smart Calendar Logo" width="120" height="120" style="border-radius: 20px;">
</p>

<h1 align="center">Smart Calendar</h1>

<p align="center">
  <strong>AI-Powered Android Personal Assistant</strong><br>
  Voice commands • Finance tracking • Location reminders • Smart scheduling
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-26%2B-green?logo=android" alt="Android 26+">
  <img src="https://img.shields.io/badge/Kotlin-1.9.20-purple?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue" alt="Compose">
  <img src="https://img.shields.io/badge/Built%20with-Kiro-orange" alt="Built with Kiro">
</p>

<p align="center">
  🎃 <em>Kiroween Hackathon Entry — Frankenstein Category</em>
</p>

---

## ✨ Features

### 🎤 Voice-First Interface
Talk naturally to your assistant:
- *"Remind me to call John at 3 PM tomorrow"*
- *"I spent 500 birr on groceries today"*
- *"Set an alarm for 7 AM every weekday"*
- *"Remind me to buy milk when I reach the store"*

### 🔊 "Kiro" Wake Word
Say **"Kiro"** followed by your command — even with your phone in your pocket. Custom implementation using Android's AudioRecord API with support for variants: Kiro, Kyro, Kero, Cairo, Kira.

### 📍 Location-Based Reminders
Geofencing-powered reminders that trigger when you arrive or leave locations:
- Named places (home, work, gym)
- Generic categories (any store, pharmacy)
- Custom radius and time constraints

### 💰 Finance Tracking
- AI-powered SMS parsing for automatic transaction detection
- Manual entry with categorization
- Budget management with 80% warnings
- Spending insights and analytics

### ✅ Tasks & Alarms
- Priority levels with color coding
- Categories: Work, Personal, Shopping, Health
- Recurring schedules (daily, weekly, monthly)
- Snooze options and rich notifications

### 📱 Home Screen Widgets
- **Voice Button (2x2)** — One-tap voice input
- **Next Up (3x1)** — Upcoming reminder display
- **Chat Widget (4x3)** — Full chat interface

### 💬 Floating Chat Bubble
Persistent overlay for quick access from any app.

---

## 🏗️ Architecture

```
app/src/main/java/com/example/voicereminder/
├── data/           # Room DAOs, entities, repositories
├── domain/         # Business logic, managers, models
├── presentation/   # Jetpack Compose UI screens
├── receivers/      # Broadcast receivers
├── sms/            # SMS parsing
└── widget/         # Home screen widgets
```

**Key Stats:**
- 100+ Kotlin files
- 8 database tables
- ~15,000 lines of code

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3 |
| Database | Room (SQLite) |
| AI | Ollama API, OpenAI API |
| Maps | OSMDroid (OpenStreetMap) |
| Location | Android Geofencing API |
| Voice | SpeechRecognizer, TextToSpeech, AudioRecord |
| Async | Kotlin Coroutines, Flow |
| Background | WorkManager, Foreground Services |

---

## 🚀 Getting Started

### Prerequisites
- Android SDK 26+ (Android 8.0 Oreo)
- Android Studio Hedgehog or later
- Kotlin 1.9.20

### Build & Install

```bash
# Clone the repository
git clone https://github.com/yourusername/smart-calendar.git
cd smart-calendar

# Build
./gradlew build

# Install on connected device
./gradlew installDebug
```

### Permissions
Grant these when prompted:
- **Microphone** — Voice input & wake word
- **Notifications** — Reminder alerts
- **Location** — Geofencing
- **SMS** — Finance tracking (optional)

---

## 🎙️ Voice Commands

| Type | Examples |
|------|----------|
| **Time Reminders** | "Remind me to [task] at [time]" |
| **Location Reminders** | "Remind me to [task] when I reach [place]" |
| **Tasks** | "Add task [description] with high priority" |
| **Alarms** | "Set alarm for [time] every weekday" |
| **Finance** | "I spent [amount] on [description]" |

---

## 📁 Kiro Specs

Feature specifications are in `.kiro/specs/`:

```
.kiro/specs/
├── multi-screen-ui/
├── chat-enhancements/
├── finance-enhancements/
├── reminder-task-alarm-enhancements/
└── calendar-ui-enhancement/
```

Each contains `requirements.md`, `design.md`, and `tasks.md`.

---

## 🎨 Design System

**Color Strategy (60-30-10):**
- 60% White (#FFFFFF) — Backgrounds
- 30% Deep Blue (#305CDE) — Branding
- 10% Light Blue (#5D83FF) — Accents

**Accessibility:**
- 48dp minimum touch targets
- 4.5:1 contrast ratio
- Screen reader support

---

## 🏆 Kiroween Hackathon

**Category:** Frankenstein — Stitching together incompatible technologies

**Technologies Combined:**
1. Voice AI (natural language understanding)
2. Finance Tracking (SMS parsing, budgeting)
3. Location Services (geofencing, maps)
4. Task Management (reminders, tasks, alarms)
5. Widget System (home screen integration)

**Development:** 90% vibe coding, 10% spec-driven development with [Kiro IDE](https://kiro.dev)

---

## 📄 License

MIT License — See [LICENSE](LICENSE) file

---

<p align="center">
  <strong><em>"Speak it. Track it. Never forget it."</em></strong>
</p>

<p align="center">
  Built with ❤️ using <a href="https://kiro.dev">Kiro</a>
</p>
