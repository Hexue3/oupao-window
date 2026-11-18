package com.oupao.wiz;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 让「不可调整大小(nonResizable)」的应用也能进 ColorOS 小窗。
 *
 * ## 问题（日志实锤）
 *
 * ```
 * FlexibleTaskController: isSupportFlexibleWindow target =
 *     com.coloros.translate/.ui.MainActivity ... support = false
 * OplusSplitScreenManagerService: … supportsSplitScreen by forceResizable activity return:true
 * TaskOrganizerController: onTaskInfoChanged … mWindowingMode=fullscreen
 * ```
 * 翻译应用的 `MainActivity` 在 manifest 里是 nonResizable，
 * ColorOS 判断「能不能进小窗」的**真正入口**是：
 *
 * ```
 * OplusZoomStarter.isSupportZoom(ActivityRecord)
 *   └─ OplusZoomWindowUtil.isSupportZoomMode(ComponentName)
 *        └─ FlexibleWindowUtils.isSupportFlexibleWindow(pkg, flattenedComponent)   ← 真正的判定
 * OplusZoomWindowUtil.isUnSupportFloatZoom(ComponentName)      ← 黑名单
 * OplusZoomWindowUtil.isUnSupportCallerZoomMode(pkg)           ← 调用方黑名单
 * FlexibleTaskController.isSupportFlexibleWindow(...)          ← 上层也有一份判定
 * ```
 *
 * 2634 只挂了最后那个（FlexibleTaskController），结果日志显示：
 * `Flex: startZoomWindow ret=0` 但任务仍 `mWindowingMode=fullscreen`
 * —— 因为 `isSupportZoomMode` 早就返回 false，压根没进 zoom 分支。
 *
 * ## 做法
 *
 * 把上面**四条判定**全部挂住，参数里出现翻译相关包时强制：
 * - `isSupportFlexibleWindow` / `isSupportZoomMode` → true
 * - `isUnSupportFloatZoom` / `isUnSupportCallerZoomMode` → false
 *
 * 只影响翻译相关的包，其它应用的小窗判定保持系统原样。
 */
public final class FlexFix {

    /** 目前只给翻译应用放开；以后要加就放这里 */
    private static final String[] ALLOW = new String[]{
            "com.coloros.translate",
            "com.oplus.translate",
            "com.coloros.ocrscanner"
    };

    /**
     * 2982：**不得强制放行小窗判定**的包（选择器类）。
     *
     * 用户指出：官方默认选择器是**全屏**的。而我们在这里把
     * `isSupportFlexibleWindow` 全局强制为 true，ColorOS 就把带
     * `oplus.intent.extra.allow_flexible_share=true` 的分享选择器开成了小窗。
     * → 选择器这几个包一律回到原厂判定（= 全屏）。
     */
    private static final String[] DENY = new String[]{
            "com.oplus.multiapp",
            "com.coloros.multiapp",
            "com.android.intentresolver",
            // 3243：**安装器一律不给小窗**（用户反馈"点 APK 后安装器变成小窗"）。
            //   我们这里把 isSupportFlexibleWindow 全局强制成 true，安装器本来 nonResizable、
            //   原厂是不会给它小窗的；放开后从小窗里的文件管理器点 APK，安装界面就落进小窗了。
            //   CrossZoom 那边只是"我们不接管"，拦不住 ROM 自己把子 Activity 开进同一个 zoom task，
            //   所以必须在判定层直接否决。
            "com.android.packageinstaller",
            "com.oplus.packageinstaller",
            "com.google.android.packageinstaller",
            "com.rosan.installer.x.revived"
    };

    /** 3243：包名/组件里带 installer 的一律按安装器处理（各家 ROM 包名不统一） */
    private static boolean looksInstaller(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        return n.contains("packageinstaller") || n.contains("packagesinstaller")
                || n.contains(".installer") || n.startsWith("installer");
    }

    private FlexFix() {
    }

