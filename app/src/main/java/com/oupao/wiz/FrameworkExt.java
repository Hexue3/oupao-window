package com.oupao.wiz;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 框架隐藏 API / ColorOS 专有 API 的反射桥接层。
 *
 * <p>为什么需要这一层：这些 API 在 AOSP 与公开 SDK 里都不存在或被隐藏：
 * <ul>
 *   <li>{@code android.os.SystemProperties} —— 隐藏类，读系统属性用</li>
 *   <li>{@code android.os.UserHandle.of(int)} / {@code myUserId()} —— 隐藏方法</li>
 *   <li>{@code android.os.UserManager.getUsers()} —— 隐藏方法（公开的是 getUserProfiles）</li>
 *   <li>{@code android.graphics.drawable.AdaptiveIconDrawable.getLayerCount()/getLayer(int)}
 *       —— ColorOS 私有扩展，公开 SDK 里没有</li>
 * </ul>
 *
 * <p>这些 API 在目标设备（ColorOS）上确实存在，直接写会编译不过；
 * 写成反射则编译期无需依赖，运行时按可用性降级——取不到就返回兜底值，
 * 绝不抛异常打断业务流程。
 *
 * <p>所有方法都是「尽力而为」语义：反射失败时返回 null / 0 / 空串，
 * 调用方不需要写 try-catch。
 */
public final class FrameworkExt {

    private FrameworkExt() {
    }

    // ---------------- SystemProperties ----------------

    /** 读系统属性，取不到返回空串。 */
    public static String sysProp(String key) {
        return sysProp(key, "");
    }

