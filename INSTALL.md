# 🚀 Installation & Setup Guide

This guide covers how to install **Scroller's Dashboard** directly on your Android device or set up the development environment.

---

## 📲 Option 1: Direct Installation (Recommended for Users)

If you just want to use the app to track your usage, follow these steps directly on your Android device:

1. **Download the APK:** Navigate to the [Releases](https://github.com/Atharav001/shortform-usage-sentinel/releases) section of this repository and download the latest `.apk` file.
2. **Allow Unknown Sources:** If prompted by your browser, allow "Installation from Unknown Sources."
3. **Install:** Open the downloaded file and tap **Install**.

### 🛠️ Required Setup (Crucial)
Because this app uses a privacy-first structural tracking method, you must manually enable the engine:
1. Open **Settings** > **Accessibility**.
2. Tap on **Installed Apps** or **Downloaded Services**.
3. Select **Scroller's Dashboard** and toggle the switch to **ON**.
4. **Note:** This permission allows the app to detect when a Reel/Short container is on screen. **No screen recording or data collection occurs.**

[Image of Android Accessibility settings menu showing the toggle to enable a service]

---

## 💻 Option 2: Developer Setup (For Code Contribution)

If you want to modify the code or build the app from source:

1. **Prerequisites:**
   * Android Studio Jellyfish+
   * Android SDK 34
2. **Clone the repository:**
   ```bash
   git clone [https://github.com/Atharav001/shortform-usage-sentinel.git](https://github.com/Atharav001/shortform-usage-sentinel.git)

1.  **Build:** Open in Android Studio and go to Build > Build APK(s).
    

🧪 Verification
---------------

To ensure the logic is calibrated correctly for your device:

1.  Open Instagram Reels.
    
2.  Perform a **short flick** (under 900ms). The counter should only increment once due to the **Physics Debounce** rule.
    
3.  Open the **Comments section**. Scroll through them; the counter should **not** move, as the container height is below the **70% Threshold**.
    

🤝 Contributing
---------------

I am actively looking to refine the **70% Golden Ratio** for foldable devices and tablets. If you have data on UI node bounds for devices like the Z Fold or Pixel Tablet, please open an issue or a PR!
