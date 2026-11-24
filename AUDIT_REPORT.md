# Scroller's Dashboard: logic audit

Date: 24 November 2026. Scope: Android source, manifest, accessibility configuration, database, dashboard, settings, history, goals, overlays, and project documentation in the current working tree. This is a source audit; no real Instagram, YouTube, Vanced, or ReVanced accessibility trace was available.

## Product invariant

The scroll count should increase only when a user advances within the dedicated Instagram Reels or YouTube Shorts vertical video pager. Browsing the home feed, a Shorts shelf, comments, profiles, search results, a horizontal carousel, or a normal long-form video must not increase it. Every accepted count should appear once in the daily total and once in event history. Missing access or paused tracking must not be presented as a verified zero.

Android reports view scroll events, not a canonical “new Reel/Short” action. The implementation must therefore prefer conservative evidence and expose the remaining uncertainty. See [Android's AccessibilityEvent documentation](https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent#TYPE_VIEW_SCROLLED).

## Tester scenarios and findings

| Scenario | Previous failure path | Change or remaining risk |
|---|---|---|
| Scroll Instagram home posts or a Reels preview | A large home feed could be counted if any large Reel-labelled node existed elsewhere in the active tree. | Count gate now requires the event source to be a large pager with a Reel/Clips ID in its own parent chain. Device traces are still needed for Instagram variants. |
| Scroll YouTube home, search, comments, or Shorts shelf | The YouTube tracker accepted large `FrameLayout` and `RecyclerView` events; an unrelated Shorts label could satisfy the separate screen scan. | The service now rejects events outside a large explicit Shorts pager. Horizontal-only events are rejected on supported Android versions. |
| Swipe quickly through two videos | Fixed 800–900 ms debounce can merge two real transitions. | Still unresolved without observed pager index or stable video identity. Reducing the debounce blindly would increase double counts. |
| One swipe emits multiple events, or a slow drag settles twice | A time debounce can count one gesture twice when events are far apart. | Still possible. A device trace should compare event source, visible index, and final video identity before replacing the debounce. |
| Navigate away immediately after a swipe | Asynchronous alert work could draw over another app. | Alert now checks the active root package before displaying. Accepted counts use the event's captured timestamp and date. |
| Scroll around midnight or leave the app open overnight | A delayed batch could land on the wrong day; screen observers could keep showing yesterday. | Batching was removed. Dashboard, Activity, and floating counter refresh their calendar date while open. |
| Service is destroyed immediately after an event | Cancelling the service scope could discard a pending database write. | Database writes now use a process-level persistence scope; an abrupt process kill before the write still cannot be ruled out. |
| Two counts arrive while screen-time sync runs | Read/replace writes could lose increments or overwrite screen time. | Count, event insertion, and screen-time updates now use Room transactions. A unique date/app index prevents duplicate summary rows. |
| User resets counts from Settings | The app could erase today's count and events while still claiming to retain history. | Reset controls and delete DAO operations were removed. Android app-data deletion and uninstall remain outside app control. |
| Upgrade a database | Destructive migration could wipe recorded history. | Destructive fallback was removed. Data-preserving migrations cover the repository's known version 4→5 and 5→6 schemas. Older unknown schemas require a migration before installation. |
| Disable tracking or revoke accessibility | Zero or incomplete counts can look like successful restraint. | Dashboard now labels an app-level tracking pause and suppresses its streak. Historical partial-day coverage is not recorded yet; older days must not be treated as verified complete data. |
| Change limits after prior days | History and streak calculations apply current limits to past days. | Remaining issue: historical goal snapshots are not stored. Labels and achievements can be reinterpreted after a goal change. |
| Use a custom ReVanced package | Only known package IDs are recognized. | Vanced plus three YouTube package IDs are supported. Custom package names require a controlled allowlist setting or per-build support. |
| Open an overlay, switch apps, or cross midnight | Floating service could keep stale data or remain running after hide. | Overlay observer rolls over daily; hide stops the foreground service and it is no longer sticky. Rapid app switching still needs a device run. |
| Reinstall or restore data | README claimed data never leaves the device. | Documentation now states that Android device backup can apply. No app-operated sync is configured. |
| Revoke Usage Access after time was recorded | An empty usage query could overwrite the day's screen time with zero. | Sync now requires Usage Access and never reduces an existing daily screen-time value. |
| Deny overlay permission or restrict foreground service launch | Starting the floating service or adding its window could throw and leave the service running. | Startup and window errors are handled; failed overlays stop their foreground service. |
| Encounter a malformed legacy date in History | Forced date parsing could crash the History screen. | Invalid record dates are skipped in monthly grouping and achievement comparisons. |

## Remaining product risks

1. **Exact per-video counts are unproven.** Android accessibility does not guarantee that a swipe produces one scroll event. The conservative pager gate reduces false positives but may miss layouts with generic IDs. Instrumentation must capture several real app versions before claiming exact counts.
2. **Coverage is not stored.** A day with permissions revoked, tracking paused, battery restrictions, or a service crash can be incomplete. The database stores totals but not intervals of valid observation. Historical “under goal” and achievements should require coverage metadata before they are presented as verified outcomes.
3. **Goals are not versioned by day.** Streaks and historical success are recalculated against current settings. Store a per-day goal snapshot to make historical judgments stable.
4. **Screen time covers the whole target app.** `UsageStatsManager` foreground time does not isolate Reels or Shorts. The UI and documentation should continue to distinguish it from video scroll counts.
5. **Third-party UI drift is expected.** Instagram and YouTube can rename view IDs or change their accessibility trees without an app update. A small, privacy-conscious diagnostic log of package, source class, ID, bounds, and scroll indices would help maintain the detector without recording video content.

## Required device checks before a release claim

For each supported package, compare manual video transitions with the in-app count across: dedicated feed up/down swipes, rapid swipes, slow drags, revisiting a prior video, taps to next video, home feed and Shorts/Reels shelves, comments, search, profiles, horizontal carousels, picture-in-picture, screen rotation, overlay open/close, midnight, tracking pause/re-enable, permission revocation, app restart, and process kill. Record both false positives and missed transitions. A build and this matrix are required before calling the detector reliable.

## Verification status

Android XML files parsed successfully in the previous review. A full Gradle build and device run remain unavailable because the machine has roughly 100–300 MB of free disk space; Gradle reports `No space left on device` while creating its generated API jar, before Kotlin compilation. No APK from these changes has been verified.
