# 📱 Scroller's Dashboard: Structural Usage Tracker

A precision-engineered Android Accessibility Service designed to monitor consumption habits on **Instagram Reels** and **YouTube Shorts**. This project moves beyond simple "Screen Time" to track **Content Density** using structural UI analysis and physics-based debouncing.

---

## 🧠 The Philosophy: Why "Screen Time" is a Lie
Standard digital wellbeing tools tell you how long you’ve been on an app, but they fail to measure the true cost of "Infinite Scroll."

* **The Problem with Minutes:** 30 minutes spent watching a single educational documentary is vastly different for your brain than 30 minutes spent watching 120 ten-second clips.
* **The Dopamine Loop:** Short-form content is engineered for "Rapid Novelty." Every swipe is a fresh dopamine hit.
* **The Goal:** This app treats every swipe as a "Context Switch." By tracking the quantity of videos rather than just duration, users get a true look at how many times their focus was hijacked.

---

## 🛠️ Technical Implementation

### 1. Structural Detection (Component Filtering)
Instead of monitoring raw pixels, the service targets specific UI engines:
* **Targeting:** `RecyclerView` and `ViewPager` components.
* **Why:** These are the specific containers used for vertical video paging. This filters out noise from Settings menus or DM lists.

### 2. The 70% "Golden Ratio" Rule
To separate the full-screen video player from half-screen elements:
* **The Logic:** A scroll is only validated if the moving container covers **at least 70% of the screen height**.
* **Result:** This "Sweet Spot" automatically ignores interactions in the comment sections or navigation bars.

### 3. 900ms "Physics Debouncing"
To solve the "Micro-Scroll" problem—where a single physical flick triggers multiple system events:
* **The Rule:** "One-at-a-Time."
* **The Logic:** Once a scroll is detected, the engine "blinks" and refuses to count anything else for 900ms (Instagram) or 800ms (YouTube). This eliminates jitter while remaining fast enough to catch "speed-scrollers."

---

## 📸 Interface & Logic Visualization

| Tracking Dashboard | Accessibility Setup | Structural Detection |
| :---: | :---: | :---: |
| <img width="786" height="1600" alt="image" src="https://github.com/user-attachments/assets/7d67b7d8-cd74-4c74-a220-355ee97fedf0" />| <img width="712" height="1406" alt="image" src="https://github.com/user-attachments/assets/d4be32e1-cf78-4bdc-a194-e04003468f6c" />| <img width="785" height="1600" alt="image" src="https://github.com/user-attachments/assets/db1e634d-98a5-4cfb-bc43-416dc19a1e04" />
 


---

## 🔒 Privacy & Ethics
**Privacy-First by Design:**
* **No Screen Recording:** The app never captures screenshots or video. It "sees" the structure of the UI, not the content.
* **Local Processing:** All structural metadata is processed on-device. No data is sent to external servers.
* **Transparency:** The Accessibility Service is used strictly for geometric validation of scroll containers.

---

## 🚀 Development Process
This application was developed iteratively using **Android Studio AI** as a pair-programmer. 
* **Logic Evolution:** Refined through multiple phases—from 2-minute "session" cooldowns to the final 900ms "Human-Gesture" logic.
* **Refinement:** Logic was tracked and adjusted via **Local History** to ensure near-perfect capture rates across different device aspect ratios.

---

## 📄 License
This project is licensed under the **MIT License** - see the LICENSE file for details.
