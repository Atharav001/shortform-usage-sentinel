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
| 🎯 Scroll Tracking | Estimates Reel and Short transitions from Android accessibility scroll events |
| 🚨 Pattern Interrupt Overlay | Glassmorphic alert screen drawn directly over the target app |
| ✅ To-Do & Habit Sync | Your goals appear on the alert screen mid-scroll-hole |
| 📊 Analytics | Visual trends, daily activity, and rolling weekly comparisons |
| 🔥 Streak System | Gamified rewards for staying under your scroll limits |
| 🔒 Biometric Security | Fingerprint / Face Unlock for private usage screens |
| 🗄️ On-device Storage | Activity data is stored in a local Room database |
| 🔋 Background Tracking | Accessibility service watches supported apps and dismisses its counter when you leave them |

---

## ⚙️ How It Works

### 1. The Core Engine — Scroll Tracking

At the heart of the app is the `ScrollerAccessibilityService`.

When you enable the Accessibility Service, the app listens to UI events within Instagram and YouTube in real time. Custom-built trackers inspect accessibility scroll events and the visible video screen. Android does not provide a guaranteed “new Reel/Short” event, so counts are estimates and can vary with app versions and device accessibility behavior.

With optional Usage Access, the service periodically reads foreground time from Android's `UsageStatsManager`. Screen time is separate from scroll counting.

```
User swipes → AccessibilityService captures event
    → Tracker checks the visible video context and scroll event
        → Scroll count incremented
            → UsageStatsManager sync applied
```

---

### 2. The Pattern Interrupt — Real-Time Alert System

This is the app's most powerful feature. Once you reach a pre-configured scroll limit (e.g., 50 Reels), the app triggers a **Real-Time Intervention**:

- **Overlay Technology** — An accessibility alert appears over the supported app when the configured threshold is reached.
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
- Habit items persist indefinitely; completion is recorded by local calendar date.

**Universal Sync**
- Tasks and habits added anywhere — on the dashboard, or even from inside the alert overlay mid-scroll — are immediately synced across the entire app.
- If you add a goal while stuck in a scroll hole, it will be waiting for you on the dashboard when you return.

---

### 4. Advanced Data Analytics

The dashboard serves as a **Command Center** for your digital health.

- **Visual Trends** — The dashboard compares today's app screen time with yesterday; History compares rolling seven-day windows.
- **Streak System** — Stay under your scroll limit for the day and you earn a streak. Consecutive days of disciplined usage are tracked and celebrated, gamifying the process of intentional living.
- **Biometric Security** — Users who want to keep their usage habits private can gate detailed analysis screens behind **Fingerprint or Face Unlock**.

---

### 5. Privacy & Efficiency

**The app has no account or app-operated sync service.** Android's device backup settings may back up app data.

- All scroll events, tasks, habits, and analytics are stored locally using a **Room Database** (Android's native SQLite abstraction layer).
- No user accounts or app-operated cloud sync.
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
- Accessibility Service permission for scroll counts
- Optional Usage Stats permission (`PACKAGE_USAGE_STATS`) for screen time
- Optional Display over other apps permission (`SYSTEM_ALERT_WINDOW`) for the floating counter

### Setup Steps

1. **Install** the app and open it.
2. **Grant Accessibility Service** — Navigate to *Settings → Accessibility → Scroller's Dashboard* and enable it.
3. **Grant Usage Access** if you want screen-time totals.
4. **Grant Overlay Permission** if you want the floating counter.
5. **Set your scroll limits** for Instagram Reels and YouTube Shorts.
6. **Add your To-Do items and Habits** to the Goals page.
7. Start scrolling — and let the app hold you accountable.

---

## 📸 Screenshots

<table>
  <tr>
    <td align="center">
      <strong>🏠 Home Page</strong><br/><br/>
      <img src="https://github.com/user-attachments/assets/91019c4c-2ec7-401e-aa68-4f04a1d7ffca" alt="Home Page" width="220"/>
      <br/><sub>Live scroll counts, streaks & trend indicators</sub>
    </td>
    <td align="center">
      <strong>⚡ Activity Center</strong><br/><br/>
      <img src="https://github.com/user-attachments/assets/bbd6717d-ef5c-4734-99a8-2661a8b1d144" alt="Activity Center" width="220"/>
      <br/><sub>Real-time session breakdown & app usage</sub>
    </td>
    <td align="center">
      <strong>✅ Goals — To-Do List</strong><br/><br/>
      <img src="https://github.com/user-attachments/assets/97283e27-1403-4de4-b2b7-6274a379003f" alt="Goals To-Do List" width="220"/>
      <br/><sub>Daily & permanent tasks at a glance</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <strong>🔁 Goals — Habit List</strong><br/><br/>
      <img src="https://github.com/user-attachments/assets/94685165-3752-444b-9ed7-e803e338fd0e" alt="Habit List" width="220"/>
      <br/><sub>Recurring habits with 24-hour reset</sub>
    </td>
    <td align="center">
      <strong>📅 History</strong><br/><br/>
      <img src="https://github.com/user-attachments/assets/1046188d-d2e2-4743-9ddb-df6048492830" alt="History" width="220"/>
      <br/><sub>Past usage and scroll trends</sub>
    </td>
    <td align="center">
      <strong>⚙️ Settings</strong><br/><br/>
      <img src="https://github.com/user-attachments/assets/0570299c-ebdc-454c-87fc-d0a81afa4982" alt="Settings" width="220"/>
      <br/><sub>Scroll limits, biometric lock & preferences</sub>
    </td>
  </tr>
</table>

> 📁 Place your screenshot images in a `/screenshots` folder at the root of the repository and name them as referenced above. Recommended resolution: **1080 × 2340px** (or any standard portrait phone resolution).

---

## 📱 App Sections

```
📱 Scroller's Dashboard
├── 🏠  Dashboard        → Live scroll counts, screen time, streak, trend indicators
├── 📋  Goals            → To-Do list and Habit tracker with daily/permanent modes
├── 📊  Analytics        → Historical data, seven-day comparisons, and activity charts
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

> Tracking is processed on-device. Android backup behavior depends on device settings.

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
