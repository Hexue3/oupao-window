package com.oupao.wiz;
public class FlyW extends com.oupao.wiz.XMH {
    public FlyW()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p3)
    {
        java.lang.Object v0 = null;
        try {
            v0 = p3.thisObject;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 == null) {
            return;
        }
        // 3440：只有"第一次拿到"才跑 onWms()（它会注册指针监听等）。
        //   额外挂的 addWindow/removeWindow 是在 WMS 持有锁时回调的，
        //   所以 onWms 必须 post 到主线程去做，不能在钩子里同步跑（死锁风险）。
        boolean first;
        synchronized (FlyW.class) {
            first = com.oupao.wiz.FlyH.wms == null;
            com.oupao.wiz.FlyH.wms = v0;
        }
        if (first) {
            try {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        try {
                            com.oupao.wiz.FlyH.onWms();
                        } catch (Throwable ignore) {
                        }
                    }
                });
            } catch (Throwable ignore) {
            }
        }
        return;
    }
}