    /** 读系统属性，取不到返回默认值 {@code def}。 */
    public static String sysProp(String key, String def) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getMethod("get", String.class, String.class);
            Object v = m.invoke(null, key, def);
            return v == null ? def : v.toString();
        } catch (Throwable t) {
            return def;
        }
    }

    /** 读系统属性（int 形式），取不到返回 0。 */
        /** 读系统属性（long 形式），取不到返回 0。 */
        /** 系统属性标志位（getFlags），取不到返回 0。 */
        // ---------------- UserHandle / UserManager ----------------

    /** 当前用户 id，取不到返回 0。 */
    public static int myUserId() {
        try {
            Class<?> c = Class.forName("android.os.UserHandle");
            Method m = c.getMethod("myUserId");
            Object v = m.invoke(null);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * 构造指定 uid 对应的 UserHandle，取不到返回 null。
     *
     * <p>{@code android.os.UserHandle} 本身是公开类（只有 of() 等是
     * &#64;hide），所以可以直接用真实类型做返回值——
     * {@code Context.startActivityAsUser(Intent, UserHandle)} 与
     * {@code LauncherApps.startMainActivity(ComponentName, UserHandle, ...)}
     * 都要求这个类型，返 Object 会报「找不到符号」。
     */
    public static android.os.UserHandle userHandleOf(int uid) {
        try {
            // of() 是 @hide，编译期拿不到；用反射调，返回值仍是真实类型
            java.lang.reflect.Method m = android.os.UserHandle.class
                    .getMethod("of", int.class);
            return (android.os.UserHandle) m.invoke(null, uid);
        } catch (Throwable t) {
            return null;
        }
    }

    /** UserHandle 的 Class 对象（隐藏类，运行时反射获取），取不到返回 null。 */
    public static Class<?> userHandleClass() {
        try {
            return Class.forName("android.os.UserHandle");
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * UserManager.getUsers()，取不到返回空 List。
     *
     * <p>smali 里调用点紧接着就是 {@code List.iterator()}，所以必须返回 List
     * （早先返 Object[]，javac 报「找不到符号 iterator()」）。
     */
    public static java.util.List<Object> usersOf(Object userManager) {
        java.util.List<Object> empty = java.util.Collections.emptyList();
        if (userManager == null) {
            return empty;
        }
        try {
            Method m = userManager.getClass().getMethod("getUsers");
            Object v = m.invoke(userManager);
            if (v instanceof java.util.List) {
                return new java.util.ArrayList<>((java.util.List<?>) v);
            }
            if (v instanceof Object[]) {
                return java.util.Arrays.asList((Object[]) v);
            }
        } catch (Throwable t) {
            // 退回到公开 API
        }
        try {
            Method m = userManager.getClass().getMethod("getUserProfiles");
            Object v = m.invoke(userManager);
            if (v instanceof java.util.List) {
                return new java.util.ArrayList<>((java.util.List<?>) v);
            }
            if (v instanceof Object[]) {
                return java.util.Arrays.asList((Object[]) v);
            }
        } catch (Throwable t) {
            // ignore
        }
        return empty;
    }

    /** UserInfo.id，取不到返回 0。 */
    public static int userInfoId(Object userInfo) {
        if (userInfo == null) {
            return 0;
        }
        try {
            Field f = userInfo.getClass().getField("id");
            Object v = f.get(userInfo);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---------------- ColorOS AdaptiveIconDrawable ----------------

    /** ColorOS 扩展：AdaptiveIconDrawable 的图层数，取不到返回 0。 */
    public static int adaptiveIconLayerCount(Object drawable) {
        if (drawable == null) {
            return 0;
        }
        try {
            Method m = drawable.getClass().getMethod("getLayerCount");
            Object v = m.invoke(drawable);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** ColorOS 扩展：取 AdaptiveIconDrawable 的第 index 个图层，取不到返回 null。 */
    public static Object adaptiveIconLayer(Object drawable, int index) {
        if (drawable == null) {
            return null;
        }
        try {
            Method m = drawable.getClass().getMethod("getLayer", int.class);
            return m.invoke(drawable, index);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 取 Drawable 的某个图层（兼容 ColorOS 扩展与标准 API），取不到返回 null。
     * 返回 Drawable 而不是 Object：调用点紧接着就是 setBounds/draw，
     * 收成 Object 会让 javac 报「找不到符号」。
     */
    public static android.graphics.drawable.Drawable drawableLayer(
            Object drawable, int index) {
        if (drawable == null) {
            return null;
        }
        // 优先 ColorOS 扩展
        Object o = adaptiveIconLayer(drawable, index);
        if (o != null) {
            return (android.graphics.drawable.Drawable) o;
        }
        try {
            Method m = drawable.getClass().getMethod("getLayer", int.class);
            return (android.graphics.drawable.Drawable) m.invoke(drawable, index);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 取 Drawable 的图层数（兼容 ColorOS 扩展），取不到返回 0。 */
    public static int drawableLayerCount(Object drawable) {
        if (drawable == null) {
            return 0;
        }
        int n = adaptiveIconLayerCount(drawable);
        if (n > 0) {
            return n;
        }
        try {
            Method m = drawable.getClass().getMethod("getLayerCount");
            Object v = m.invoke(drawable);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---------------- 通用工具 ----------------

    /** 安全地读取实例字段值（兼容隐藏字段），取不到返回 null。 */
    public static Object field(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        try {
            Class<?> c = obj.getClass();
            Field f = c.getField(name);
            return f.get(obj);
        } catch (Throwable t) {
            try {
                Field f = obj.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    /** 安全地调用无参方法，取不到返回 null。 */
    public static Object call(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        try {
            Method m = obj.getClass().getMethod(name);
            return m.invoke(obj);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 把一组对象转成 List，便于遍历。 */
        // ---------------- @hide：Context.startActivityAsUser ----------------

    /** Context.startActivityAsUser(Intent, UserHandle)——@hide，反射调用。 */
    public static void startActivityAsUser(Object context, Object intent, Object user) {
        if (context == null || intent == null) {
            return;
        }
        try {
            Method m = context.getClass().getMethod("startActivityAsUser",
                    android.content.Intent.class, android.os.UserHandle.class);
            m.invoke(context, intent, user);
        } catch (Throwable t) {
            try {
                context.getClass().getMethod("startActivity", android.content.Intent.class)
                        .invoke(context, intent);
            } catch (Throwable ignored) {
            }
        }
    }

    // ---------------- @hide：PendingIntent.getIntent / send ----------------

    /** PendingIntent.getIntent()——@hide。 */
    public static android.content.Intent pendingIntentGetIntent(Object pi) {
        if (pi == null) {
            return null;
        }
        try {
            Method m = pi.getClass().getMethod("getIntent");
            return (android.content.Intent) m.invoke(pi);
        } catch (Throwable t) {
            return null;
        }
    }

    /** PendingIntent.send(Context)——@hide 的单参重载。 */
    public static boolean pendingIntentSend(Object pi, Object context) {
        if (pi == null) {
            return false;
        }
        try {
            Method m = pi.getClass().getMethod("send", android.content.Context.class);
            m.invoke(pi, context);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** PendingIntent.send(Context, int, Intent, OnFinished, Handler, String, Bundle)。 */
    public static boolean pendingIntentSendFull(Object pi, Object context, int code,
                                                Object intent, Object onFinished,
                                                Object handler, String str, Object bundle) {
        if (pi == null) {
            return false;
        }
        try {
            Method m = pi.getClass().getMethod("send",
                    android.content.Context.class, int.class, android.content.Intent.class,
                    Class.forName("android.app.PendingIntent$OnFinished"),
                    android.os.Handler.class, String.class, android.os.Bundle.class);
            m.invoke(pi, context, Integer.valueOf(code), intent, onFinished, handler, str, bundle);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * AppOpsManager.checkOpNoThrow(int op, int uid, String pkg)。
     *
     * 这个重载在 android-35 的 stub jar 里是 @hide（只剩 String 版），
     * 设备运行时却一直都在——原 smali 调的就是它，所以走反射。
     * 取不到时返回 0（MODE_ALLOWED），与原逻辑的默认值一致。
     */
    public static int appOpsCheck(Object appOps, int op, int uid, String pkg) {
        if (appOps == null) {
            return 0;
        }
        try {
            Method m = appOps.getClass().getMethod("checkOpNoThrow",
                    int.class, int.class, String.class);
            Object r = m.invoke(appOps, Integer.valueOf(op), Integer.valueOf(uid), pkg);
            if (r instanceof Integer) {
                return ((Integer) r).intValue();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