    private static boolean anyDenied(Object[] args) {
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a == null) {
                continue;
            }
            String name = null;
            if (a instanceof String) {
                name = (String) a;
            } else if (a instanceof android.content.ComponentName) {
                name = ((android.content.ComponentName) a).getPackageName();
            } else {
                name = pkgOf(a);
            }
            if (name == null) {
                continue;
            }
            if (looksInstaller(name)) {
                return true;
            }
            for (int k = 0; k < DENY.length; k++) {
                String d = DENY[k];
                if (d.equals(name) || name.startsWith(d + ".") || name.startsWith(d + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean allowed(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < ALLOW.length; i++) {
            String p = ALLOW[i];
            if (p.equals(s) || s.startsWith(p + ".") || s.startsWith(p + "/")
                    || s.startsWith(p + "#")) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyAllowed(Object[] args) {
        if (args == null) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a == null) {
                continue;
            }
            if (a instanceof String) {
                if (allowed((String) a)) {
                    return true;
                }
                continue;
            }
            if (a instanceof android.content.ComponentName) {
                if (allowed(((android.content.ComponentName) a).getPackageName())) {
                    return true;
                }
                continue;
            }
            String pkg = pkgOf(a);
            if (allowed(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static void install(ClassLoader cl) {
        int n = 0;
        // 1) 真正的判定：FlexibleWindowUtils.isSupportFlexibleWindow(String, String)
        n += hook(cl, "com.android.server.wm.FlexibleWindowUtils",
                "isSupportFlexibleWindow", Boolean.TRUE);
        // 2) zoom 入口的判定
        n += hook(cl, "com.android.server.wm.OplusZoomWindowUtil",
                "isSupportZoomMode", Boolean.TRUE);
        // 3) 两个黑名单判定 → 强制 false
        n += hook(cl, "com.android.server.wm.OplusZoomWindowUtil",
                "isUnSupportFloatZoom", Boolean.FALSE);
        n += hook(cl, "com.android.server.wm.OplusZoomWindowUtil",
                "isUnSupportCallerZoomMode", Boolean.FALSE);
        // 4) 上层（2634 已有）
        n += hook(cl, "com.android.server.wm.FlexibleTaskController",
                "isSupportFlexibleWindow", Boolean.TRUE);
        // 5) 2852：对齐系统实际接口——数量上限就在这里。
        //   开源注释：三个类需**同时放行**，任一遗漏仍会被原厂拦住。
        n += hook(cl, "com.android.server.wm.FlexibleWindowManagerService",
                "isSupportFlexibleWindow", Boolean.TRUE);
        n += hookMaxWinNum(cl);
        // 3311：C17 的「最多浮窗数」是独立值（固定 2），必须一起接管
        n += hookMaxFlexibleTaskNum(cl);
        // 上限调大后，服务内部那份「任务容量」也要跟着抬（原厂 4/7，装不下 8~10 个）
        n += hookTasksNumber(cl);
        // 3241：到上限时**禁止 ROM 自动贴边**（改成关掉最旧的）。见 NoDock 注释。
        try {
            com.oupao.wiz.NoDock.install(cl);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlexFix: NoDock 安装失败", t);
        }
        // 3242 撤回：`skipRememberPosition -> false` 只对「普通小窗」有效（按包名记 bounds），
        //   对迷你窗无效（迷你窗是桌面侧的 TaskView，位置由 launcher 决定）。
        //   副作用是新开窗会出现在该应用上次的位置 → 用户要求撤回，保持原厂。

        // 上限调大后多个小窗会叠在一处（原厂只做横向让位）→ 补纵向错开
        try {
            if (com.oupao.wiz.FloatCascade.install(cl)) {
                n++;
            }
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.SLog.i("FlexFix: \u5f3a\u5236\u5c0f\u7a97\u5224\u5b9a\u6302\u4e0a " + n + " \u4e2a\u65b9\u6cd5");
    }

    /**
     * 2983：「我们自己正在开小窗」的 4 秒宽限窗（由 CrossZoom.fire / 注入前置位）。
     */
    private static boolean ourZoomNow() {
        try {
            return limitRemoved() && com.oupao.wiz.CrossZoom.zoomSupportArmed();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 2852：开关判定（任一开启即放行，对齐系统实际判定） */
    private static boolean limitRemoved() {
        try {
            return com.oupao.wiz.Cfg.crossZoom() || com.oupao.wiz.Cfg.settingsZoom();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 小窗数量上限（FlymeUI 页 → `oupao_zoom_max_on` / `oupao_zoom_max`）。
     *
     * 目标：`com.android.server.wm.FlexibleWindowManagerService.getMaxWinNum(I)I`
     * （本机在 `oplus-services.jar`，参数名 `scenario`）。
     *
     * 本机事实（反编译核对）：
     * - `FlexibleWindowUtils.getMaxWinNum()`（静态）里写死 `getMaxWinNum(1)`，
     *   `FlexibleTaskController` 判断「还能不能开下一个小窗」全走这个静态方法；
     * - `getTasksNumber()` 内部也是 `getMaxWinNum(1)` 再按全景分支给 4/7。
     * → 所以只需要接管 `scenario == 1`，其余场景（平板/全景/多模）留给原厂。
     *
     * 两种行为：
     * - **开关开**：任何时刻都返回用户设定的数字（1~10）；
     * - **开关关**：保持 2852 的老行为 —— 只在我们自己开小窗的那 4 秒宽限窗内返回 12
     *   （解除上限，免得「打不开第 2 个小窗」），其余时刻仍走原厂。
     */
    private static int hookMaxWinNum(ClassLoader cl) {
        try {
            Class<?> c = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.FlexibleWindowManagerService");
            if (c == null) {
                com.oupao.wiz.SLog.i("FlexFix: 类未找到 FlexibleWindowManagerService(getMaxWinNum)");
                return 0;
            }
            int n = 0;
            Method[] ms = c.getDeclaredMethods();
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (!"getMaxWinNum".equals(m.getName()) || m.getReturnType() != Integer.TYPE) {
                    continue;
                }
                try {
                    m.setAccessible(true);
                } catch (Throwable ignore) {
                }
                final String tag = "getMaxWinNum/" + m.getParameterTypes().length;
                if (com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.XMH() {
                    @Override
                    public void beforeHookedMethod(com.oupao.wiz.XMP p) {
                        try {
                            // 本机 ROM 实测（oplus-services.jar classes3.dex）：
                            //   static FlexibleWindowUtils.getMaxWinNum() 里写死调 getMaxWinNum(1)，
                            //   而 FlexibleTaskController 的「还能不能开下一个小窗」全走那个静态方法。
                            //   所以只接管 scenario == 1（与系统实现一致），别的场景留给原厂
                            //   （场景 2/4/5 是平板/全景/多模，手机用不到，接管了反而危险）。
                            int scenario = p.argInt(0, -1);
                            if (scenario != 1) {
                                com.oupao.wiz.SLog.r("XZmax", "getMaxWinNum scenario=" + scenario
                                        + " (未接管)", 5000L);
                                return;
                            }
                            int v = 0;
                            if (com.oupao.wiz.Cfg.zoomMaxOn()) {
                                // 3309：这里**只负责解除数量上限**。
                                //   - 上限=1：Flyme 逻辑（NoDock 屏蔽自动贴边、ZoomGate 关最早）；
                                //   - 上限>=2：完全原生行为（到上限自动贴边、贴边满两个关最旧），
                                //     模块只把 ROM 的最大窗数改成设定值。
                                //   ROM 的计数比我们的 CapR 准（不受 caption 注册滞后影响）。
                                v = com.oupao.wiz.Cfg.zoomMax();
                            } else {
                                if (!ourZoomNow()) {
                                    return;
                                }
                                v = 12;                            // 2852：我们自己开窗时解除上限
                            }
                            p.result = Integer.valueOf(v);
                            p.returnEarly = true;
                            com.oupao.wiz.SLog.r("XZmax", "FlexibleWindowManagerService." + tag
                                    + " -> " + v, 3000L);
                        } catch (Throwable ignore) {
                        }
                    }
                }) != null) {
                    n++;
                }
            }
            com.oupao.wiz.SLog.i("FlexFix: getMaxWinNum 挂上 " + n + " 个");
            return n;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlexFix: hookMaxWinNum 失败", t);
            return 0;
        }
    }

    /**
     * `FlexibleWindowManagerService.getTasksNumber()` —— 服务内部的任务容量。
     *
     * 本机实现：`maxNumber = getMaxWinNum(1)`；`> 2 && 全景分支 ? 7 : 4`。
     * 上限设成 8~10 时这份容量还停在 4/7，装不下 → 这里保证「容量不小于上限」。
     * 只在开关打开时生效，关着就是原厂值。
     */
    private static int hookTasksNumber(ClassLoader cl) {
        try {
            Class<?> c = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.FlexibleWindowManagerService");
            if (c == null) {
                return 0;
            }
            Method[] ms = c.getDeclaredMethods();
            int n = 0;
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (!"getTasksNumber".equals(m.getName()) || m.getReturnType() != Integer.TYPE
                        || m.getParameterTypes().length != 0) {
                    continue;
                }
                try {
                    m.setAccessible(true);
                } catch (Throwable ignore) {
                }
                if (com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.XMH() {
                    @Override
                    public void afterHookedMethod(com.oupao.wiz.XMP p) {
                        try {
                            if (!com.oupao.wiz.Cfg.zoomMaxOn()) {
                                return;
                            }
                            int want = com.oupao.wiz.Cfg.zoomMax() + 1;   // 3239：与 getMaxWinNum 一致（多留 1 个槽）
                            Object r = p.result;
                            int cur = (r instanceof Integer) ? ((Integer) r).intValue() : 0;
                            if (cur < want) {
                                p.result = Integer.valueOf(want);
                                com.oupao.wiz.SLog.r("XZmax", "getTasksNumber " + cur + " -> " + want, 3000L);
                            }
                        } catch (Throwable ignore) {
                        }
                    }
                }) != null) {
                    n++;
                }
            }
            com.oupao.wiz.SLog.i("FlexFix: getTasksNumber 挂上 " + n + " 个");
            return n;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlexFix: hookTasksNumber 失败", t);
            return 0;
        }
    }

    /**
     * 3311（C17 用户反馈）：ColorOS 17 多了一个**独立的「最多浮窗数」**。
     *
     * 日志实证（C17，上限设 5）：
     * ```
     * updateTaskMaxNumWhenFocusChangedInPanoramic ...
     *   maxNum = 6, maxFlexibleTaskNum: 2
     * ```
     * `getMaxWinNum` 已经被我们改成 5，但 `maxFlexibleTaskNum` 仍写死 2 →
     * 第 3 扇窗一开就被贴边（用户说的"最多只能开两个"）。
     *
     * 老版本（C16）里 `FlexibleWindowUtils.getMaxFlexibleTaskNum()` 是
     * `全景机型 ? 2 : getMaxWinNum()`，手机上等于 getMaxWinNum，所以以前没有这个问题。
     * C17 改成固定 2，需要单独接管。
     *
     * 开关开 → 返回用户设定值（和 getMaxWinNum 一致）；开关关 → 原厂。
     */
    private static int hookMaxFlexibleTaskNum(ClassLoader cl) {
        int n = 0;
        String[] classes = new String[]{
                "com.android.server.wm.FlexibleWindowUtils",
                "com.android.server.wm.FlexibleWindowManagerService",
        };
        for (int k = 0; k < classes.length; k++) {
            try {
                Class<?> c = com.oupao.wiz.Rt.cls(cl, classes[k]);
                if (c == null) {
                    continue;
                }
                Method[] ms = c.getDeclaredMethods();
                for (int i = 0; i < ms.length; i++) {
                    Method m = ms[i];
                    if (!"getMaxFlexibleTaskNum".equals(m.getName())
                            || m.getReturnType() != Integer.TYPE) {
                        continue;
                    }
                    try {
                        m.setAccessible(true);
                    } catch (Throwable ignore) {
                    }
                    if (com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.XMH() {
                        @Override
                        public void beforeHookedMethod(com.oupao.wiz.XMP p) {
                            try {
                                if (!com.oupao.wiz.Cfg.zoomMaxOn()) {
                                    return;                // 开关关着 → 原厂
                                }
                                int v = com.oupao.wiz.Cfg.zoomMax();
                                com.oupao.wiz.SLog.r("XZmax", "getMaxFlexibleTaskNum -> " + v, 3000L);
                                p.result = Integer.valueOf(v);
                                p.returnEarly = true;
                            } catch (Throwable ignore) {
                            }
                        }
                    }) != null) {
                        n++;
                    }
                }
            } catch (Throwable ignore) {
            }
        }
        com.oupao.wiz.SLog.i("FlexFix: getMaxFlexibleTaskNum 挂上 " + n + " 个");
        return n;
    }

    /**
     * 把 cls 里所有名为 name、返回 boolean 的方法挂上，命中白名单/宽限窗时直接返回 result。
     */
    private static int hook(ClassLoader cl, String cls, String name, final Boolean result) {
        try {
            Class<?> c = com.oupao.wiz.Rt.cls(cl, cls);
            if (c == null) {
                com.oupao.wiz.SLog.i("FlexFix: \u7c7b\u672a\u627e\u5230 " + cls);
                return 0;
            }
            int n = 0;
            Method[] ms = c.getDeclaredMethods();
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (!name.equals(m.getName())) {
                    continue;
                }
                if (m.getReturnType() != Boolean.TYPE) {
                    continue;
                }
                try {
                    m.setAccessible(true);
                } catch (Throwable ignore) {
                }
                final String tag = cls.substring(cls.lastIndexOf('.') + 1) + "." + name
                        + "/" + m.getParameterTypes().length;
                if (com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.XMH() {
                    @Override
                    public void beforeHookedMethod(com.oupao.wiz.XMP p) {
                        try {
                            // 2852：对齐系统实际判定 —— 不看目标是谁，只看开关。
                            // ★ 2967（用户：B站/抖音 点内部页面会被拉成小窗，"要严格控制边界，
                            //   很容易传导到别的 app"）：
                            //   **只有「我们自己正在开小窗」的那 4 秒宽限窗内**才强制放行。
                            //   B站/抖音 这类 App 自己会调 ColorOS 的小窗 API（`androidx.flexible`），
                            //   原来这个全局放行把它们的请求也放过去了 → 点视频/订单页被拉成小窗。
                            //   现在它们自己的请求回到**原厂判定**（不支持就是不支持）。
                            // 白名单（翻译类）永远放行；其它只有「我们自己正在开小窗」的宽限窗内才放行
                            // 2982：选择器类（multiapp/intentresolver）**永远不强制**，回到原厂判定
                            if (anyDenied(p.args)) {
                                return;
                            }
                            // 2983：**白名单式** —— 只有「我们自己正在开小窗」的 4 秒窗口内才强制放行；
                            //   其余（包括 B站/抖音自己调小窗 API）一律回到原厂判定。
                            if (!anyAllowed(p.args) && !ourZoomNow()) {
                                return;
                            }
                            p.result = result;
                            p.returnEarly = true;
                            com.oupao.wiz.SLog.i("FlexFix: " + tag + " -> " + result);
                        } catch (Throwable ignore) {
                        }
                    }
                }) != null) {
                    n++;
                }
            }
            com.oupao.wiz.SLog.i("FlexFix: " + cls + "." + name + " \u6302\u4e0a " + n + " \u4e2a");
            return n;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlexFix: hook " + cls + "." + name + " \u5931\u8d25", t);
            return 0;
        }
    }

    /** 从各种可能的目标对象里抠出包名 */
    private static String pkgOf(Object o) {
        if (o == null) {
            return null;
        }
        try {
            var m = o.getClass().getMethod("getPackageName");
            Object v = m.invoke(o);
            if (v instanceof String) {
                return (String) v;
            }
        } catch (Throwable ignore) {
        }
        String[] fields = new String[]{"packageName", "mActivityComponent", "intent", "topActivity"};
        for (int i = 0; i < fields.length; i++) {
            try {
                Field f = o.getClass().getField(fields[i]);
                Object v = f.get(o);
                if (v instanceof String) {
                    return (String) v;
                }
                if (v instanceof android.content.ComponentName) {
                    return ((android.content.ComponentName) v).getPackageName();
                }
                if (v instanceof android.content.Intent) {
                    return ((android.content.Intent) v).getPackage();
                }
                if (v != null) {
                    String r = pkgOf(v);
                    if (r != null) {
                        return r;
                    }
                }
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
}
