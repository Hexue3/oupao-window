package com.oupao.wiz;

/**
 * 全局字母引索（侧边呼出）。
 *
 * 思路：边缘出现一条全局引索条（{@link IdxBar}），划到哪个字母就在旁边显示该字母的 app。
 * 它已经有应用网格、拼音字母分区、右侧索引条、已添加/应用/功能分区。
 *
 * 触发方式：从屏幕左/右边缘**竖着**滑一下（竖滑不会和系统边缘返回手势打架，
 * 横滑留给系统返回）。触发点必须落在屏幕竖直方向的中间 60% 区域内，
 * 避免和上/下拉状态栏、下边缘手势冲突。
 */
public final class Idx {

    private static final int NONE = 0;
    private static final int CAND = 1;
    private static final int FIRED = 2;

    private static int st = NONE;
    private static long lastDownAt;
    private static long lastEventAt;
    private static float lastDownX;
    private static float lastDownY;
    private static boolean started;
    private static long masterAt;
    private static boolean masterVal;
    private static int cw;
    private static int ch;
    private static long sizeAt;
    private static boolean rightVal;
    private static boolean leftVal;
    private static int side = 1;
    private static float x0;
    private static float y0;
    /**
     * 本次手势有没有真的呼出过引索条。
     *
     * 感应条（{@link IdxBand}）和全局旁听（{@link FlyInput}）**两路都会喂** {link #feed}，
     * 而 UP 一到就把 st 清成 NONE，所以感应条那边必须在 feed(1) 之前就知道
     * 「这次手势算不算引索手势」——用这个标记，不能用 st。
     */
    private static boolean firedThisGesture;
    /** MOVE 去重：两路喂进来的同一条事件只处理一次 */
    private static float lastMoveX = Float.NaN;
    private static float lastMoveY = Float.NaN;
    private static long lastMoveAt;
    // ---- dp 缓存：以前每个事件都要 Ui.dp() 六七次（每次过一遍 Resources），
    //      拖动时这些全是热路径开销。屏幕尺寸变了才重算。 ----
    private static int dpBand;     // 18dp：边缘判定宽度
    private static int dpTol;      // 4dp：条的上下容差
    private static int dpBarW;     // 16dp：条宽
    private static int dpBarW2;    // 14dp：条内额外容差
    private static int dpAct;      // 8dp：竖滑触发阈值
    private static int dpHoriz;    // 12dp：横滑判定
    /** 横屏标记（1s 刷新一次，避免每个事件都去问 Display.getRotation） */
    private static boolean landscape;
    /** 配置变化监听是否已注册（旋转时立刻撤掉感应条，不用等定时 sync） */
    private static boolean ccReg;

    private Idx() {
    }

