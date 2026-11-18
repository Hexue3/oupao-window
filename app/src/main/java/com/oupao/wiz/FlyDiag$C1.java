package com.oupao.wiz;
public class FlyDiag$C1 extends com.oupao.wiz.XMH {
    static int TH;
    static
    {
        com.oupao.wiz.FlyDiag$C1.TH = 150;
    }
    public FlyDiag$C1()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p9)
    {
        try {
            if ((com.oupao.wiz.Cfg.flyMini())
                    && (com.oupao.wiz.Cfg.flyMiniEasy())
                    && !com.oupao.wiz.Hooks.imeShown()) {   // 2817
                var v0_9 = p9.result;
                if ((v0_9 instanceof Integer)) {
                    var v2 = ((Integer) v0_9).intValue();
                    // ★★ 3179：**根因在这里**。
                    //   以前用 ROM 的第 4 个参数当"位移"来判断（args[3]），日志实测那个值是
                    //   2000 上下（不是位移，更像坐标/面积），永远大于阈值 150 →
                    //   **任何**手势都被判定成"上滑"，于是 0 被改写成迷你窗：
                    //     左右滑小白条 → 变成转迷你窗/拖动，贴边彻底失效。
                    //   现在改用**我们自己跟踪的真实位移**：必须"向上 + 纵向占优 + 距离够"。
                    float dy = com.oupao.wiz.Hooks.sMoveDy;
                    float dx = com.oupao.wiz.Hooks.sMoveDx;
                    int thr = com.oupao.wiz.Cfg.miniSwipe();
                    boolean up = (dy < 0f) && (Math.abs(dy) > Math.abs(dx) * 1.2f)
                            && (Math.abs(dy) > thr);
                    if ((v2 == -1 || v2 == 6) && up) {
                        p9.setResult(Integer.valueOf(0));
                        com.oupao.wiz.SLog.i("FlyMini: 上滑(dy=" + ((int) dy) + " dx=" + ((int) dx)
                                + " thr=" + thr + ") → 转迷你窗");
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        com.oupao.wiz.Hooks.onGestureMode(p9);
        return;
    }
}