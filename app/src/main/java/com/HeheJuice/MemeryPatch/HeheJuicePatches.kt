package com.HeheJuice.MemeryPatch

import android.os.VibrationEffect
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class HeheJuiceSpoof : IXposedHookLoadPackage {

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
        } catch (t: Throwable) {}

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
        } catch (t: Throwable) {}

        // 1C. Spoof Samsung's Hidden Static Build Variables
        try {
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_PLATFORM_INT", 180500)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_INT", 180500)
        } catch (t: Throwable) {}

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
        } catch (t: Throwable) {}

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
        } catch (t: Throwable) {}

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
        } catch (t: Throwable) {}

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
        } catch (t: Throwable) {}

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
        } catch (t: Throwable) {}

        // =========================================================
        // LAYER 3: com.samsung.android.app.memory 振动兼容修复
        // 崩溃: NoSuchMethodError VibrationEffect.semCreateWaveform(IIF)
        // 方案: 系统缺该方法时, hookAllMethods("b0") 全部重载,
        //       在回调里按 returnType 运行时分流跳过方法体
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

        // 0) 先确认模块确实加载进了目标进程
        XposedBridge.log("$tag: handleLoadPackage for ${lpparam.packageName}, starting vibration fix")

        try {
            // 1) 探测系统是否真的缺 (IIF) 重载; 不缺就不干预
            if (systemHasSemCreateWaveform()) {
                XposedBridge.log("$tag: semCreateWaveform(IIF) exists, no patch needed")
                return
            }
            XposedBridge.log("$tag: semCreateWaveform(IIF) MISSING -> installing b0 hook")

            val cls = XposedHelpers.findClass(
                "com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity",
                lpparam.classLoader
            )
            XposedBridge.log("$tag: target class loaded: ${cls.name}")

            var hooked = 0
            val hook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val ret = param.method.returnType
                    XposedBridge.log(
                        "$tag: b0 intercepted -> ${param.method.toGenericString()}, args=${param.args?.joinToString()}"
                    )
                    param.result = safeValueFor(ret)
                }
            }

            // 2) 无条件 hook 所有名为 b0 的方法(不论参数/返回类型)
            XposedBridge.hookAllMethods(cls, "b0", hook)
            hooked = cls.declaredMethods.count { it.name == "b0" }
            XposedBridge.log("$tag: hooked b0 overloads = $hooked")

            if (hooked == 0) {
                // 兜底: b0 若声明在父类, 往上找一层
                var p = cls.superclass
                while (p != null && p != Any::class.java) {
                    if (p.declaredMethods.any { it.name == "b0" }) {
                        XposedBridge.hookAllMethods(p, "b0", hook)
                        XposedBridge.log("$tag: hooked b0 on superclass ${p.name}")
                        hooked = 1
                        break
                    }
                    p = p.superclass
                }
            }
            if (hooked == 0) {
                XposedBridge.log("$tag: WARN no b0 method found anywhere in hierarchy")
            }
        } catch (t: Throwable) {
            // 关键: 不再吞异常, 完整打到日志, 方便定位
            XposedBridge.log("$tag: installMemoryVibrationFix FAILED: $t")
            XposedBridge.log(t)
        }
    }

    /** 系统是否真的存在 semCreateWaveform(IIF) */
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
     * 根据目标方法的返回类型给出安全返回值。
     * - VibrationEffect 或子类 -> safeClickEffect()
     * - void                  -> null
     * - 原始类型              -> 对应零值 (Xposed 对原始类型不接受 null)
     * - 其他引用类型          -> null
     */
    private fun safeValueFor(ret: Class<*>): Any? {
        return when {
            ret == java.lang.Void.TYPE -> null
            VibrationEffect::class.java.isAssignableFrom(ret) -> safeClickEffect()
            ret.isPrimitive -> when (ret) {
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Boolean.TYPE -> false
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Character.TYPE -> '\u0000'
                else -> null
            }
            else -> null
        }
    }

    /** 公开 API 构造的降级效果: 优先 CLICK 预定义, 否则短促 oneShot */
    private fun safeClickEffect(): VibrationEffect? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return null
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