    /**
     * 当前是不是横屏。
     *
     * 先看 `Display.getRotation()`（1/3 = 横屏）—— 这是最可靠的；
     * 系统上下文的 `DisplayMetrics` 在某些版本里**不随旋转变化**（一直报竖屏尺寸），
     * 只靠 `width>height` 会漏判。两条都查一遍。
     */
    static boolean isLandscape(android.content.Context c) {
        try {
            if (c == null) {
                return false;
            }
            var wm = (android.view.WindowManager) c.getSystemService("window");
            if (wm != null) {
                var d = wm.getDefaultDisplay();
                if (d != null) {
                    int r = d.getRotation();
                    if ((r == 1) || (r == 3)) {
                        return true;
                    }
                    var m = new android.util.DisplayMetrics();
                    d.getRealMetrics(m);
                    if (m.widthPixels > m.heightPixels) {
                        return true;
                    }
                    if (r == 0 || r == 2) {
                        return false;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    /** 本次手势是否已经呼出过引索条（供 {@link IdxBand} 判断要不要透传点击） */
    static boolean firedThisGesture() {
        return firedThisGesture;
    }

    /** 由 FlyInput 的全局触摸流驱动：action 0=DOWN 1=UP 2=MOVE 3=CANCEL */
    static void feed(int action, float x, float y) {
        feed(action, x, y, 0);
    }

    /** sideHint：感应条传入自己的侧（1=右 -1=左 0=未知） */
    static void feed(int action, float x, float y, int sideHint) {
        long __t0 = android.os.SystemClock.uptimeMillis();
        lastEventAt = __t0;
        try {
            // 感应条和全局旁听两路都会喂 MOVE，同一条事件只处理一次
            if (action == 2) {
                if ((x == lastMoveX) && (y == lastMoveY) && ((__t0 - lastMoveAt) < 8L)) {
                    return;
                }
                lastMoveX = x;
                lastMoveY = y;
                lastMoveAt = __t0;
            }
            long nowMs = android.os.SystemClock.uptimeMillis();
            // 配置/尺寸缓存 1s：拖拽时每个事件都读文件会很卡
            if ((nowMs - masterAt) > 1000L) {
                masterAt = nowMs;
                masterVal = com.oupao.wiz.Cfg.idxMaster();
                rightVal = com.oupao.wiz.Cfg.idxRight();
                leftVal = com.oupao.wiz.Cfg.idxLeft();
                var c0 = com.oupao.wiz.Cfg.sysContext();
                if (c0 != null) {
                    var d0 = c0.getResources().getDisplayMetrics();
                    cw = d0.widthPixels;
                    ch = d0.heightPixels;
                    dpBand = com.oupao.wiz.Ui.dp(c0, 18);
                    dpTol = com.oupao.wiz.Ui.dp(c0, 4);
                    dpBarW = com.oupao.wiz.Ui.dp(c0, 16);
                    dpBarW2 = com.oupao.wiz.Ui.dp(c0, 14);
                    dpAct = com.oupao.wiz.Ui.dp(c0, 8);
                    dpHoriz = com.oupao.wiz.Ui.dp(c0, 12);
                    landscape = isLandscape(c0);
                }
            }
            if (!masterVal) {
                if (st != NONE) {
                    reset();
                }
                st = NONE;
                return;
            }
            var ctx = com.oupao.wiz.Cfg.sysContext();
            if (ctx == null) {
                st = NONE;
                return;
            }
            int w = cw;
            int h = ch;
            if ((w <= 0) || (h <= 0)) {
                st = NONE;
                return;
            }
            // 横屏不启用（感应条那边也会被移除；这里再兜一道，
            // 防止旋转瞬间还有残留事件把引索条拉出来）
            if (landscape || (w > h)) {
                st = NONE;
                return;
            }
            int band = dpBand;
            if (action == 0) {
                long now = android.os.SystemClock.uptimeMillis();
                if ((now - lastDownAt) < 60L && (Math.abs(x - lastDownX) < 4f)
                        && (Math.abs(y - lastDownY) < 4f)) {
                    return;   // 同一个 DOWN 被两路喂进来了
                }
                lastDownAt = now;
                lastDownX = x;
                lastDownY = y;
                st = NONE;
                firedThisGesture = false;
                // 扇形菜单正在用时不要抢手势
                if (com.oupao.wiz.FlyH.st != 0) {
                    return;
                }
                boolean r = rightVal && (x >= (w - band));
                boolean l = leftVal && (x <= band);
                if ((!r) && (!l)) {
                    return;
                }
                // 只认引索条自己的矩形（高 34%、下移 12%），手指出去就不算
                int bh = ((h * 42) / 100);   // 与 IdxBar.BAR_PERCENT 保持一致
                int bt = (((h - bh) / 2) + ((h * 12) / 100));
                int tol = ((h * 3) / 100) + dpTol;
                if ((y < (bt - tol)) || (y > (bt + bh + tol))) {
                    return;
                }
                st = CAND;
                side = (sideHint != 0) ? sideHint : (r ? 1 : -1);
                x0 = x;
                y0 = y;
                return;
            }
            if (action == 1) {
                st = NONE;
                com.oupao.wiz.IdxBar.release();
                return;
            }
            if (action == 3) {
                st = NONE;
                com.oupao.wiz.IdxBar.hide();
                return;
            }
            // 已经呼出：贴边上下滑选字母；往内滑进入网格选 app
            if (st == FIRED) {
                int barW = dpBarW;
                float inward = (side > 0) ? (w - x) : x;
                // 手指还在条内（宽 16dp + 6dp 容差）才跟字母；
                // 离开条只认「真的在图标上」的悬停，不再改字母
                if (inward <= (barW + dpBarW2)) {
                    com.oupao.wiz.IdxBar.setByY((int) y);
                } else {
                    com.oupao.wiz.IdxBar.hover((int) x, (int) y);
                }
                return;
            }
            if (st != CAND) {
                return;
            }
            float dy = (y - y0);
            float dx = ((x - x0) * side);
            int act = dpAct;
            // 一旦判为横滑（返回手势），本次手势就永远不再参与
            // 否则用户在返回手势中把手指往上/下带一点，就会误触发引索条
            float adx = Math.abs(dx);
            float ady = Math.abs(dy);
            int horiz = dpHoriz;
            if ((adx > horiz) && (adx >= ady)) {
                st = NONE;
                return;
            }
            // MOVE：竖滑超过阈值、且明显比横移更"竖"才算触发
            if ((Math.abs(dy) >= act) && (Math.abs(dy) > (Math.abs(dx) * 1.5f))) {
                st = FIRED;
                firedThisGesture = true;
                if (!com.oupao.wiz.IdxBar.isShown()) {
                    open(ctx);
                }
                // 刚呼出就先选中手指当前位置对应的字母
                com.oupao.wiz.IdxBar.setByY((int) y);
            }
        } catch (Throwable t) {
            st = NONE;
        }
        long __dt = android.os.SystemClock.uptimeMillis() - __t0;
        // 阈值放大到 40ms：以前 6ms 就记一条，而 write 是**写文件**——
        // 拖动时每条 MOVE 都写一次盘，本身就是卡顿的来源。
        if (__dt >= 40L) {
            com.oupao.wiz.SLog.i("Idx: feed slow " + __dt + "ms a=" + action + " st=" + st);
        }
    }

    private static void open(android.content.Context ctx) {
        try {
            if (isLandscape(ctx)) {
                // 横屏不启用（第三道保险）
                return;
            }
            if (com.oupao.wiz.Cfg.idxGameBlock() && com.oupao.wiz.Sug.gameOn) {
                com.oupao.wiz.SLog.i("Idx: \u6e38\u620f\u6a21\u5f0f \u4e0d\u547c\u51fa");
                return;
            }
            // ★ 2817：最近任务（多任务）界面不呼出
            if (com.oupao.wiz.Hooks.inRecentsWindow()) {
                com.oupao.wiz.SLog.i("Idx: \u6700\u8fd1\u4efb\u52a1\u754c\u9762 \u4e0d\u547c\u51fa");
                return;
            }
            if ((!com.oupao.wiz.Cfg.idxGlobal()) && (!isLauncher())) {
                com.oupao.wiz.SLog.i("Idx: \u4ec5\u684c\u9762\u751f\u6548 \u8df3\u8fc7");
                return;
            }
            if (com.oupao.wiz.Cfg.idxVib()) {
                com.oupao.wiz.FlyH.buzz();
            }
            com.oupao.wiz.SLog.i("Idx: \u547c\u51fa\u5b57\u6bcd\u5f15\u7d22");
            com.oupao.wiz.IdxBar.show(ctx, side);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Idx: \u547c\u51fa\u5931\u8d25", t);
        }
    }

    /** 彻底复位：状态清零 + 收起浮层（开关被关、感应条被删、手势中断都走这里） */
    static void reset() {
        try {
            st = NONE;
            firedThisGesture = false;
            com.oupao.wiz.IdxBar.hide();
        } catch (Throwable ignore) {
        }
    }

    /** 2822：后台线程跑的重活（应用列表预热 / 最近应用 / 分身扫描），忙则跳过 */
    private static volatile boolean heavyBusy;
    /** 本次 heavyTick 开始的时间（uptimeMillis）：用来发现 heavyBusy 被卡住 */
    private static volatile long heavyAt;
    private static Thread heavyThread;

    /** 重活超过这么久还「忙」就认为卡住了（正常一轮 <200ms，慢的也就 1~2s） */
    private static final long HEAVY_STUCK_MS = 15000L;

    static void heavyTick(final android.content.Context c)
    {
        // 2975：灭屏/锁屏期间不做重活（预热/扫描/刷新最近应用）
        if (com.oupao.wiz.Cfg.sleepingNow()) {
            return;
        }
        // ★ 2949：heavyBusy 一旦被卡住（线程异常结束/某一步阻塞后没走到复位），
        //   预热、空闲刷新、分身扫描、「最近应用」刷新就**永久停摆**且一声不响
        //   —— 2949 装完实测就撞上过：开机后只有分身扫描（它自己有 25s 自转链）还在跑，
        //   IDxBar 的 idle refresh / 最近应用刷新一次都没有。
        //   这里加「超时复位」：卡住 >15s 就强制放开下一拍，并留一条限流日志。
        if (heavyBusy && ((android.os.SystemClock.uptimeMillis() - heavyAt) > HEAVY_STUCK_MS)) {
            com.oupao.wiz.SLog.r("IdxHeavyStuck", "Idx: heavyTick \u5361\u4f4f>15s\u2192\u590d\u4f4d", 5000L);
            heavyBusy = false;
        }
        if (heavyBusy) {
            return;
        }
        heavyBusy = true;
        heavyAt = android.os.SystemClock.uptimeMillis();
        try {
            if ((heavyThread == null) || (!heavyThread.isAlive())) {
                heavyThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        } catch (Throwable ignore) {
                        }
                        // 四件事各自 try：哪一步坏了要能看出来，且不连累后面的步骤
                        try {
                            com.oupao.wiz.IdxBar.prewarm(c);
                        } catch (Throwable t) {
                            com.oupao.wiz.SLog.x("IdxHeavyPrewarm", t);
                        }
                        try {
                            com.oupao.wiz.IdxBar.refreshIfIdle(c);
                        } catch (Throwable t) {
                            com.oupao.wiz.SLog.x("IdxHeavyIdle", t);
                        }
                        try {
                            com.oupao.wiz.FlyH.cloneTick();
                        } catch (Throwable t) {
                            com.oupao.wiz.SLog.x("IdxHeavyClone", t);
                        }
                        try {
                            com.oupao.wiz.Tools.refreshRecents(c);
                        } catch (Throwable t) {
                            com.oupao.wiz.SLog.x("IdxHeavyRecents", t);
                        }
                        heavyBusy = false;
                    }
                }, "Oupao-IdxHeavy");
                heavyThread.setDaemon(true);
                heavyThread.start();
            } else {
                heavyBusy = false;
            }
        } catch (Throwable t) {
            heavyBusy = false;
        }
    }

    /** 模块装载时调用：按配置同步边缘感应条 */
    public static void start() {
        if (started) {
            return;
        }
        started = true;
        try {
            // 旋转/分辨率变化：立刻同步感应条（竖屏→横屏要马上撤掉，
            // 否则那 1.5s 的定时同步期间它还挂在那儿吃触摸）
            var cNow = com.oupao.wiz.Cfg.sysContext();
            if ((cNow != null) && (!ccReg)) {
                ccReg = true;
                try {
                    cNow.registerComponentCallbacks(new android.content.ComponentCallbacks() {
                        public void onConfigurationChanged(android.content.res.Configuration cfg) {
                            try {
                                com.oupao.wiz.IdxBand.sync(com.oupao.wiz.Cfg.sysContext());
                            } catch (Throwable ignore) {
                            }
                        }

                        public void onLowMemory() {
                        }
                    });
                } catch (Throwable ignore) {
                }
            }
            final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.postDelayed(new Runnable() {
                public void run() {
                    var c = com.oupao.wiz.Cfg.sysContext();
                    // ★ 2949：每一步各自 try —— 以前三件事包在一个大 try 里，
                    //   IdxBand.sync 一抛异常就把后面的 heavyTick（预热/空闲刷新/分身扫描/最近应用）
                    //   全部连带跳过，而且静默；实测开机后出现过「一次都不再刷新」。
                    try {
                        com.oupao.wiz.IdxBand.sync(c);
                    } catch (Throwable t) {
                        com.oupao.wiz.SLog.x("IdxBandSync", t);
                    }
                    try {
                        // ★ 2822：预热/刷新要加载上百个应用 + 分组，实测在主线程上造成
                        //   `Slow dispatch took 122ms`（用户反馈卡顿），全部丢到后台线程。
                        com.oupao.wiz.Idx.heavyTick(c);
                    } catch (Throwable t) {
                        com.oupao.wiz.SLog.x("IdxHeavyTick", t);
                    }
                    try {
                        // 看门狗：手势进行中却已 1.2s 没事件（感应条被删/开关被关/中断）→ 收起
                        if ((st != NONE) && ((android.os.SystemClock.uptimeMillis() - lastEventAt) > 1200L)) {
                            com.oupao.wiz.SLog.i("Idx: watchdog reset");
                            reset();
                        }
                        // 顺带驱动分身扫描（内部有 60s 节流，很便宜）。
                        // cloneTick 以前因为 cloneAt 初始为 0 的写法成了死代码，
                        // 没人调用就永远不扫 → 「更多应用」里没有分身应用。
                        // 2822：分身扫描 / 最近应用刷新同样走后台（见 heavyTick）
                    } catch (Throwable t) {
                        com.oupao.wiz.SLog.x("IdxWatchdog", t);
                    }
                    // 「仅在桌面」时要更快地切换感应条，否则切到 app 会残留死区。
                    // 这一句以前在 try 外面：Cfg 读失败（开机早期很常见）就 postDelayed 落空，
                    // 整个定时循环就此永久停摆 → 这里也包进 try，默认 1.5s 继续跑。
                    long next = 1500L;
                    try {
                        next = (!com.oupao.wiz.Cfg.idxGlobal()) ? 700L : 1500L;
                    } catch (Throwable t) {
                        com.oupao.wiz.SLog.x("IdxNext", t);
                    }
                    h.postDelayed(this, next);
                }
            }, 2500L);
            com.oupao.wiz.SLog.i("Idx: start build=" + com.oupao.wiz.BuildConfig.VERSION_NAME + " 感应条同步已启动");
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Idx: start 失败", t);
        }
    }

    private static final java.util.concurrent.ExecutorService EX =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    /** 震动丢到后台线程：避免 VibratorService 的 IPC 阻塞手势处理 */
    static void buzzAsync(final int level) {
        try {
            EX.execute(new Runnable() {
                public void run() {
                    try {
                        var c = com.oupao.wiz.Cfg.sysContext();
                        if (c == null) {
                            return;
                        }
                        var v = (android.os.Vibrator) c.getSystemService("vibrator");
                        if (v == null) {
                            return;
                        }
                        // 换字母的反馈很轻：14ms 一下，不占主线程、不拖慢手势
                        // 3275：振幅跟随系统触感强度
                        int amp = com.oupao.wiz.Cfg.sysHapticAmp();
                        v.vibrate(android.os.VibrationEffect.createOneShot(14L, amp));
                    } catch (Throwable ignore) {
                    }
                }
            });
        } catch (Throwable ignore) {
        }
    }

    /** 启动应用（app = {tag, label}） */
    static void launch(Object[] app) {
        try {
            if ((app == null) || (app[0] == null)) {
                return;
            }
            var tag = String.valueOf(app[0]);
            com.oupao.wiz.SLog.i("Idx: 启动 " + tag);
            com.oupao.wiz.FlyH.launch(tag);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Idx: 启动失败", t);
        }
    }

    /**
     * 前台是不是桌面（取不到就当作不在桌面 —— 宁可不生效，不能误生效）。
     *
     * ⚠️ 这个方法由 {@link IdxBand#sync} 每 700ms（「仅桌面」开启时）调一次，
     * 所以**绝不能在这里打日志**：非桌面时每秒 1.4 条，半小时就把
     * /data/system/oupao.log 刷掉上千行，还会盖掉真正有用的日志。
     * 需要诊断时，临时在这里加一条 {@code SLog.v}（记得用完删）。
     */
    static boolean isLauncher() {
        try {
            var pkg = com.oupao.wiz.Sug.fgPkg();
            if ((pkg == null) || pkg.isEmpty()) {
                return false;
            }
            return pkg.toLowerCase().contains("launcher")
                    || pkg.contains("com.android.internal.app");
        } catch (Throwable t) {
            return false;
        }
    }

    /** 诊断用：返回当前前台包名与判定结果，调用方自己决定要不要打日志 */
    }
