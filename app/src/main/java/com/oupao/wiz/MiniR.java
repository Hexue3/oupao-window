package com.oupao.wiz;
public class MiniR implements java.lang.Runnable {
    private int op;
    public MiniR(int p1)
    {
        this.op = p1;
        return;
    }
    public void run()
    {
        int v0 = 0;
        try {
            v0 = this.op;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != 1) {
            if (v0 != 2) {
                if (v0 != 3) {
                    if (v0 != 4) {
                        if (v0 != 7) {
                            if (v0 != 5) {
                                com.oupao.wiz.MiniUI.rstPoll();
                                return;
                            } else {
                                com.oupao.wiz.MiniUI.lg("MiniR: run rm");
                                com.oupao.wiz.MiniUI.rm();
                                return;
                            }
                        } else {
                            com.oupao.wiz.MiniUI.lg("MiniR: run sync");
                            com.oupao.wiz.MiniUI.syncNow();
                            return;
                        }
                    } else {
                        com.oupao.wiz.MiniUI.lg("MiniR: run commit");
                        com.oupao.wiz.MiniUI.commit();
                        return;
                    }
                } else {
                    com.oupao.wiz.MiniUI.lg("MiniR: run hide");
                    com.oupao.wiz.MiniUI.hide();
                    return;
                }
            } else {
                com.oupao.wiz.MiniUI.lg("MiniR: run step");
                com.oupao.wiz.MiniUI.step();
                return;
            }
        } else {
            com.oupao.wiz.MiniUI.lg("MiniR: run show");
            com.oupao.wiz.MiniUI.show();
            return;
        }
    }
}