package com.oupao.wiz;

/**
 * 3442：**指针监听的注册重试**。
 *
 * 原来 `onWms()` 里是 `postDelayed(new FlyR(), 20000)` —— 开机后要等 **20 秒**
 * 扇形菜单才划得出来（用户："每次开机进桌面到能呼出菜单太久，不像引索条一进桌面就能滑"）。
 *
 * 现在改成 1.2s 起、每 400ms 重试一次，**注册成功就停**（最多 ~20s）。
 * `registerPointerEventListener` 失败时本来就没副作用，重试是安全的。
 */
public class FlyR implements java.lang.Runnable {
    /** 已经重试的次数（静态：多次 new 也共享预算） */
    private static int tries;

    public FlyR()
    {
        return;
    }

    public void run()
    {
        try {
            if (com.oupao.wiz.FlyH.reg) {
                return;                      // 已经注册上了
            }
            com.oupao.wiz.FlyH.reg();
            if (com.oupao.wiz.FlyH.reg) {
                com.oupao.wiz.SLog.i("Fly: \u6307\u9488\u76d1\u542c\u5c31\u7eea\uff08\u7b2c "
                        + (tries + 1) + " \u6b21\u5c1d\u8bd5\uff09");
                return;
            }
            if (tries++ < 50) {
                var h = com.oupao.wiz.Hooks.main();
                if (h != null) {
                    h.postDelayed(new FlyR(), 400L);
                }
            }
        } catch (Throwable ignore) {
        }
        return;
    }
}
