package com.oupao.wiz;

/**
 * 触桥：**WMS 指针旁听**（只读不拦）→ 驱动自绘扇形菜单 [FlyFanH]。
 *
 * 为什么用旁听：起手点不受 ColorOS 角落手势区限制，从最死角也能呼出；
 * 而且我们不消费、不改分发，点击行为完全不受影响。
 */
public final class FlyFanBr {

    public static int side = 0;
    public static float downX;
    public static float downY;
    public static boolean moved;
    public static float lastP;
    public static int lastSel = -1;
    /**
     * 2774：这一条手势是不是已经被扇形菜单接管了。
     * 必须独立记着 —— 因为旁听（这里）先收到 UP 就把菜单 hide 了，
     * 等 FlyZone 的 UP 再来问 alive() 已经是 false，就会误判成"纯点击"再重放一次点击。
     */
    public static boolean gestured;

    // 2825：键盘弹起时，小窗底部横条的触摸会被输入法吃掉（实测连 getGestureMode 都不调用），
    //   所以在这里（WMS 旁听层，能看到所有触摸）自己认这个手势：
    //   DOWN 落在横条里 + 键盘在 + 往上滑够 → 收键盘后补一次横条上滑（转迷你窗）。
    private static float imeDownY;

    // 2794：停留（未选应用时松手收起=关）状态下，用来识别"上滑手势"→ 自动关菜单
    private static float upDownX;
    private static float upDownY;
    private static boolean upTrack;

    /** 上滑手势阈值：位移 ≥60px 且以竖直为主（上滑回桌面那种） */
    private static final float UP_SWIPE = 60.0f;

    private static final android.os.Handler H =
            new android.os.Handler(android.os.Looper.getMainLooper());

    /** 看门狗：漏事件时兜底收菜单 */
    private static final Runnable WD = new Runnable() {
        public void run() {
            try {
                // 2776：停留等选择时，700ms 没事件也不能收（否则刚停住就被看门狗关掉）
                if (com.oupao.wiz.FlyFanH.alive() && !com.oupao.wiz.FlyFanH.staying()) {
                    // 3436 探针：这条以前是静默的，看不出"菜单是被谁收掉的"
                    com.oupao.wiz.SLog.i("FlyFanBr: 看门狗 700ms 无事件 → 收菜单");
                    com.oupao.wiz.FlyFanH.hide();
                    com.oupao.wiz.FlyState.setOpen(false);
                }
            } catch (Throwable ignore) {
            }
            reset();
        }
    };

    private FlyFanBr() {
    }

