package com.oupao.wiz;

import android.os.IBinder;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

/**
 * 触摸事件注入（把感应条吃掉的触摸还给下层 app）。
 *
 * 沿用的索引触发判定：
 * `ServiceManager.getService("input")` → `IInputManager$Stub.asInterface(binder)`
 * → `injectInputEvent(MotionEvent, 0)`。
 *
 * 它没有用 `InputManager.getInstance()`（那个在 system_server 里能取到实例，
 * 但注入链路上会走另一套策略；系统实现用的一直是 IInputManager binder 代理）。
 * 这里两种都备着：优先 IInputManager，失败再退 InputManager。
 *
 * 需要 INJECT_EVENTS 权限 —— 我们在 system_server 里，本来就有。
 */
public final class IdxTouch {

    private static final int MODE_ASYNC = 0;

    private static boolean probed;
    private static Object target;          // IInputManager 代理 或 InputManager 实例
    private static java.lang.reflect.Method injectM;

    private IdxTouch() {
    }

    static synchronized boolean ready() {
        if (probed) {
            return target != null && injectM != null;
        }
        probed = true;
        // 1) 首选：IInputManager binder 代理（与系统实现一致）
        try {
            // android.os.ServiceManager 是 @hide，编译期看不到，只能反射
            var smc = com.oupao.wiz.Rt.cls(null, "android.os.ServiceManager");
            IBinder b = null;
            if (smc != null) {
                var get = smc.getDeclaredMethod("getService", String.class);
                get.setAccessible(true);
                var r = get.invoke(null, "input");
                if (r instanceof IBinder) {
                    b = (IBinder) r;
                }
            }
            if (b != null) {
                var stub = com.oupao.wiz.Rt.cls(null, "android.hardware.input.IInputManager$Stub");
                if (stub != null) {
                    var asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
                    asInterface.setAccessible(true);
                    var iim = asInterface.invoke(null, b);
                    if (iim != null) {
                        var m = iim.getClass().getMethod("injectInputEvent",
                                InputEvent.class, Integer.TYPE);
                        m.setAccessible(true);
                        target = iim;
                        injectM = m;
                        com.oupao.wiz.SLog.i("IdxTouch: 注入就绪(IInputManager)");
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxTouch: IInputManager 不可用", t);
        }
        // 2) 兜底：InputManager.getInstance()
        try {
            var c = com.oupao.wiz.Rt.cls(null, "android.hardware.input.InputManager");
            if (c != null) {
                var gi = c.getDeclaredMethod("getInstance");
                gi.setAccessible(true);
                var im = gi.invoke(null);
                if (im != null) {
                    var m = c.getDeclaredMethod("injectInputEvent",
                            InputEvent.class, Integer.TYPE);
                    m.setAccessible(true);
                    target = im;
                    injectM = m;
                    com.oupao.wiz.SLog.i("IdxTouch: 注入就绪(InputManager)");
                    return true;
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxTouch: InputManager 不可用", t);
        }
        target = null;
        injectM = null;
        com.oupao.wiz.SLog.e("IdxTouch: 注入不可用，感应条保持不可触摸");
        return false;
    }

    /**
     * 2807：注入一个按键（用来收起输入法 —— 键盘在时 BACK 只收键盘，不会关掉小窗）。
     */
    static boolean sendKey(int code)
    {
        if (!ready()) {
            com.oupao.wiz.SLog.i("IdxTouch: 注入未就绪，按键放弃 code=" + code);
            return false;
        }
        try {
            long t = SystemClock.uptimeMillis();
            android.view.KeyEvent d = new android.view.KeyEvent(t, t,
                    android.view.KeyEvent.ACTION_DOWN, code, 0);
            d.setSource(android.view.InputDevice.SOURCE_KEYBOARD);   // 2822：不设源会被策略丢弃
            var r1 = injectM.invoke(target, d, Integer.valueOf(MODE_ASYNC));
            android.view.KeyEvent u = new android.view.KeyEvent(t, t,
                    android.view.KeyEvent.ACTION_UP, code, 0);
            u.setSource(android.view.InputDevice.SOURCE_KEYBOARD);
            var r2 = injectM.invoke(target, u, Integer.valueOf(MODE_ASYNC));
            // 3202：把注入结果打出来（之前忽略返回值，注入被策略丢掉也看不出来）
            com.oupao.wiz.SLog.i("IdxTouch: 注入按键 code=" + code
                    + " down=" + r1 + " up=" + r2
                    + " target=" + (target == null ? "null" : target.getClass().getSimpleName()));
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxTouch: 注入按键失败", t);
            return false;
        }
    }

    /** 注入一个事件（action 用 MotionEvent.ACTION_*） */
    static volatile long injectUntil;

    /** 2835：自己的重放注入刚发出（MODE_ASYNC，到达钩子有延迟）。FlyGate 要在这个窗口内放行。 */
    public static boolean injectingNow() {
        return android.os.SystemClock.uptimeMillis() < injectUntil;
    }

    static boolean send(long downTime, long eventTime, int action, float x, float y) {
        if (!ready()) {
            return false;
        }
        MotionEvent e = null;
        try {
            e = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
            if (e == null) {
                return false;
            }
            e.setSource(InputDevice.SOURCE_TOUCHSCREEN);   // 0x1002
            injectUntil = android.os.SystemClock.uptimeMillis() + 400L;   // 2835：这个窗口内不挞
            var r = injectM.invoke(target, e, MODE_ASYNC);
            return (r instanceof Boolean) && ((Boolean) r);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxTouch: 注入失败 a=" + action, t);
            return false;
        } finally {
            if (e != null) {
                try {
                    e.recycle();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * 把一段被吃掉的触摸**原样重放**给下层：
     * DOWN → 若干 MOVE → UP，坐标和时间都用当前时刻重设。
     */
    static boolean replay(float[] xs, float[] ys, int n) {
        if (!ready()) {
            return false;
        }
        long down = SystemClock.uptimeMillis();
        boolean ok = send(down, down, MotionEvent.ACTION_DOWN, xs[0], ys[0]);
        if (!ok) {
            return false;
        }
        for (int i = 1; i < n; i++) {
            send(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, xs[i], ys[i]);
        }
        send(down, SystemClock.uptimeMillis() + 1L, MotionEvent.ACTION_UP,
                xs[n - 1], ys[n - 1]);
        return true;
    }
}
