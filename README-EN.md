**English** · [繁體中文](README.md)

---

# Memory-Patch

- Fixes the crash when opening `Memory Double Tap` in Samsung Memory (`com.samsung.android.app.memory`).
- This app is originally designed for One UI 7+, but under specific conditions, it triggers a call to an API newly added in One UI 9, causing the crash.
- The module provides system version spoofing, **DexKit anti-obfuscation dynamic crash interception**, and **haptic feedback restoration**.

## ⚠️ How it works (Hooks)
- **This module works entirely via runtime Hooks, requiring NO APK edit.** 
- Requires **LSPosed 2.0.0+**.

## Problem

When double tapping `Power Button` while `Memory` is selected, the `Memory` app triggers a call to the private static method `VibrationEffect.semCreateWaveform(IIF)`. However, this method is actually an API **newly added in One UI 9**.

On systems that do not include this new API (such as One UI 7~8), this causes the app to throw a `NoSuchMethodError` and crash immediately. **Furthermore, Samsung changes the target method name via R8 obfuscation in subsequent app updates (e.g., from `b0` to `i0`), breaking traditional hardcoded Hooks.**

```java
java.lang.NoSuchMethodError: No static method semCreateWaveform(IIF)Landroid/os/VibrationEffect; in class Landroid/os/VibrationEffect;
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.b0(...) // Old version
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.i0(...) // New version v1.0.00.48
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.onCreate(...)
```

## Solution

Three layers work together:

1. **System Spoofing** — Spoofs the One UI version to `9.5` (90500) and `SEM_INT`. It intercepts `File` and `FileInputStream` to simulate the existence of `/system/etc/permissions/com.samsung.android.oneui.version.xml`, preventing the app from refusing to run due to version checks.

2. **DexKit Dynamic Crash Interception** — To withstand Samsung's obfuscation updates, the module uses DexKit to dynamically scan the APK at runtime. It accurately locates the crash-triggering method (whether named `b0` or `i0`) by matching signatures (return type `void`, param count `3`). The resolution result is cached based on the APK's last modified time for instant loading on subsequent launches. It intercepts and bypasses the original method (by setting `param.result = null`), completely avoiding the `NoSuchMethodError`.

3. **Haptic Feedback Restoration** — Since the original method is bypassed, haptic feedback is lost. The Hook actively acquires the system `Vibrator` service and injects a "buzzing" waveform constructed via the public `VibrationEffect.createWaveform` API to restore the original haptic feedback.

```kotlin
// Core Hook logic illustration
val hook = object : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) {
        // 1. Manually trigger vibration (restore feedback)
        val context = param.thisObject as? android.content.Context
        context?.getSystemService(android.content.Context.VIBRATOR_SERVICE)?.let { vibrator ->
            (vibrator as? android.os.Vibrator)?.vibrate(buzzEffect)
        }
        // 2. Bypass the crashing original method
        param.result = null 
    }
}
```

## Scope

- **Package:** `com.samsung.android.app.memory`
- **Activation:** After checking the scope and enabling it in `LSPosed`, you **MUST force-stop** the target app. You can use `am force-stop com.samsung.android.app.memory` in `adb shell` for a quick restart, or simply reboot your device.