package com.HeheJuice.MemeryPatch

import android.os.Build
import android.os.VibrationEffect
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
        // 诊断第一步: 无条件记录模块是否被注入到当前进程
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
        // LAYER 3: com.samsung.android.app.memory 振动兼容修复
        // 崩溃: NoSuchMethodError VibrationEffect.semCreateWaveform(IIF)
        // 方案: One UI 8 系统缺此方法 -> Hook ScreenCaptureViewerActivity.b0,
        //       用公开 API 构造等效振动效果返回
        // =========================================================
        if (lpparam.packageName == "com.samsung.android.app.memory") {
            installMemoryVibrationFix(lpparam)
        }
    }

    // -----------------------------------------------------------------
    // LAYER 3 实现
    // -----------------------------------------------------------------
    private fun installMemoryVibrationFix(lpparam: LoadPackageParam) {
        val tag = "HeheJuice"
        XposedBridge.log("$tag: ====== LAYER3 START for ${lpparam.packageName} ======")

        // 1) 检查系统是否真的缺少 semCreateWaveform(IIF)
        val hasApi = systemHasSemCreateWaveform()
        XposedBridge.log("$tag: systemHasSemCreateWaveform(IIF) = $hasApi")
        if (hasApi) {
            XposedBridge.log("$tag: API exists, no patch needed.")
            return
        }

        // 2) 加载目标类(不初始化)
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
            XposedBridge.log(t)
            return
        }

        // 3) 枚举 b0
        val b0Methods = cls.declaredMethods.filter { it.name == "b0" }
        XposedBridge.log("$tag: declared b0 count = ${b0Methods.size}")
        if (b0Methods.isEmpty()) {
            XposedBridge.log("$tag: !! b0 NOT FOUND in ${cls.name}. Dump all methods:")
            cls.declaredMethods.forEach { XposedBridge.log("$tag:   ${it.toGenericString()}") }
            return
        }
        b0Methods.forEach { XposedBridge.log("$tag: b0 candidate -> ${it.toGenericString()}") }

        // 4) 逐个 Hook b0
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val m = param.method as? java.lang.reflect.Method
                XposedBridge.log("$tag: >>> b0 HIT -> ${m?.toGenericString() ?: param.method.name}")
                val replacement = createSafeVibrationEffect()
                param.result = replacement
                XposedBridge.log("$tag: b0 -> replaced with $replacement")
            }
        }

        var ok = 0
        for (m in b0Methods) {
            try {
                XposedBridge.hookMethod(m, hook)
                XposedBridge.log("$tag: hooked -> ${m.toGenericString()}")
                ok++
            } catch (t: Throwable) {
                XposedBridge.log("$tag: hookMethod FAILED for $m: $t")
            }
        }
        XposedBridge.log("$tag: ====== LAYER3 DONE, hooked=$ok ======")
    }

    /** 检查系统是否真的存在 semCreateWaveform(IIF) */
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

    /** 用公开 API 构造安全的振动效果: 优先 CLICK, 否则短促 oneShot */
    private fun createSafeVibrationEffect(): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return try {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } catch (_: Throwable) {
            try {
                VibrationEffect.createOneShot(50L, 128)
            } catch (_: Throwable) {
                null
            }
        }
    }
}