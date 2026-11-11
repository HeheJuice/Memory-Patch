**English** · [繁體中文](README.md)

---

# Memory-Patch

- Fixes the crash when opening `Memory Double Tap` in Samsung Memory (`com.samsung.android.app.memory`)
- This app is originally designed for One UI 7+, but under specific conditions, it triggers a call to an API newly added in One UI 9, causing the crash.
- The module provides system version spoofing and crash interception.

## ⚠️ How it works (Hooks)
**This module works entirely via runtime Hooks, requiring NO APK edit.** 
Requires **LSPosed 2.0.0+**.

## Problem

When double tapping `Power Button` when `Memory` is selected, the `Memory` triggers a call to the private static method `VibrationEffect.semCreateWaveform(IIF)` inside `ScreenCaptureViewerActivity.b0()`. However, this method is actually an API **newly added in One UI 9**.

On systems that do not include this new API (such as One UI 7~8), this causes the app to throw a `NoSuchMethodError` and crash immediately:

```java
java.lang.NoSuchMethodError: No static method semCreateWaveform(IIF)Landroid/os/VibrationEffect; in class Landroid/os/VibrationEffect;
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.b0(...)
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.i0(...)
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.onCreate(...)
```

## Solution

Three layers work together:

1. **System Spoofing** — Spoofs the One UI version to `9.5` (90500) and `SEM_INT`. It intercepts `File` and `FileInputStream` to simulate the existence of `/system/etc/permissions/com.samsung.android.oneui.version.xml`, preventing the app from refusing to run due to version checks.

2. **Crash Interception** — Hooks the `ScreenCaptureViewerActivity.b0()` method. Since `b0` returns `void`, we intercept it before execution and bypass the original method (by setting `param.result = null`), completely avoiding the `NoSuchMethodError` crash caused by calling a non-existent One UI 9 API.

```kotlin
// Core Hook logic illustration
val hook = object : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) {
        val context = param.thisObject as? android.content.Context
        context?.getSystemService(android.content.Context.VIBRATOR_SERVICE)?.let { vibrator ->
            (vibrator as? android.os.Vibrator)?.vibrate(buzzEffect)
        }
        param.result = null // Bypass the crashing original method
    }
}
```

## Scope

- **Package:** `com.samsung.android.app.memory`
- **Activation:** After checking the scope and enabling it in `LSPosed`, you **MUST force-stop** the target app. You can use `am force-stop com.samsung.android.app.memory` in `adb shell` for a quick restart, or simply reboot your device.