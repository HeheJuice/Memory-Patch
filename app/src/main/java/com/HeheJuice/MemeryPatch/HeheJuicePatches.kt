package com.HeheJuice.MemeryPatch

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class HeheJuicePatches : IXposedHookLoadPackage {

    private val customXmlContent = """
        <?xml version="1.0" encoding="utf-8"?>
        <permissions>
         <feature name="com.samsung.android.oneui.version.10000" />
         <feature name="com.samsung.android.oneui.version.10100" />
         <feature name="com.samsung.android.oneui.version.10200" />
         <feature name="com.samsung.android.oneui.version.10500" />
         <feature name="com.samsung.android.oneui.version.20000" />
         <feature name="com.samsung.android.oneui.version.20100" />
         <feature name="com.samsung.android.oneui.version.20500" />
         <feature name="com.samsung.android.oneui.version.30000" />
         <feature name="com.samsung.android.oneui.version.30100" />
         <feature name="com.samsung.android.oneui.version.30101" />
         <feature name="com.samsung.android.oneui.version.40000" />
         <feature name="com.samsung.android.oneui.version.40100" />
         <feature name="com.samsung.android.oneui.version.40101" />
         <feature name="com.samsung.android.oneui.version.50000" />
         <feature name="com.samsung.android.oneui.version.50100" />
         <feature name="com.samsung.android.oneui.version.50101" />
         <feature name="com.samsung.android.oneui.version.60000" />
         <feature name="com.samsung.android.oneui.version.60100" />
         <feature name="com.samsung.android.oneui.version.60101" />
         <feature name="com.samsung.android.oneui.version.70000" />
         <feature name="com.samsung.android.oneui.version.80000" />
         <feature name="com.samsung.android.oneui.version.90000" />
         <feature name="com.samsung.android.oneui.version.90500" />
        </permissions>
    """.trimIndent()

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 診斷第一步: 無條件記錄模塊是否被注入到當前進程
        XposedBridge.log("HeheJuice: ===== LOADED into ${lpparam.packageName} =====")

        if (lpparam.packageName == null) return

        val targetPath = "/system/etc/permissions/com.samsung.android.oneui.version.xml"
        val xmlBytes = customXmlContent.toByteArray(Charsets.UTF_8)

        // =========================================================
        // LAYER 1: SYSTEM PROPERTIES & INT OVERRIDES (All Apps)
        // =========================================================
        try {
            val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            val propHookString = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = "90500"
                    if (key == "ro.build.version.sep") param.result = "180500"
                }
            }
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, propHookString)
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "get", String::class.java, String::class.java, propHookString)

            val propHookInt = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = 90500
                    if (key == "ro.build.version.sep") param.result = 180500
                }
            }
            XposedHelpers.findAndHookMethod(systemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType, propHookInt)
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L1A failed: $t")
        }

        // 1B. Spoof PackageManager.hasSystemFeature
        try {
            val featureHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val featureName = param.args[0] as? String ?: return
                    if (featureName.startsWith("com.samsung.android.oneui.version")) {
                        param.result = true
                    }
                }
            }
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, featureHook)
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, Int::class.javaPrimitiveType, featureHook)
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L1B failed: $t")
        }

        // 1C. Spoof Samsung's Hidden Static Build Variables
        try {
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_PLATFORM_INT", 180500)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_INT", 180500)
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L1C failed: $t")
        }

        // 1D. Spoof Samsung's Proprietary SemSystemProperties Wrapper
        try {
            val semSystemPropertiesClass = XposedHelpers.findClass("android.os.SemSystemProperties", lpparam.classLoader)

            val semPropHookString = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = "90500"
                    if (key == "ro.build.version.sep") param.result = "180500"
                }
            }
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, semPropHookString)
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "get", String::class.java, String::class.java, semPropHookString)

            val semPropHookInt = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as? String ?: return
                    if (key == "ro.build.version.oneui") param.result = 90500
                    if (key == "ro.build.version.sep") param.result = 180500
                }
            }
            XposedHelpers.findAndHookMethod(semSystemPropertiesClass, "getInt", String::class.java, Int::class.javaPrimitiveType, semPropHookInt)
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L1D failed: $t")
        }

        // =========================================================
        // LAYER 2: UNIVERSAL VIRTUAL FILE SIMULATION (All Apps)
        // =========================================================

        // 2A. Mock existence globally
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "exists", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = true
                    }
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L2A failed: $t")
        }

        // 2B. Mock file length metrics
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "length", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    if (file.absolutePath == targetPath) {
                        param.result = xmlBytes.size.toLong()
                    }
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L2B failed: $t")
        }

        // 2C. Intercept FileInputStream constructors safely via /dev/null redirect
        try {
            val fileStreamHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val arg = param.args[0]
                    val path = if (arg is File) arg.absolutePath else arg as? String

                    if (path == targetPath) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "isOneUISpoofStream", true)
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "spoofStream", ByteArrayInputStream(xmlBytes))
                        if (arg is File) {
                            param.args[0] = File("/dev/null")
                        } else {
                            param.args[0] = "/dev/null"
                        }
                    }
                }
            }
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, File::class.java, fileStreamHook)
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, String::class.java, fileStreamHook)
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L2C failed: $t")
        }

        // 2D. Handle byte-array parsing transactions directly
        try {
            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        param.result = bis.read()
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", ByteArray::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        val b = param.args[0] as ByteArray
                        param.result = bis.read(b)
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        val b = param.args[0] as ByteArray
                        val off = param.args[1] as Int
                        val len = param.args[2] as Int
                        param.result = bis.read(b, off, len)
                    }
                }
            })

            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "available", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        param.result = bis.available()
                    }
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L2D failed: $t")
        }

        // =========================================================
        // LAYER 3: com.samsung.android.app.memory 振動兼容修復
        // 崩潰: NoSuchMethodError VibrationEffect.semCreateWaveform(IIF)
        // 方案: One UI 8 系統缺此方法 -> Hook 攔截並主動觸發震動
        // =========================================================
        if (lpparam.packageName == "com.samsung.android.app.memory") {
            installMemoryVibrationFix(lpparam)
        }
    }

    // -----------------------------------------------------------------
    // LAYER 3 實現
    // -----------------------------------------------------------------
    private fun installMemoryVibrationFix(lpparam: LoadPackageParam) {
        val tag = "HeheJuice"
        XposedBridge.log("$tag: ====== LAYER3 START for ${lpparam.packageName} ======")

        // 1) 檢查系統是否真的缺少 semCreateWaveform(IIF)
        if (systemHasSemCreateWaveform()) {
            XposedBridge.log("$tag: API exists, no patch needed.")
            return
        }

        // 2) 加載目標類(不初始化)
        val cls: Class<*>
        try {
            cls = Class.forName(
                "com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity",
                false,
                lpparam.classLoader
            )
            XposedBridge.log("$tag: target class loaded -> ${cls.name}")
        } catch (t: Throwable) {
            XposedBridge.log("$tag: !! Class.forName FAILED: $t")
            return
        }

        // 3) 同時尋找 b0 (舊版) 和 i0 (新版 v1.0.00.48)
        val targetMethodNames = listOf("b0", "i0")
        var totalHooked = 0

        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val m = param.method as? java.lang.reflect.Method
                XposedBridge.log("$tag: >>> ${m?.name} HIT -> ${m?.toGenericString() ?: param.method.name}")
                
                // a. 主動觸發震動服務
                val context = param.thisObject as? Context
                context?.getSystemService(Context.VIBRATOR_SERVICE)?.let { vibrator ->
                    if (vibrator is Vibrator) {
                        val effect = createBuzzVibrationEffect()
                        if (effect != null) {
                            vibrator.vibrate(effect)
                            XposedBridge.log("$tag: ${m?.name} -> manually triggered buzz effect")
                        } else {
                            vibrator.vibrate(80L)
                            XposedBridge.log("$tag: ${m?.name} -> manually triggered fallback vibration")
                        }
                    }
                }

                // b. 阻斷原方法，防止 NoSuchMethodError
                // b0 和 i0 都是 void 方法，設置 null 即可跳過原方法體
                param.result = null 
            }
        }

        for (methodName in targetMethodNames) {
            val methods = cls.declaredMethods.filter { it.name == methodName }
            if (methods.isNotEmpty()) {
                for (m in methods) {
                    try {
                        XposedBridge.hookMethod(m, hook)
                        XposedBridge.log("$tag: hooked -> ${m.toGenericString()}")
                        totalHooked++
                    } catch (t: Throwable) {
                        XposedBridge.log("$tag: hookMethod FAILED for $m: $t")
                    }
                }
            }
        }
        
        XposedBridge.log("$tag: ====== LAYER3 DONE, total hooked=$totalHooked ======")
    }

    /** 檢查系統是否真的存在 semCreateWaveform(IIF) */
    private fun systemHasSemCreateWaveform(): Boolean {
        return try {
            VibrationEffect::class.java.getDeclaredMethod(
                "semCreateWaveform",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
            true
        } catch (_: NoSuchMethodException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 構造一段"滋滋"蜂鳴式振動效果。
     * 用多段短脈衝 + 短間隔交替，產生類似馬達/蜂鳴的連續感。
     * timings 與 amplitudes 一一對應：
     *   amplitude = 0 表示該段靜止，> 0 表示該段振動。
     */
    private fun createBuzzVibrationEffect(): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        // 主方案：蜂鳴波形 (5 段 18ms 振動 + 12ms 間隔 ≈ 138ms)
        try {
            val timings = longArrayOf(0L, 18, 12, 18, 12, 18, 12, 18, 12, 18)
            val amplitudes = intArrayOf(0, 220, 0, 220, 0, 220, 0, 220, 0, 220)
            return VibrationEffect.createWaveform(timings, amplitudes, -1)
        } catch (_: Throwable) {
            // 回退：一次較明顯的單脈衝
            try {
                return VibrationEffect.createOneShot(80L, 200)
            } catch (_: Throwable) {
                // 最後回退：輕微點擊
                return try {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}