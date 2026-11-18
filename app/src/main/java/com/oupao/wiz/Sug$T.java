package com.oupao.wiz;
public class Sug$T implements java.lang.Runnable {
    private static int tries;
    public Sug$T()
    {
        return;
    }
    public void run()
    {
        do {
            var v0_0 = com.oupao.wiz.Sug$T.tries;
            if (v0_0 < 40) {
                com.oupao.wiz.Sug$T.tries = (v0_0 + 1);
                try {
                    Thread.sleep(15000);
                } catch (Throwable __e_v0_1) {
                    com.oupao.wiz.SLog.e("Sug: boot\u68c0\u67e5\u5f02\u5e38", __e_v0_1);
                }
            } else {
                com.oupao.wiz.SLog.i("Sug: boot\u7b49\u5f85\u8d85\u65f6(10min), \u653e\u5f03\u521d\u59cb\u5316");
                return;
            }
        } while(!"1".equals(com.oupao.wiz.FrameworkExt.sysProp("sys.boot_completed")));
        try {
            com.oupao.wiz.SLog.i("Sug: boot\u5b8c\u6210\uff0c\u5f00\u59cb\u521d\u59cb\u5316(\u7ebf\u7a0b)");
            com.oupao.wiz.Sug.init();
            return;
        } catch (Throwable __e_v0_3) {
            com.oupao.wiz.SLog.e("Sug: init\u5f02\u5e38(\u7ebf\u7a0b)", __e_v0_3);
            return;
        }
    }
}