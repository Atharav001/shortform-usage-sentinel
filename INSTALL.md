# 🚀 Installation & Setup Guide

Follow these steps to set up the development environment for **Scroller's Dashboard** or to sideload the app onto your device.

---

## 🛠️ Prerequisites
* **Android Studio Jellyfish** (or newer)
* **Android SDK 34+** (Android 14 "Upside Down Cake")
* **Physical Android Device:** Recommended for testing Accessibility Services (emulators often struggle with precise gesture physics).

---

## 💻 Developer Setup
1. **Clone the Repository:**
   ```bash
   git clone [https://github.com/Atharav001/shortform-usage-sentinel.git](https://github.com/Atharav001/shortform-usage-sentinel.git)

_(Note: Ensure you use the updated project URL)._

2\. **Open in Android Studio:** Allow the Gradle sync to complete. This project utilizes Android Studio AI features; ensuring your IDE is up to date will help with viewing the iterative logic suggestions in the codebase.3. **Build the APK:** Go to Build > Build Bundle(s) / APK(s) > Build APK(s).

3\. Enabling the Service (Crucial Step)
---------------------------------------

Because this app uses the **Accessibility Service API**, it will not track data until manual permission is granted:

1.  **Install the APK** on your device.
    
2.  Navigate to **Settings** > **Accessibility**.
    
3.  Find **"Scroller's Dashboard"** under the 'Downloaded Apps' or 'Installed Services' section.
    
4.  Toggle **Use Service** to **ON**.
    

4\. Verification
----------------

1.  **Open Instagram or YouTube.**
    
2.  **Navigate to the Reels or Shorts tab.**
    
3.  **Perform a few full-screen swipes.**
    
4.  **Return to the app** to see the **"Buffered"** counts update in real-time.
    

🧪 Technical Deep Dive: The Validation Flow
-------------------------------------------

The core engine follows this decision tree for every scroll event detected to ensure near-perfect accuracy:

*   **App Check:** Is the packageName either com.instagram.android or com.google.android.youtube?
    
*   **Class Check:** Is the className a RecyclerView or ViewPager?
    
*   **Geometry Check:** Does the AccessibilityNodeInfo bounds-in-screen height cover **\> 70%** of the device's y-axis?
    
*   **Debounce Check:** Has it been **\> 900ms** since the last validated increment?
    

🤝 Contributing
---------------

I am actively looking to refine the **70% Golden Ratio** for foldable devices and tablets. If you have data on UI node bounds for devices like the Z Fold or Pixel Tablet, please open an issue or a PR!
