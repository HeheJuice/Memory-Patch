**繁體中文** · [English](README-EN.md)

---

# Memory-Patch

- 修復三星 Memory (`com.samsung.android.app.memory`) 中打開 `Memory Double Tap` 時的崩潰問題。
- 本應用原本是針對 One UI 7+ 設計的，但在特定條件下觸發了對 One UI 9 新增 API 的調用，導致崩潰。
- 模塊提供系統版本偽裝和崩潰攔截。

## ⚠️ 運作方式 (Hooks)
**本模塊完全透過運行時 Hook 運作，無需修改或重新編譯任何 APK 檔案。** 
需要安裝 **LSPosed 2.0.0+** 框架。

## 問題 (Problem)

當選中 `Memory` 時雙擊 `電源鍵`，`Memory應用`會在 `ScreenCaptureViewerActivity.b0()` 中觸發對私有靜態方法 `VibrationEffect.semCreateWaveform(IIF)` 的調用。然而，該方法實際上是 **One UI 9 才新增**的 API。

在未包含此新增 API 的系統（如 One UI 7~8）上，這會導致應用拋出 `NoSuchMethodError` 並直接閃退：

```java
java.lang.NoSuchMethodError: No static method semCreateWaveform(IIF)Landroid/os/VibrationEffect; in class Landroid/os/VibrationEffect;
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.b0(...)
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.i0(...)
	at com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity.onCreate(...)
```

## 修復方式 (Solution)

三個 Layer 協同運作：

1. **系統偽裝** — 偽裝 One UI 版本為 `9.5` (90500) 及 `SEM_INT`，並攔截 `File` 與 `FileInputStream`，模擬 `/system/etc/permissions/com.samsung.android.oneui.version.xml` 的存在，防止應用因版本檢查而拒絕運行。

2. **崩潰攔截** — Hook `ScreenCaptureViewerActivity.b0()` 方法。由於 `b0` 是 `void` 返回類型，我們在執行前攔截並直接跳過原方法（設為 `param.result = null`），徹底避免因調用不存在的 One UI 9 API 而導致的 `NoSuchMethodError` 崩潰。

```kotlin
// 核心 Hook 邏輯示意
val hook = object : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) {
        val context = param.thisObject as? android.content.Context
        context?.getSystemService(android.content.Context.VIBRATOR_SERVICE)?.let { vibrator ->
            (vibrator as? android.os.Vibrator)?.vibrate(buzzEffect)
        }
        param.result = null // 跳過崩潰的原方法
    }
}
```

## 範圍 (Scope)

- **套件：** `com.samsung.android.app.memory`
- **正式啟用：** 在 `LSPosed` 中勾選作用域並啟用後**必須強制停止**目標應用，可在 `adb shell` 中使用 `am force-stop com.samsung.android.app.memory` 快速重啟，或直接重啟手機。