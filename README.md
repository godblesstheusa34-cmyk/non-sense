# Fluid Home

Fluid Home is a real, hardware-accelerated Android home-screen launcher inspired by the depth and direct manipulation of HTC Sense 3, rebuilt on current public Android APIs. It does **not** ship or load HTC's obsolete framework. The two supplied research archives are intentionally kept as reference inputs; both were extracted into one ignored `reference/HTC-Sense3-decoded` tree during development.

## What works

- HOME/DEFAULT intent registration, so Android can select Fluid Home as the default launcher.
- Runtime window-inset and display sizing (status/navigation bars, display scaling, rotation, and camera cutout), rather than hard-coded S24 Ultra pixels or DPI.
- Five home pages, installed launchable apps, an app drawer, app launching, long-press icon placement/removal, and persistent layout preferences.
- System `AppWidgetHost` integration: provider picker, bind permission, configuration, persisted widgets, resize, page move, and removal.
- Stationary system wallpaper with a finger-origin, direction-aware wave. The current and incoming icon planes translate while a localized crest lifts, scales, tilts, fades, and moves shadows. It deliberately avoids cube/cylinder rotation.
- `postInvalidateOnAnimation`/`ValueAnimator` frame scheduling, GPU layers, and velocity-sensitive settling. Android decides the actual display refresh cadence; verify 120 Hz on the phone with Developer Options rather than assuming it from a timer.

The recovered Sense material established direct touch-to-animation progress, sine-like depth, and a 400 ms ordinary snap as useful references. The wave itself is a new implementation, as required, and not an exact recovered HTC effect.

## Build

Prerequisites are JDK 17 and Android SDK platform 36.

```sh
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs the same build on pushes, pull requests, and manual dispatches, then uploads `fluid-home-debug-apk`.

## Install from a phone

1. Merge the pull request on GitHub.
2. Open the repository's **Actions** tab, open the latest **Android APK** run, and download **fluid-home-debug-apk**.
3. Unzip the downloaded artifact, tap `app-debug.apk`, and allow “Install unknown apps” for the browser/files app if prompted.
4. Press Home, or open **Settings → Apps → Choose default apps → Home app**, and select **Fluid Home**.
5. Long-press empty home space to add a widget. Long-press a widget to resize/move/remove it; long-press an icon to rearrange it.

## Device verification still required

The project targets API 36 and adapts to runtime metrics, but physical behavior cannot be certified without the SM-S928U. On the phone, check: 120 Hz with “Show refresh rate” enabled; cutout/status/navigation spacing in both gesture and button navigation; widget bind/configuration for several vendors; launch behavior under work profiles; gesture cancellation; and layout restore after reboot, density/font scaling changes, and rotation.
