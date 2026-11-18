package com.oupao.wiz;
public class Tap {
    private static Object sCap;
    private static boolean sOn;
    private static Runnable sR;
    private Tap()
    {
        return;
    }
    public static void cancel()
    {
        com.oupao.wiz.Tap.sOn = false;
        return;
    }
    public static Object cap()
    {
        return com.oupao.wiz.Tap.sCap;
    }
    public static void down(Object p4, float p5, float p6)
    {
        com.oupao.wiz.Tap.sCap = p4;
        com.oupao.wiz.Tap.sOn = true;
        var v0_1 = com.oupao.wiz.Hooks.main();
        if (v0_1 != null) {
            var v1_1 = com.oupao.wiz.Tap.sR;
            if (v1_1 != null) {
                v0_1.removeCallbacks(v1_1);
            }
            var v1_0 = new com.oupao.wiz.Lp();
            com.oupao.wiz.Tap.sR = v1_0;
            v0_1.postDelayed(v1_0, 480);
        }
        return;
    }
    public static void fire()
    {
        if (com.oupao.wiz.Tap.sOn) {
            com.oupao.wiz.Tap.sOn = false;
            com.oupao.wiz.Tap.sR = null;
            if (com.oupao.wiz.Cfg.muteDirect()) {
                java.lang.Object v0_1 = com.oupao.wiz.Win.capTask(com.oupao.wiz.Tap.sCap);
                if (v0_1 == null) {
                    v0_1 = com.oupao.wiz.Hooks.task();
                }
                if (v0_1 != null) {
                    var v1 = com.oupao.wiz.Win.zoomState(v0_1);
                    var v2 = com.oupao.wiz.Win.taskPkg(v0_1);
                    var v3 = com.oupao.wiz.Win.taskUid(v0_1);
                    com.oupao.wiz.SLog.i(new StringBuilder("\u957f\u6309\u9759\u97f3: state=").append(v1).append(" pkg=").append(v2).toString());
                    if ((v1 >= 2) && ((3 >= v1) && ((v2 != null) && (v3 >= 0)))) {
                        com.oupao.wiz.Mute.toggle(v3, v2);
                    }
                }
            }
        }
        return;
    }
}