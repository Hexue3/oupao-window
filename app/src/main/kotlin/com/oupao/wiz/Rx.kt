package com.oupao.wiz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Context 获取 + 广播入口。对应原 smali 的 com.oupao.wiz.Rx。
 *
 * 模块自身进程里拿不到 SystemUI 的 Context，所以这里准备了三条兜底路径：
 *   ctxA —— ActivityThread.currentActivityThread().getSystemContext()
 *   ctxB —— ActivityThread.currentActivityThread().getApplication()
 *   ctxC —— AppGlobals.getInitialApplication()
 * 依次尝试，任一成功即可（不同 Android / ColorOS 版本可用性不同）。
 *
 * 同时它也是个 BroadcastReceiver：收到广播时同步配置、初始化侧边栏、分发事件。
 */
class Rx : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            if (Cfg.ctx == null) {
                Cfg.ctx = context.applicationContext ?: context
            }
            // 3145：App 进程接上本地 prefs（与远程 prefs 是同一个文件）
            Cfg.attachLocal(Cfg.ctx)
            FlyS.syncWith(context)
            Sug.init()
            // 2829：把装钩子时因为拿不到 Context 而没写出去的能力位补写一遍
            runCatching { Caps.flushPending(context) }
            if (intent.action == SoftReboot.ACTION) {
                if (android.os.Process.myUid() == 1000) SoftReboot.runFromSystem()
                return@runCatching
            }
            Ev.dispatch(intent.action, intent)
            // 主开关刚被打开 → 立刻补装功能钩子（原来只能重启手机才生效）
            // 调试：CFG 广播带 oupao_reinstall=1 时强制重装功能钩子
            if (intent.getIntExtra("oupao_reinstall", 0) == 1) {
                Boot.forceReinstall()
                return@runCatching
            }
            val mOn = Cfg.masterOn()
            Boot.logRaw("Rx: masterOn=" + mOn + " hookCount=" + XBridge.hookCount +
                " sysDone=" + Hooks.sysDone + " in=" + Boot.featuresInstalledIn)
            if (mOn) {
                Boot.installFeaturesAsync()
            }
        }
    }

    companion object {

        /** 是否已成功拿到可用 Context */
        @JvmField
        @Volatile
        var ok = false

        /**
         * 系统 Context：
         * ActivityThread.currentActivityThread().getSystemContext()
         */
        @JvmStatic
        fun ctxA(): Context? {
            val at = callStatic("android.app.ActivityThread", "currentActivityThread") ?: return null
            val c = call1(at, "getSystemContext")
            return c as? Context
        }

        /**
         * Application Context：
         * ActivityThread.currentActivityThread().getApplication()
         */
        @JvmStatic
        fun ctxB(): Context? {
            val at = callStatic("android.app.ActivityThread", "currentActivityThread") ?: return null
            val c = call1(at, "getApplication")
            return c as? Context
        }

        /** AppGlobals.getInitialApplication() */
        @JvmStatic
        fun ctxC(): Context? {
            val c = callStatic("android.app.AppGlobals", "getInitialApplication")
            return c as? Context
        }

        /**
         * 从任意对象上掏 Context：先试 getContext() 方法，再试 mContext 字段。
         * SystemUI 里很多内部类两种都没有，所以返回 null 是常态。
         */
        @JvmStatic
        fun ctxFrom(target: Any?): Context? {
            if (target == null) return null
            call1(target, "getContext")?.let { if (it is Context) return it }
            Rt.get(target, "mContext")?.let { if (it is Context) return it }
            return null
        }

        // ---------- 反射小工具 ----------

        /** 调用对象的无参方法 */
        @JvmStatic
        fun call1(target: Any?, name: String): Any? {
            return runCatching {
                val m = Rt.methodByName(target?.javaClass, name, 0)
                Rt.invoke(m, target, null)
            }.onFailure { SLog.e("Rx: call1 失败", it) }.getOrNull()
        }

        /** 调用类的静态无参方法 */
        @JvmStatic
        fun callStatic(clsName: String, method: String): Any? {
            return runCatching {
                val c = Class.forName(clsName)
                val m = Rt.methodByName(c, method, 0)
                Rt.invoke(m, null, null)
            }.onFailure { SLog.e("Rx: callStatic 失败 $method", it) }.getOrNull()
        }

        /** 安装广播接收与初始化（转发给 RxInit） */
        @JvmStatic
        fun install() = RxInit.install()

        /** 延迟初始化（拿到 Context 后再做重活） */
        @JvmStatic
        fun lazy(target: Any?) = RxInit.lazy(target)
    }
}
