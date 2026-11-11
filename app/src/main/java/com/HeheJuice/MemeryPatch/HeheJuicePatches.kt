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
import java.util.ArrayList

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
        XposedBridge.log("HeheJuice: ===== LOADED into ${lpparam.packageName} =====")

        if (lpparam.packageName == null) return

        val targetPath = "/system/etc/permissions/com.samsung.android.oneui.version.xml"
        val xmlBytes = customXmlContent.toByteArray(Charsets.UTF_8)

        // ========================= LAYER 1 =========================
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

        try {
            val featureHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val featureName = param.args[0] as? String ?: return
                    if (featureName.startsWith("com.samsung.android.oneui.version")) param.result = true
                }
            }
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, featureHook)
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, Int::class.javaPrimitiveType, featureHook)
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L1B failed: $t")
        }

        try {
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_PLATFORM_INT", 180500)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_INT", 180500)
        } catch (t: Throwable) {
            XposedBridge.log("HeheJuice: L1C failed: $t")
        }

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

        // ========================= LAYER 2 =========================
        try {
            XposedHelpers.findAndHookMethod(File::class.java, "exists", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if ((param.thisObject as File).absolutePath == targetPath) param.result = true
                }
            })
            XposedHelpers.findAndHookMethod(File::class.java, "length", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if ((param.thisObject as File).absolutePath == targetPath) param.result = xmlBytes.size.toLong()
                }
            })
            val fileStreamHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val arg = param.args[0]
                    val path = if (arg is File) arg.absolutePath else arg as? String
                    if (path == targetPath) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "isOneUISpoofStream", true)
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "spoofStream", ByteArrayInputStream(xmlBytes))
                        param.args[0] = if (arg is File) File("/dev/null") else "/dev/null"
                    }
                }
            }
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, File::class.java, fileStreamHook)
            XposedHelpers.findAndHookConstructor(FileInputStream::class.java, String::class.java, fileStreamHook)

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
                        param.result = bis.read(param.args[0] as ByteArray)
                    }
                }
            })
            XposedHelpers.findAndHookMethod(FileInputStream::class.java, "read", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (XposedHelpers.getAdditionalInstanceField(param.thisObject, "isOneUISpoofStream") == true) {
                        val bis = XposedHelpers.getAdditionalInstanceField(param.thisObject, "spoofStream") as ByteArrayInputStream
                        param.result = bis.read(param.args[0] as ByteArray, param.args[1] as Int, param.args[2] as Int)
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
            XposedBridge.log("HeheJuice: L2 failed: $t")
        }

        // ========================= LAYER 3 =========================
        if (lpparam.packageName == "com.samsung.android.app.memory") {
            installMemoryVibrationFix(lpparam)
        }
    }

    private fun installMemoryVibrationFix(lpparam: LoadPackageParam) {
        val tag = "HeheJuice"
        XposedBridge.log("$tag: ====== LAYER3 START for ${lpparam.packageName} ======")

        val hasApi = systemHasSemCreateWaveform()
        XposedBridge.log("$tag: systemHasSemCreateWaveform(IIF) = $hasApi")
        if (hasApi) {
            XposedBridge.log("$tag: API exists, no patch needed.")
            return
        }

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

        val b0Methods = cls.declaredMethods.filter { it.name == "b0" }
        XposedBridge.log("$tag: declared b0 count = ${b0Methods.size}")
        if (b0Methods.isEmpty()) {
            XposedBridge.log("$tag: !! b0 NOT FOUND. Dump:")
            cls.declaredMethods.forEach { XposedBridge.log("$tag:   ${it.toGenericString()}") }
            return
        }
        b0Methods.forEach { XposedBridge.log("$tag: b0 candidate -> ${it.toGenericString()}") }

        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val args = param.args ?: emptyArray()
                // b0(IIF) => hapticId, repeatIndex, scale
                val hapticId = args.getOrNull(0) as? Int ?: -1
                val repeatIndex = args.getOrNull(1) as? Int ?: -1
                val scale = args.getOrNull(2) as? Float ?: 1.0f

                XposedBridge.log("$tag: >>> b0 HIT args=(hapticId=$hapticId, repeatIndex=$repeatIndex, scale=$scale)")

                // 1) 首选: 用 UI9 同样的 SemHapticSegment + Composed 完全还原
                var effect = buildSemWaveform(hapticId, repeatIndex, scale)
                if (effect != null) {
                    XposedBridge.log("$tag: b0 -> restored SEM effect (hapticId=$hapticId, scale=$scale)")
                } else {
                    // 2) UI9 失败回退路径: 用 0xc3a4 的通用 haptic
                    effect = buildSemWaveform(0xc3a4, repeatIndex, 1.0f)
                    if (effect != null) {
                        XposedBridge.log("$tag: b0 -> fallback SEM effect (0xc3a4)")
                    } else {
                        // 3) 最后兜底: 蜂鸣波形
                        effect = createBuzzVibrationEffect()
                        XposedBridge.log("$tag: b0 -> fallback buzz waveform")
                    }
                }
                param.result = effect
            }
        }

        var ok = 0
        for (m in b0Methods) {
            try {
                XposedBridge.hookMethod(m, hook)
                ok++
            } catch (t: Throwable) {
                XposedBridge.log("$tag: hookMethod FAILED for $m: $t")
            }
        }
        XposedBridge.log("$tag: ====== LAYER3 DONE, hooked=$ok ======")
    }

    private fun systemHasSemCreateWaveform(): Boolean {
        return try {
            VibrationEffect::class.java.getDeclaredMethod(
                "semCreateWaveform",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            )
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 按 UI9 semCreateHaptic(IIF) 的逻辑, 用反射重建:
     *   SemHapticSegment(hapticId, scale) -> list -> Composed(list, repeatIndex) -> validate
     * 任何一步失败返回 null, 交给上层回退。
     */
    private fun buildSemWaveform(hapticId: Int, repeatIndex: Int, scale: Float): VibrationEffect? {
        val tag = "HeheJuice"
        return try {
            val cl = VibrationEffect::class.java.classLoader

            // 1) SemHapticSegment
            val segmentCls = Class.forName("android.os.vibrator.SemHapticSegment", false, cl)

            // 夹紧 scale 到 [0,1], 与 UI9 行为一致
            val safeScale = when {
                scale.isNaN() -> 1.0f
                scale < 0f -> 0f
                scale > 1f -> 1.0f
                else -> scale
            }

            // 优先 (I, F) 构造; 不存在则退化 (I)
            val segment = try {
                val ctor = segmentCls.getDeclaredConstructor(Int::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                ctor.isAccessible = true
                ctor.newInstance(hapticId, safeScale)
            } catch (_: NoSuchMethodException) {
                val ctor = segmentCls.getDeclaredConstructor(Int::class.javaPrimitiveType)
                ctor.isAccessible = true
                ctor.newInstance(hapticId)
            } catch (_: Throwable) {
                null
            } ?: return null

            // 2) Composed(list, repeatIndex)
            val composedCls = Class.forName("android.os.VibrationEffect\$Composed", false, cl)
            val composedCtor = composedCls.getDeclaredConstructor(java.util.List::class.java, Int::class.javaPrimitiveType)
            composedCtor.isAccessible = true

            val list: MutableList<Any> = ArrayList()
            list.add(segment)
            val effect = composedCtor.newInstance(list, repeatIndex) as? VibrationEffect ?: return null

            // 3) validate()，与 UI9 一致
            try {
                val validate = VibrationEffect::class.java.getDeclaredMethod("validate")
                validate.isAccessible = true
                validate.invoke(effect)
            } catch (_: Throwable) {
                // validate 失败视作无效
                XposedBridge.log("$tag: SEM effect validate failed, discard")
                return null
            }

            effect
        } catch (t: Throwable) {
            XposedBridge.log("$tag: buildSemWaveform failed: $t")
            null
        }
    }

    /**
     * 最后兜底: 蜂鸣波形, 仅在 SemHapticSegment 不可用时使用。
     * 5 x (18ms 振 + 12ms 停), 振幅 220。
     */
    private fun createBuzzVibrationEffect(): VibrationEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return try {
            val timings = longArrayOf(0L, 18, 12, 18, 12, 18, 12, 18, 12, 18)
            val amplitudes = intArrayOf(0, 220, 0, 220, 0, 220, 0, 220, 0, 220)
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } catch (_: Throwable) {
            try {
                VibrationEffect.createOneShot(80L, 200)
            } catch (_: Throwable) {
                try {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}