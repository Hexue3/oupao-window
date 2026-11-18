package com.oupao.wiz;
public final class FlyInput {
    private static long down;
    private static boolean ended;
    private static int lastAction;
    private static long lastTime;
    private static long otherDown;
    private static long startTime;
    private static float startX;
    private static float startY;
    static
    {
        com.oupao.wiz.FlyInput.down = -1;
        com.oupao.wiz.FlyInput.lastTime = -1;
        com.oupao.wiz.FlyInput.startTime = -1;
        com.oupao.wiz.FlyInput.otherDown = -1;
        com.oupao.wiz.FlyInput.lastAction = -1;
        com.oupao.wiz.FlyInput.ended = true;
    }
    private FlyInput()
    {
        return;
    }
    public static void accept(int p8, int p9, long p10, long p12, float p14, float p15)
    {
        if (((p8 == 1) || (p8 == 2)) && ((p9 >= 0) && (p9 <= 3))) {
            // 字母引索：边缘竖滑呼出（与扇形菜单互不影响）
            try {
                // ★ 2766：旧菜单改由"旁听"驱动（呼出方式换了，菜单本体没动）
                com.oupao.wiz.FlyFanBr.accept(p9, p14, p15);
                if (Boolean.TRUE.booleanValue()) {
                    return;
                }
                // 感应条能真正收到触摸时，手势已经由它驱动了；
                // 这里再喂一遍会让同一条 MOVE 被处理两次（拖起来不跟手）。
                if (!com.oupao.wiz.IdxBand.activeAt(p14)) {
                    com.oupao.wiz.Idx.feed(p9, p14, p15);
                }
            } catch (Throwable ignore) {
            }
            try {
                if (p9 != 0) {                    if (((p10 == com.oupao.wiz.FlyInput.down) || (p10 == com.oupao.wiz.FlyInput.otherDown)) && (!com.oupao.wiz.FlyInput.ended)) {
                        if (p9 != 3) {
                            if (p12 >= com.oupao.wiz.FlyInput.lastTime) {
                                if ((p12 != com.oupao.wiz.FlyInput.lastTime) || (p9 != com.oupao.wiz.FlyInput.lastAction)) {
                                    com.oupao.wiz.FlyInput.lastTime = p12;
                                    com.oupao.wiz.FlyInput.lastAction = p9;
                                    com.oupao.wiz.FlyH.pressUp();
                                    if (p9 == 1) {
                                        com.oupao.wiz.FlyInput.ended = true;
                                    }
                                    com.oupao.wiz.FlyH.handle(p9, p14, p15);
                                    if (p9 != 1) {
                                    } else {
                                        com.oupao.wiz.FlyS.unpil();
                                        com.oupao.wiz.FlyH.zdrv = false;
                                        // ★ 2722：这一下没把菜单呼出来 → 判定为"点击"，由我们重放回下层。
                                        //   原因：Launcher 侧闸门（FlyGate）为了不让"呼出菜单瞬间触发上滑手势"
                                        //   已经把这条流从桌面手势链里拿掉了，ColorOS 自带的"点击重放"也就没了；
                                        //   不补这一步，热区里的点击会整个消失。
                                        // 2723：点击改由桌面的 consumer 重放（我们只丢 MOVE，见 FlyGate），这里不再自注入                                        }
                                        com.oupao.wiz.SLog.i(new StringBuilder().append("FlyInput: end down=").append(p10).append(" st=").append(com.oupao.wiz.FlyH.st).toString());
                                    }
                                } else {
                                }
                            }
                            return;
                        } else {
                            com.oupao.wiz.FlyInput.ended = true;
                            com.oupao.wiz.FlyH.pressUp();
                            if ((!com.oupao.wiz.Cfg.flyMaster()) || ((!com.oupao.wiz.FlyS.pil) || ((com.oupao.wiz.FlyH.st != 2) || (com.oupao.wiz.FlyH.win == null)))) {
                                com.oupao.wiz.FlyH.cancel();
                                com.oupao.wiz.SLog.i(new StringBuilder().append("FlyInput: CANCEL -> hide down=").append(p10).toString());
                            } else {
                                com.oupao.wiz.FlyH.sel = -1;
                                com.oupao.wiz.FlyH.stayOrClose();
                                com.oupao.wiz.SLog.i(new StringBuilder().append("FlyInput: takeover CANCEL -> stay down=").append(p10).toString());
                            }
                            com.oupao.wiz.FlyS.unpil();
                            com.oupao.wiz.FlyH.zdrv = false;
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    if ((p12 != com.oupao.wiz.FlyInput.startTime) || ((Math.abs((p14 - com.oupao.wiz.FlyInput.startX)) >= 0.5f) || (Math.abs((p15 - com.oupao.wiz.FlyInput.startY)) >= 0.5f))) {
                        if (p10 != com.oupao.wiz.FlyInput.otherDown) {
                            if (p10 > com.oupao.wiz.FlyInput.down) {
                                com.oupao.wiz.FlyInput.down = p10;
                                com.oupao.wiz.FlyInput.otherDown = -1;
                                com.oupao.wiz.FlyInput.startTime = p12;
                                com.oupao.wiz.FlyInput.startX = p14;
                                com.oupao.wiz.FlyInput.startY = p15;
                                com.oupao.wiz.FlyInput.lastTime = p12;
                                com.oupao.wiz.FlyInput.lastAction = p9;
                                com.oupao.wiz.FlyInput.ended = true;
                                if (!com.oupao.wiz.FlyH.stick) {
                                    if (com.oupao.wiz.FlyH.st != 0) {
                                        com.oupao.wiz.FlyH.cancel();
                                    }
                                    int v9_2 = 0;
                                    com.oupao.wiz.FlyH.zdrv = false;
                                    com.oupao.wiz.FlyH.handle(p9, p14, p15);
                                    if (com.oupao.wiz.FlyH.st != 0) {
                                        v9_2 = 0;
                                    } else {
                                        v9_2 = 1;
                                    }
                                    com.oupao.wiz.FlyInput.ended = v9_2 != 0;
                                    if (!com.oupao.wiz.FlyInput.ended) {
                                        com.oupao.wiz.FlyH.pressDown(p14, p15);
                                    }
                                    com.oupao.wiz.SLog.r("r-fly-input", new StringBuilder().append("FlyInput: begin src=").append(p8).append(" down=").append(p10).append(" event=").append(p12).append(" st=").append(com.oupao.wiz.FlyH.st).toString(), 5000L);
                                    return;
                                } else {
                                    return;
                                }
                            } else {
                            }
                        }
                        com.oupao.wiz.SLog.i(new StringBuilder().append("FlyInput: duplicate/stale DOWN src=").append(p8).append(" down=").append(p10).toString());
                        return;
                    } else {
                        if (p10 != com.oupao.wiz.FlyInput.down) {
                            com.oupao.wiz.FlyInput.otherDown = p10;
                        }
                        com.oupao.wiz.SLog.i(new StringBuilder().append("FlyInput: alias DOWN src=").append(p8).append(" down=").append(p10).append(" event=").append(p12).append(" canonical=").append(com.oupao.wiz.FlyInput.down).toString());
                        return;
                    }
                }
            } catch (Throwable __e_v8_25) {
                com.oupao.wiz.FlyInput.ended = true;
                com.oupao.wiz.FlyH.cancel();
                com.oupao.wiz.FlyS.unpil();
                com.oupao.wiz.FlyH.zdrv = false;
                com.oupao.wiz.SLog.e("FlyInput: stream failed", __e_v8_25);
            }
            return;
        } else {
            return;
        }
    }
    public static void post(android.view.MotionEvent p3)
    {
        try {
            if (com.oupao.wiz.FlyH.reg) {
                if (p3.getActionMasked() <= 3) {
                    var v0_4 = new com.oupao.wiz.FlyQ();
                    v0_4.a = p3.getActionMasked();
                    v0_4.x = p3.getRawX();
                    v0_4.y = p3.getRawY();
                    v0_4.d = p3.getDownTime();
                    v0_4.t = p3.getEventTime();
                    var v3_1 = com.oupao.wiz.Hooks.main();
                    if (v3_1 == null) {
                        return;
                    } else {
                        v3_1.post(v0_4);
                        return;
                    }
                } else {
                }
            }
        } catch (Throwable __e_v3_2) {
            com.oupao.wiz.SLog.e("FlyInput: post failed", __e_v3_2);
            return;
        }
        return;
    }
    public static void run(com.oupao.wiz.FlyQ p9)
    {
        com.oupao.wiz.FlyH.pend = false;
        com.oupao.wiz.FlyInput.accept(1, p9.a, p9.d, p9.t, p9.x, p9.y);
        return;
    }
    public static void zone(android.view.MotionEvent p8)
    {
        com.oupao.wiz.FlyInput.accept(2, p8.getActionMasked(), p8.getDownTime(), p8.getEventTime(), p8.getRawX(), p8.getRawY());
        return;
    }
}