# 📵 Scroller's Dashboard

> **Your digital conscience. Built for intentional living.**

Scroller's Dashboard is a high-performance digital wellness application designed to break the cycle of mindless scrolling on short-form video platforms like **Instagram Reels** and **YouTube Shorts**. Unlike traditional screen-time apps that only count minutes, this app measures the physical act of scrolling — forcing you to confront exactly how much content you are consuming.

---

## 🧠 The Problem

Short-form video platforms are engineered to keep you scrolling. A "30-minute" session often means dozens — sometimes hundreds — of videos consumed without a single conscious decision. Standard screen-time tools tell you *how long* you were on your phone. They don't tell you *how deep* you went.

Scroller's Dashboard does.

---

## ✨ Features at a Glance

| Feature | Description |
|---|---|
| 🎯 Precision Scroll Tracking | Counts every Reel and Short, not just app-open minutes |
| 🚨 Pattern Interrupt Overlay | Glassmorphic alert screen drawn directly over the target app |
| ✅ To-Do & Habit Sync | Your goals appear on the alert screen mid-scroll-hole |
| 📊 Advanced Analytics | Visual trends, 3-day averages, and improvement indicators |
| 🔥 Streak System | Gamified rewards for staying under your scroll limits |
| 🔒 Biometric Security | Fingerprint / Face Unlock for private usage screens |
| 🗄️ 100% Local Storage | All data lives on your device — never on a server |
| 🔋 Low Battery Impact | Background engine activates only when target apps are open |

---

## ⚙️ How It Works

### 1. The Core Engine — Precision Tracking

At the heart of the app is the `ScrollerAccessibilityService`.

When you enable the Accessibility Service, the app listens to UI events within Instagram and YouTube in real time. Custom-built trackers — `InstagramTracker` and `YouTubeTracker` — analyze screen height and touch events to detect every swipe to a new video. It doesn't merely detect that the app is open; it counts each individual flick.

The service also periodically syncs with Android's `UsageStatsManager` to ensure the total screen-time displayed matches the system's official records, keeping data accurate and trustworthy.

```
User swipes → AccessibilityService captures event
    → Tracker validates scroll gesture
        → Scroll count incremented
            → UsageStatsManager sync applied
```

---

### 2. The Pattern Interrupt — Real-Time Alert System

This is the app's most powerful feature. Once you reach a pre-configured scroll limit (e.g., 50 Reels), the app triggers a **Real-Time Intervention**:

- **Overlay Technology** — An alert is drawn directly over Instagram or YouTube using Android's overlay permission, making it impossible to ignore.
- **Psychological Redirection** — Instead of a simple "stop" message, the overlay displays your **To-Do List** and **Daily Habits**, placing your actual goals side-by-side with your scroll count.
- **The Conscious Choice** — You are presented with two options:
  - 🏠 **Quit & Take a Break** — Sends you back to the home screen.
  - ▶️ **Continue** — Lets you scroll on, but only as a deliberate, conscious decision.

The goal is not to lock you out — it's to create a moment of mindfulness in the middle of autopilot behavior.

---

### 3. Goal & Habit Synchronization

Scroller's Dashboard treats your productivity and your scrolling as two sides of the same coin.

**To-Do List**
- Add tasks you need to complete today.
- Choose **Refresh Daily** mode to auto-clear at midnight, or keep tasks **permanent** until you mark them done.

**Habit Tracker**
- For recurring behaviors like *"Drink Water"*, *"Meditate"*, or *"Read for 20 minutes"*.
- Habit items persist indefinitely but their **completion status resets every 24 hours**.

**Universal Sync**
- Tasks and habits added anywhere — on the dashboard, or even from inside the alert overlay mid-scroll — are immediately synced across the entire app.
- If you add a goal while stuck in a scroll hole, it will be waiting for you on the dashboard when you return.

---

### 4. Advanced Data Analytics

The dashboard serves as a **Command Center** for your digital health.

- **Visual Trends** — Today's scroll count and screen time are compared against your **3-day rolling average**, displayed in clear green (improving) or red (slipping) indicators.
- **Streak System** — Stay under your scroll limit for the day and you earn a streak. Consecutive days of disciplined usage are tracked and celebrated, gamifying the process of intentional living.
- **Biometric Security** — Users who want to keep their usage habits private can gate detailed analysis screens behind **Fingerprint or Face Unlock**.

---

### 5. Privacy & Efficiency

**Your data never leaves your device.**

- All scroll events, tasks, habits, and analytics are stored locally using a **Room Database** (Android's native SQLite abstraction layer).
- No user accounts. No cloud sync. No data sold or shared.
- The background tracking engine is optimized for minimal battery drain, activating only when a target app (Instagram or YouTube) is detected as open.

---

## 🛠️ Technical Overview

| Component | Technology |
|---|---|
| Platform | Android (Native) |
| Background Service | `AccessibilityService` API |
| Screen Time Data | `UsageStatsManager` API |
| Alert Overlay | Android Overlay Permission (`TYPE_APPLICATION_OVERLAY`) |
| Local Database | Room Database (SQLite) |
| Scroll Tracking | Custom `InstagramTracker` & `YouTubeTracker` |
| Biometrics | Android BiometricPrompt API |

---

## 🚀 Getting Started

### Prerequisites

- Android **6.0 (Marshmallow)** or higher
- Accessibility Service permission
- Usage Stats permission (`PACKAGE_USAGE_STATS`)
- Display over other apps permission (`SYSTEM_ALERT_WINDOW`)

### Setup Steps

1. **Install** the app and open it.
2. **Grant Accessibility Service** — Navigate to *Settings → Accessibility → Scroller's Dashboard* and enable it.
3. **Grant Usage Access** — Navigate to *Settings → Digital Wellbeing / Usage Access* and allow the app.
4. **Grant Overlay Permission** — Allow the app to display over other apps when prompted.
5. **Set your scroll limits** for Instagram Reels and YouTube Shorts.
6. **Add your To-Do items and Habits** to the Goals page.
7. Start scrolling — and let the app hold you accountable.

---

## 📸 App Sections

```
📱 Scroller's Dashboard
├── 🏠  Dashboard        → Live scroll counts, screen time, streak, trend indicators
├── 📋  Goals            → To-Do list and Habit tracker with daily/permanent modes
├── 📊  Analytics        → Historical data, 3-day averages, and improvement charts
├── ⚙️  Settings         → Scroll limits, daily refresh, biometric lock, app config
└── 🚨  Alert Overlay    → Real-time pattern interrupt drawn over Instagram/YouTube
```

---

## 🔐 Permissions Explained

| Permission | Why It's Needed |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | To detect and count scroll events inside Instagram and YouTube |
| `PACKAGE_USAGE_STATS` | To read official screen-time data from Android |
| `SYSTEM_ALERT_WINDOW` | To display the intervention overlay above other apps |
| `USE_BIOMETRIC` | To lock detailed analytics behind fingerprint / face unlock |

> All permissions are used exclusively on-device. No data is transmitted externally.

---

## 💡 Philosophy

> *"Every reel you watch is a choice. This app just makes sure it's actually a choice."*

Scroller's Dashboard is not about shame or restriction. It is about **awareness**. The moment you see *"You've watched 47 Reels in the last 20 minutes"* displayed over your feed, the illusion of passive consumption breaks. That moment of friction is where intentional living begins.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions, feature requests, and bug reports are welcome. Please open an issue or submit a pull request.

---

<p align="center">
  Built with purpose. Designed for focus. Made to help you reclaim your time.
</p>
