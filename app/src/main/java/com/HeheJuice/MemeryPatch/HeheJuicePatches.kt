package com.HeheJuice.MemeryPatch

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.luckypray.dexkit.DexKitBridge
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

        // =========================================================
        // LAYER 1: SYSTEM PROPERTIES & INT OVERRIDES
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
        } catch (t: Throwable) { XposedBridge.log("HeheJuice: L1A failed: $t") }

        try {
            val featureHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val featureName = param.args[0] as? String ?: return
                    if (featureName.startsWith("com.samsung.android.oneui.version")) param.result = true
                }
            }
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, featureHook)
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "hasSystemFeature", String::class.java, Int::class.javaPrimitiveType, featureHook)
        } catch (t: Throwable) { XposedBridge.log("HeheJuice: L1B failed: $t") }

        try {
            val buildVersionClass = XposedHelpers.findClass("android.os.Build\$VERSION", lpparam.classLoader)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_PLATFORM_INT", 180500)
            XposedHelpers.setStaticIntField(buildVersionClass, "SEM_INT", 180500)
        } catch (t: Throwable) { XposedBridge.log("HeheJuice: L1C failed: $t") }

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
        } catch (t: Throwable) { XposedBridge.log("HeheJuice: L1D failed: $t") }

        // =========================================================
        // LAYER 2: UNIVERSAL VIRTUAL FILE SIMULATION
        // =========================================================
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
        } catch (t: Throwable) { XposedBridge.log("HeheJuice: L2 failed: $t") }

        // =========================================================
        // LAYER 3: com.samsung.android.app.memory 振動兼容修復
        // =========================================================
        if (lpparam.packageName == "com.samsung.android.app.memory") {
            installMemoryVibrationFix(lpparam)
        }
    }

    private fun installMemoryVibrationFix(lpparam: LoadPackageParam) {
        val tag = "HeheJuice"
        XposedBridge.log("$tag: ====== LAYER3 START for ${lpparam.packageName} ======")

        if (systemHasSemCreateWaveform()) {
            XposedBridge.log("$tag: API exists, no patch needed.")
            return
        }

        val targetClass = "com.samsung.android.app.memory.feature.capture.ScreenCaptureViewerActivity"
        
        // 修复: 使用 versionCode 消除 Unresolved reference 错误
        @Suppress("DEPRECATION")
        val versionCode = lpparam.appInfo?.versionCode?.toLong() ?: 0L
        
        val targetMethod = findTargetMethod(lpparam, targetClass, versionCode)

        if (targetMethod != null) {
            XposedBridge.log("$tag: hooking method -> ${targetMethod.toGenericString()}")
            XposedBridge.hookMethod(targetMethod, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // 1. 手動觸發震動 (因為原方法被跳過)
                    val context = param.thisObject as? Context
                    context?.getSystemService(Context.VIBRATOR_SERVICE)?.let { vibrator ->
                        if (vibrator is Vibrator) {
                            vibrator.vibrate(createBuzzVibrationEffect())
                            XposedBridge.log("$tag: ${targetMethod.name} -> manually triggered vibration")
                        }
                    }
                    // 2. 阻斷原方法，防止 NoSuchMethodError
                    param.result = null
                }
            })
            XposedBridge.log("$tag: ====== LAYER3 DONE ======")
        } else {
            XposedBridge.log("$tag: !! Failed to locate target method. Patch not applied.")
        }
    }

    /**
     * 尋找目標方法 (包含緩存機制與動態降級)
     */
    private fun findTargetMethod(lpparam: LoadPackageParam, className: String, versionCode: Long): Method? {
        val tag = "HeheJuice"
        val cacheKey = "ScreenCaptureViewerActivity_Method_v$versionCode"

        val prefs = getModulePrefs(lpparam)
        val cachedSignature = prefs?.getString(cacheKey, null)

        if (cachedSignature != null) {
            try {
                val parts = cachedSignature.split("#")
                if (parts.size == 3) {
                    val cls = Class.forName(parts[0], false, lpparam.classLoader)
                    val paramTypes = parseParamTypes(parts[2], lpparam.classLoader)
                    val method = cls.getDeclaredMethod(parts[1], *paramTypes)
                    XposedBridge.log("$tag: Loaded method from cache -> ${method.toGenericString()}")
                    return method
                }
            } catch (e: Throwable) {
                XposedBridge.log("$tag: Cache invalid, re-scanning... ($e)")
            }
        }

        var foundMethod: Method? = null
        val latch = CountDownLatch(1)
        val apkPath = lpparam.appInfo.sourceDir

        Thread {
            try {
                System.loadLibrary("dexkit")
                DexKitBridge.create(apkPath).use { bridge ->
                    val results = bridge.findMethod {
                        matcher {
                            declaredClass = className
                            returnType = "void"
                            paramCount = 3 // 根據崩潰日誌，大概率是 3 個參數
                        }
                    }

                    val methodData = results.firstOrNull()
                    if (methodData != null) {
                        foundMethod = methodData.getMethodInstance(lpparam.classLoader)
                        foundMethod?.let { m ->
                            val signature = "${m.declaringClass.name}#${m.name}#${m.parameterTypes.joinToString(",") { it.name }}"
                            prefs?.edit()?.putString(cacheKey, signature)?.apply()
                        }
                    }
                }
            } catch (t: Throwable) {
                XposedBridge.log("$tag: DexKit scan failed: $t")
            } finally {
                latch.countDown()
            }
        }.start()

        try {
            latch.await(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            XposedBridge.log("$tag: DexKit wait interrupted")
        }

        if (foundMethod == null) {
            XposedBridge.log("$tag: DexKit failed, falling back to hardcoded names...")
            val fallbackNames = listOf("b0", "i0")
            for (name in fallbackNames) {
                try {
                    val cls = Class.forName(className, false, lpparam.classLoader)
                    foundMethod = cls.declaredMethods.firstOrNull { it.name == name && it.returnType == Void.TYPE }
                    if (foundMethod != null) {
                        XposedBridge.log("$tag: Fallback found method -> ${foundMethod.toGenericString()}")
                        break
                    }
                } catch (_: Throwable) {}
            }
        }

        return foundMethod
    }

    private fun getModulePrefs(lpparam: LoadPackageParam): SharedPreferences? {
        return try {
            val context = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader),
                "currentApplication"
            ) as? Context
            context?.getSharedPreferences("HeheJuice_Memory_Patch", Context.MODE_PRIVATE)
        } catch (t: Throwable) {
            null
        }
    }

    private fun parseParamTypes(paramStr: String, classLoader: ClassLoader): Array<Class<*>> {
        if (paramStr.isEmpty()) return emptyArray()
        return paramStr.split(",").map { type ->
            when (type) {
                "int" -> Int::class.javaPrimitiveType!!
                "float" -> Float::class.javaPrimitiveType!!
                "boolean" -> Boolean::class.javaPrimitiveType!!
                "long" -> Long::class.javaPrimitiveType!!
                else -> Class.forName(type, false, classLoader)
            }
        }.toTypedArray()
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
        } catch (_: Throwable) { false }
    }

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
                try { VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK) } catch (_: Throwable) { null }
            }
        }
    }
}