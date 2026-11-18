package com.oupao.wiz;

/**
 * 运行时系统版本读取。
 *
 * 为什么不能直接写 {@code android.os.Build.VERSION.SDK_INT}：
 * 它在本工程里是 {@code static final int}，javac 会按编译期 android.jar 里的
 * 常量值（35）**内联并折叠**所有直接比较——
 *
 * <pre>
 *   if (Build.VERSION.SDK_INT &lt; 26) { 旧实现 }   // 35 &lt; 26 恒假 → 整段被删
 *   int sdk = Build.VERSION.SDK_INT;              // 直接变成 int sdk = 35
 * </pre>
 *
 * 结果就是真机上永远走「SDK 35」的分支（`regBack` 的 33/34 判断、
 * Diag 里上报的 SDK 版本全都错）。这里改成反射读字段，拿到的是设备真实值。
 * 反射只做一次，结果缓存在非 final 字段里。
 */
public final class Sys {

    private static int sdk = -1;

    private Sys() {
    }

    /** android.os.Build.VERSION.SDK_INT 的真实运行时值 */
    public static int sdk() {
        if (sdk < 0) {
            int v;
            try {
                java.lang.reflect.Field f =
                        android.os.Build.VERSION.class.getField("SDK_INT");
                f.setAccessible(true);
                v = f.getInt(null);
            } catch (Throwable ignored) {
                v = 26;             // 兜底：按 minSdk 处理
            }
            sdk = v;
        }
        return sdk;
    }
}