    public static void accept(int a, float x, float y) {
        try {
            H.removeCallbacks(WD);
            H.postDelayed(WD, 700L);
            // 3130：C17 上 system_server 写 sys.oupao.fly.r/.lr 读回是空的（FlyState 恒 0）→
            //   这里优先用同进程 FlyS 的本地副本，属性只作为兜底。否则整段直接被下面的 return 吃掉，
            //   表现就是「左右下角划不出扇形菜单、热区只把点击吃掉再重放」。
            int lr = com.oupao.wiz.FlyS.lrNow();
            int r = com.oupao.wiz.FlyS.radiusNow();
            android.content.Context c = com.oupao.wiz.Cfg.sysContext();
            if ((lr == 0) || (r <= 0) || (c == null)) {
                com.oupao.wiz.SLog.r("r-fanbr", "FlyFanBr: 几何为空 lr=" + lr + " r=" + r
                        + " ctx=" + (c != null), 5000L);
                return;
            }
            android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
            // 2943（用户：移除「收回键盘后补发上滑」的逻辑）：
            //   原来这里在每次 DOWN 算横条矩形、跟踪上滑，然后叫
            //   `Hooks.replayMiniAfterIme()`（先收键盘再补发手势）。每按一个键都走一遍，
            //   既慢又不准。现在键盘在时直接走系统自己的转迷你入口
            //   （`Hooks.tryNativeMini`），整段删除。
            if (a == 0) {
                reset();
                gestured = false;
                // ★ 2817：游戏模式屏蔽（「屏蔽左右下角呼出菜单」）—— 以前只有旧的 FlyH 路径判过，
                //   FlyFanBr 接管呼出后一直没接这根线，所以这个开关在游戏里是死的。
                if (com.oupao.wiz.Hooks.gameBlock()) {
                    com.oupao.wiz.SLog.i("FlyFanBr: 游戏模式 不呼出");
                    return;
                }
                // 3200：多任务（概览）界面里不呼出扇形菜单。
                //   `oupao_recents_visible` 由 FlyGate 在桌面 onStateSetEnd 时写进 Settings.Global，
                //   所以进/出概览都会刷新。只拦方向判定之前，热区点击重放那条不受影响。
                if (com.oupao.wiz.Sug.recentsOpen()) {
                    com.oupao.wiz.SLog.i("FlyFanBr: 多任务界面 不呼出");
                    return;
                }
                // 3501：输入法打开期间不呼出扇形菜单（用户 2026-11-17 要求）。
                //   键盘弹出时从角落上滑，FlyZone 会 pilfer 抢走输入法的指针流
                //   （logcat 实测 input_cancel: [InputMethod, reason=input channel stole pointer stream]），
                //   菜单还会盖在键盘上。这里在 DOWN 就退出，side 保持 0，MOVE/UP 也不会再建菜单。
                if (com.oupao.wiz.Hooks.imeShown()) {
                    com.oupao.wiz.SLog.r("r-fanbr-ime", "FlyFanBr: 输入法打开 不呼出", 1000L);
                    return;
                }
                int sd = com.oupao.wiz.FlyGeom.side(x, y, (float) dm.widthPixels, (float) dm.heightPixels, (float) r, lr);
                // ★ 2795：只在"菜单停留中 + 这次按下**不在角落热区**"时才算上滑手势。
                //   之前漏了这个条件，于是"在菜单停留状态下再划一次选应用"（向上划）会被当成上滑手势
                //   → 菜单被收起、什么都没启动（用户报的"划到应用松手不打开、还要再点一下"就是这个）。
                upTrack = com.oupao.wiz.FlyFanH.staying() && (sd == com.oupao.wiz.FlyGeom.SIDE_NONE);
                if (upTrack) {
                    upDownX = x;
                    upDownY = y;
                }
                if (sd == com.oupao.wiz.FlyGeom.SIDE_NONE) {
                    return;
                }
                // 2774：热区窗口没建/没挂上就别呼出 —— 否则没人消费这条手势，必然穿透。
                if (!zoneLive(sd)) {
                    return;
                }
                side = sd;
                downX = x;
                downY = y;
                // ★ 2774：**DOWN 时不再呼出菜单**（原来一按下就弹，所以"点一下角落也弹菜单"），
                //   只记下起点；等手指真的划出"角落手势"（FlyGeom.claimed 的两轴阈值）才呼出。
            } else if (a == 2) {
                if (side == 0) {
                    return;
                }
                // 3501：手势进行中键盘弹起 → 立刻放弃这次呼出，别等 FlyFanH.show 再兜底。
                if (com.oupao.wiz.Hooks.imeShown()) {
                    com.oupao.wiz.SLog.r("r-fanbr-ime", "FlyFanBr: 输入法打开 取消手势", 1000L);
                    reset();
                    return;
                }
                if (!com.oupao.wiz.FlyFanH.alive()) {
                    // 纯点击/手抖过不了这两轴阈值 → 永远不弹菜单（点击交给 FlyZone 重放）
                    float slop = 8.0f * dm.density;
                    try {
                        slop = (float) android.view.ViewConfiguration.get(c).getScaledTouchSlop();
                    } catch (Throwable ignore) {
                    }
                    if (!com.oupao.wiz.FlyGeom.claimed(side, downX, downY, x, y, slop, dm.density)) {
                        return;
                    }
                    moved = true;
                    gestured = true;
                    upTrack = false;      // 2795：这次手势归菜单了，不再当上滑手势处理
                    com.oupao.wiz.FlyState.setOpen(true);
                    com.oupao.wiz.FlyFanH.show(c, side, x, y);   // 3433：带上呼出瞬间的手指位置（作为动画 0 点）
                    com.oupao.wiz.FlyFanH.move(x, y, false);
                    // 2798：呼出时振动**恢复**（2793 误删；用户要的是"松手不振动"，呼出要有）
                    com.oupao.wiz.FlyH.buzz();
                    lastSel = -1;
                }
                // ★ 2797：moved 必须按手指真的动了来置位，不能只靠新建菜单那条路径。
                //   菜单停留（未选应用时松手收起=关）后再划一次时，菜单已经存在 → 跳过新建分支
                //   → moved 一直 false → 松手只停留、不启动（用户报的划到应用松手不打开）。
                if (!moved) {
                    float mdx = x - downX;
                    float mdy = y - downY;
                    if (((mdx * mdx) + (mdy * mdy)) > 400.0f) {
                        moved = true;
                    }
                }
                lastP = com.oupao.wiz.FlyFanH.progressOf(x, y);
                com.oupao.wiz.FlyFanH.move(x, y, lastP >= 0.999f);
                int cur = com.oupao.wiz.FlyFanH.sel();
                if (cur != lastSel) {
                    lastSel = cur;
                    if (cur >= 0) {
                        // 2793：这条**保留** —— 划到某个应用时给一次触感反馈
                        com.oupao.wiz.FlyH.buzz();
                    }
                }
            } else if (a == 1) {
                com.oupao.wiz.SLog.r("r-fanbr-up", "FlyFanBr: UP moved=" + moved + " lastP=" + lastP
                        + " sel=" + com.oupao.wiz.FlyFanH.sel(), 2000L);   // 2795 诊断
                if (com.oupao.wiz.FlyFanH.alive()) {
                    if (moved && (lastP >= 0.999f) && (com.oupao.wiz.FlyFanH.sel() >= 0)) {
                        // 2793：松手**不振动**（用户：松手后不需要振动）
                        com.oupao.wiz.FlyFanH.release();
                    } else if (com.oupao.wiz.FlyFanH.staying()) {
                        // 3438：菜单**已经停住了**，这一下 UP 只是"点了一下"——
                        //   再调 stay() 会把 v.sel 重置成 -1，正好把 tap() 刚选中的图标冲掉
                        //   （用户："菜单展开后点击图标没法打开"）。什么都不做。
                    } else {
                        // ★ 2776：「未选应用时松手收起」终于生效
                        //   开(flyStayOff=1) → 直接消失（原来一直这样，所以这个开关看着"有用"）
                        //   关(默认)        → 菜单停留等选择（原来也是直接消失，所以开关等于没用）
                        boolean stayOff = false;
                        try {
                            stayOff = com.oupao.wiz.Cfg.flyStayOff();
                        } catch (Throwable ignore) {
                        }
                        if (stayOff) {
                            com.oupao.wiz.FlyFanH.hide();
                        } else {
                            com.oupao.wiz.FlyFanH.stay();
                        }
                    }
                    com.oupao.wiz.FlyState.setOpen(false);
                }
                reset();
            } else if (a == 3) {
                upTrack = false;
                com.oupao.wiz.FlyFanH.hide();
                com.oupao.wiz.FlyState.setOpen(false);
                reset();
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyFanBr: 异常", t);
            reset();
        }
    }

    /** 那个角落的热区窗口（消费型）现在是否真的挂上了 —— 没挂就绝不出菜单 */
    private static boolean zoneLive(int sd) {
        try {
            android.view.View v = (sd == com.oupao.wiz.FlyGeom.SIDE_RIGHT)
                    ? com.oupao.wiz.FlyS.zone2 : com.oupao.wiz.FlyS.zone;
            return (v != null) && v.isAttachedToWindow();
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static void reset() {
        side = 0;
        moved = false;
        lastP = 0.0f;
        lastSel = -1;
    }
}
