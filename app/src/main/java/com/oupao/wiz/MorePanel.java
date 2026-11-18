package com.oupao.wiz;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.BitmapShader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 「更多应用」悬浮操作区（完整重写）
 * - 整页滚动：已添加 / 应用-工具切换 / 应用按拼音 A-Z 分组（# 置底）
 * - 管理模式：右上角 管理/完成；每个应用/工具显示 +（绿）/ −（红），点击加入/移出固定
 * - 图标：后台线程渲染（圆形 + 分身角标），主线程零位图计算；缓存 tag@size
 * - 索引条：★ + A-Z + #，垂直居中，整列可拖动，每换一个字母振动一次
 * - 动画：呼出只做中心缩放(0.85→1)，遮罩全程不透明；关闭缩放+淡出后再移除窗口
 */
public final class MorePanel {

    private static View rootView;
    private static WindowManager wm;
    private static FrameLayout panelRef;
    private static LinearLayout innerRef;
    /** 本次面板的宽度（refresh 重建内容时要用同一个值，否则横屏会串成竖屏尺寸） */
    private static int panelW;
    /**
     * 面板内容的整体缩放系数（1 = 竖屏原样）。
     *
     * 横屏时面板等比缩小了，**内容也得跟着缩**：原来只改了面板尺寸，
     * 图标还是 30dp、字母条还是每个 13dp → 图标显大、字母条被挤出面板
     * （用户反馈「图标和引索条没等比缩小、引索条显示不完全」）。
     */
    private static float panelScale = 1f;
    /** 本次面板的高度（字母条按可用高度自适应压缩要用） */
    private static int panelH;
    private static int tab = 0;
    private static ArrayList<String> pinState = new ArrayList<String>();
    /** 已添加固定应用的网格（拖拽时就地挪动用） */
    private static GridLayout pinGrid;
    /** 拖拽排序的待处理结果：ACTION_DROP 记录，ACTION_DRAG_ENDED 才执行（避免崩系统） */
    private static String pendFrom;
    private static String pendTo;
    private static boolean pinLoaded = false;

    private static final HashMap<String, Bitmap> iconCache = new HashMap<String, Bitmap>();
    private static final ArrayList<Object[]> iconJobs = new ArrayList<Object[]>();

    /** 当前面板的滚动容器（refresh 时用它保存/恢复滚动位置） */
    private static ScrollView svRef;
    /** 滚动内容层（jumpTo 需要它作为参照） */
    private static LinearLayout contentRef;
    /** 索引条上的字母与对应锚点视图（供边缘滑动驱动跳转） */
    static final java.util.ArrayList<String> idxLetters = new java.util.ArrayList<String>();
    static final java.util.ArrayList<View> idxTargets = new java.util.ArrayList<View>();

    private MorePanel() {
    }

    // ---------------- 工具方法 ----------------

    private static String labelOf(String pin) {
        try {
            int i = pin.indexOf('\u0001');
            if (i > 0) {
                return pin.substring(0, i);
            }
        } catch (Throwable ignore) {
        }
        return pin;
    }

    private static String tagOf(String pin) {
        try {
            int i = pin.lastIndexOf('\u0001');
            if (i >= 0) {
                return pin.substring(i + 1);
            }
        } catch (Throwable ignore) {
        }
        return pin;
    }

    private static String pkgOf(String tag) {
        int h = tag.indexOf('#');
        return (h > 0) ? tag.substring(0, h) : tag;
    }

    private static int uidOf(String tag) {
        try {
            String[] seg = tag.split("#");
            if (seg.length >= 3) {
                return Integer.parseInt(seg[2]);
            }
        } catch (Throwable ignore) {
        }
        return 0;
    }

    /** 取某个包的桌面启动 Activity（ACTION_MAIN + CATEGORY_LAUNCHER），取不到返回 null */
    private static String launchClsOf(Context ctx, String pkg) {
        try {
            var pm = ctx.getPackageManager();
            var it = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            it.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            it.setPackage(pkg);
            var list = pm.queryIntentActivities(it, 0);
            if ((list != null) && (list.size() > 0)) {
                var ri = (android.content.pm.ResolveInfo) list.get(0);
                if ((ri != null) && (ri.activityInfo != null)) {
                    return ri.activityInfo.name;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String appLabel(Context ctx, String pkg) {
        try {
            var pm = ctx.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (Throwable ignore) {
        }
        return pkg;
    }

    private static String letterOf(String label) {
        try {
            String fl = PinUI.firstLetter(label);
            if ((fl != null) && (fl.length() > 0)) {
                return fl.toUpperCase();
            }
        } catch (Throwable ignore) {
        }
        return "#";
    }

    private static Bitmap circleOf(Drawable d, int size) {
        try {
            if ((d == null) || (size <= 0)) {
                return null;
            }
            var src = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            var c0 = new Canvas(src);
            d.setBounds(0, 0, size, size);
            d.draw(c0);
            var out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            var c = new Canvas(out);
            var p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            float r = (((float) size) / 2.0f);
            c.drawCircle(r, r, r, p);
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---------------- 显示 / 关闭 ----------------

    /** true = 本次面板操作的是「索引条自己的固定应用」(oupao_idx_pins)，false = 面板那套 */
    static boolean forIdx;
    private static boolean pendingIdx;

    /** 供 IdxBar ★ 长按调用：打开「索引条版」面板（外观/交互与更多应用面板一致） */
    public static void showIdx() {
        pendingIdx = true;
        show();
    }

    /** 当前面板要读写的固定应用原始串 */
    private static String curPins() {
        return forIdx ? com.oupao.wiz.Cfg.idxPins() : com.oupao.wiz.Cfg.flyPins();
    }

    /** 持久化到对应的那套（索引条 → oupao_idx_pins；面板 → oupao_fly_pins） */
    private static void persist(String next) {
        // 固定应用一变，索引条的 ★ 分组和字母排除都要立刻重建，别等 60s 过期
        try {
            com.oupao.wiz.IdxBar.invalidate();
        } catch (Throwable ignore) {
        }
        if (forIdx) {
            com.oupao.wiz.Cfg.setIdxPins(next);
            try {
                // 空列表要带哨兵值，否则接收端的空串保护会把它丢掉
                String send = next.isEmpty() ? com.oupao.wiz.Cfg.PINS_EMPTY : next;
                com.oupao.wiz.Ev.send(com.oupao.wiz.Ev.it("com.oupao.wiz.CFG")
                        .putExtra("oupao_idx_pins", send));
            } catch (Throwable ignore) {
            }
            return;
        }
        try {
            // 3151：面板模式**直接**写 fly_pins，不再经过 gset + Pins.save()。
            // Pins.save() 会看 Pins.deskMode 决定写 desk 还是 panel —— 在面板里那个标志
            // 可能是上一个模式留下的 true，于是面板的增删被写进「小窗模式」那套
            // （用户现象：小窗/引索条都好，只有面板 +/- 不生效）。
            com.oupao.wiz.Cfg.writePinsKey("oupao_fly_pins", next);
        } catch (Throwable t) {
            SLog.e("MorePanel: 写 fly_pins 失败", t);
        }
    }

    /**
     * 面板尺寸 + 是否横屏。
     *
     * 竖屏：沿用 78%宽 × 58%高。
     * 横屏：**等比缩小** —— 按竖屏那版的宽高比（78%短边 : 58%长边）取值，
     * 高度占横屏高度的 88%，宽度跟着比例缩，不再把整个横屏铺满
     * （用户反馈：横屏时面板横跨整屏，很难看）。
     *
     * ⚠️ 尺寸用 `getRealMetrics` 拿：系统上下文里的 `DisplayMetrics`
     * 在某些版本不随旋转变化（和 `Idx.isLandscape` 是同一个坑）。
     */
    private static int[] calcSize(android.content.Context ctx) {
        var dm = ctx.getResources().getDisplayMetrics();
        int sw = dm.widthPixels;
        int sh = dm.heightPixels;
        try {
            var w2 = (WindowManager) ctx.getSystemService("window");
            var d = (w2 == null) ? null : w2.getDefaultDisplay();
            if (d != null) {
                var real = new android.util.DisplayMetrics();
                d.getRealMetrics(real);
                if ((real.widthPixels > 0) && (real.heightPixels > 0)) {
                    sw = real.widthPixels;
                    sh = real.heightPixels;
                }
            }
        } catch (Throwable ignore) {
        }
        int pw;
        int ph;
        int scale100;
        boolean land = (sw > sh);
        if (land) {
            int portW = ((sh * 78) / 100);     // 横屏的短边 = 竖屏的宽
            int portH = ((sw * 58) / 100);     // 横屏的长边 = 竖屏的高
            ph = ((sh * 88) / 100);
            pw = (int) ((((float) ph) * portW) / Math.max(1, portH));
            int cap = ((sh * 92) / 100);       // 别超过短边的 92%
            if (pw > cap) {
                pw = cap;
            }
            // 内容缩放：与竖屏那版的面板宽度（78% × 短边）之比
            scale100 = Math.max(40, (pw * 100) / Math.max(1, portW));
        } else {
            pw = ((sw * 78) / 100);
            ph = ((sh * 58) / 100);
            scale100 = 100;
        }
        return new int[]{pw, ph, land ? 1 : 0, scale100};
    }

    /**
     * 造一个「密度被缩放」的 Context：之后所有 `Ui.dp()` / `setTextSize()` 都自动等比缩放。
     *
     * 为什么用这个而不是在调用点乘系数：面板里的 dp/sp 有几十处（标题、管理按钮、
     * 分区标题、Tab、应用名、间距、圆角…），逐个乘既容易漏又容易重复缩放
     * （2627 就只缩了图标和字母条，标题/按钮/文字全是原尺寸）。
     * 改密度是一处改动、全内容生效。
     */
    private static android.content.Context scaleCtx(android.content.Context ctx, float scale) {
        try {
            if ((ctx == null) || (scale <= 0.01f) || (Math.abs(scale - 1.0f) < 0.01f)) {
                return ctx;
            }
            var cfg = new android.content.res.Configuration(ctx.getResources().getConfiguration());
            int dpi = cfg.densityDpi;
            if (dpi <= 0) {
                dpi = ctx.getResources().getDisplayMetrics().densityDpi;
            }
            cfg.densityDpi = Math.max(72, ((int) (dpi * scale)));
            var c2 = ctx.createConfigurationContext(cfg);
            if (c2 != null) {
                SLog.i("MorePanel: 内容缩放 dpi " + dpi + " -> " + cfg.densityDpi);
                return c2;
            }
        } catch (Throwable t) {
            SLog.e("MorePanel: 缩放 Context 失败", t);
        }
        return ctx;
    }

    public static void show() {
        forIdx = pendingIdx;
        pendingIdx = false;
        try {
            if (rootView != null) {
                hide();
            }
            var ctx = Cfg.sysContext();
            if (ctx == null) {
                return;
            }
            wm = (WindowManager) ctx.getSystemService("window");
            if (wm == null) {
                return;
            }
            var dm = ctx.getResources().getDisplayMetrics();
            var sz = calcSize(ctx);
            final int pw = sz[0];
            final int ph = sz[1];
            final boolean land = (sz[2] == 1);
            panelW = pw;
            panelH = ph;
            panelScale = Math.max(0.4f, sz[3] / 100f);
            // 横屏时要按「从哪一侧呼出」贴边：FlyH.side 1=左 2=右（0=未知，当右边）
            final boolean fromLeft = (com.oupao.wiz.FlyH.side == 1);

            try {
                Cfg.inv();          // 切到面板：清缓存，重新读最新配置
            } catch (Throwable ignore) {
            }
            // 顺手刷新一次「最近应用」（系统最近任务是最新的，打开面板就强制刷）
            try {
                com.oupao.wiz.Tools.refreshRecents(ctx, true);
            } catch (Throwable ignore) {
            }
            try {
                // 3152：`Pins.deskMode` 是个**静态**标志，小窗页 build 过之后就一直是 true，
                // 面板里再调 Pins.save() 就会把增删写进「小窗模式」那套（面板看起来没反应）。
                // 这里进面板时显式复位，避免同一进程内串数据。
                com.oupao.wiz.Pins.deskMode = false;
                if (pinOverride != null &&
                        android.os.SystemClock.uptimeMillis() - pinOverrideAt < 5000L) {
                    pinState = new ArrayList<String>(pinOverride);
                } else {
                    pinOverride = null;
                    pinState = new ArrayList<String>(Pins.parse(curPins()));
                }
                // 3149 诊断：面板 +/- 没反应时看这一行
                SLog.i("MorePanel: build appMgr=" + com.oupao.wiz.Pins.appMgr
                        + " forIdx=" + forIdx
                        + " mode=" + com.oupao.wiz.Cfg.moreMode()
                        + " pins=" + pinState.size()
                        + " rawLen=" + (curPins() == null ? -1 : curPins().length()));
            } catch (Throwable ignore) {
            }
            final long shownAt = android.os.SystemClock.uptimeMillis();
            var layer = new FrameLayout(ctx);
            layer.setBackgroundColor(0x66000000);
            layer.setAlpha(1.0f);
            layer.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent e) {
                    if (android.os.SystemClock.uptimeMillis() - shownAt < 350) {
                        return true;
                    }
                    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        MorePanel.hide();   // 点空白关闭
                    }
                    return true;
                }
            });

            var panel = new FrameLayout(ctx);
            var pbg = new GradientDrawable();
            pbg.setShape(GradientDrawable.RECTANGLE);
            pbg.setColor(Ui.cardBg());
            pbg.setCornerRadius(Ui.dp(ctx, 18));
            panel.setBackground(pbg);
            panel.setClickable(true);
            // 关键：初始缩放必须在 addView 之前，否则首帧是全尺寸 → 亮度跳变
            panel.setAlpha(1.0f);
            panel.setScaleX(0.85f);
            panel.setScaleY(0.85f);
            panelRef = panel;

            var inner = new LinearLayout(ctx);
            inner.setOrientation(LinearLayout.VERTICAL);
            innerRef = inner;
            panel.addView(inner, new FrameLayout.LayoutParams(-1, -1));

            buildContent(scaleCtx(ctx, panelScale), pw);

            var plp = new FrameLayout.LayoutParams(pw, ph);
            if (land) {
                // 横屏：贴着呼出那一侧（左边呼出→贴左，右边呼出→贴右），垂直居中
                plp.gravity = (fromLeft ? Gravity.START : Gravity.END) | Gravity.CENTER_VERTICAL;
                int m = Ui.dp(ctx, 16);
                if (fromLeft) {
                    plp.leftMargin = m;
                } else {
                    plp.rightMargin = m;
                }
            } else {
                plp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
                plp.topMargin = Ui.dp(ctx, 40);
            }
            layer.addView(panel, plp);

            var lp = new WindowManager.LayoutParams();
            lp.type = 2003;
            lp.format = -3;
            lp.width = -1;
            lp.height = -1;
            lp.flags = 0x20 | 0x20000;   // NOT_TOUCH_MODAL | ALT_FOCUSABLE_IM（不触发输入法）
            // 3224：玻璃质感 —— 窗口背后做模糊（实测系统的磨砂卡片），
            //   配合下面把卡片颜色调成半透明，就是"毛玻璃"而不是死板的白块。
            boolean blurOk = false;
            try {
                lp.flags |= 0x4;         // FLAG_BLUR_BEHIND
                lp.setBlurBehindRadius(com.oupao.wiz.Ui.dp(ctx, 28));
                blurOk = true;
            } catch (Throwable ignore) {
                blurOk = false;
            }
            lp.gravity = Gravity.CENTER;
            lp.setTitle("oupao-more");
            try {
                lp.setFitInsetsTypes(0);
            } catch (Throwable ignore) {
            }
            layer.setFocusableInTouchMode(true);
            layer.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, android.view.KeyEvent e) {
                    if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)
                            && (e.getAction() == android.view.KeyEvent.ACTION_UP)) {
                        MorePanel.hide();
                        return true;
                    }
                    return false;
                }
            });
            wm.addView(layer, lp);
            // 3224：模糊生效时把卡片改成半透明，让磨砂效果透出来（不生效就保持原样）
            if (blurOk) {
                try {
                    int cb0 = com.oupao.wiz.Ui.cardBg();
                    int a = (cb0 >>> 24) & 0xFF;
                    if (a > 0xB4) {
                        a = 0xB4;        // **压到**约 70%：透出模糊，同时保证文字可读
                    }
                    if (a < 0x9A) {
                        a = 0x9A;
                    }
                    pbg.setColor((a << 24) | (cb0 & 0x00FFFFFF));
                    panel.invalidate();
                    com.oupao.wiz.SLog.i("MorePanel: 已启用背景模糊（玻璃质感）");
                } catch (Throwable ignore) {
                }
            }
            rootView = layer;
            layer.requestFocus();

            // 动画延后一帧启动：只缩放，不动 alpha，遮罩保持同一暗度
            panel.post(new Runnable() {
                public void run() {
                    try {
                        panelRef.setPivotX(panelRef.getWidth() / 2.0f);
                        panelRef.setPivotY(panelRef.getHeight() / 2.0f);
                        panelRef.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(240)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                    } catch (Throwable ignore) {
                    }
                }
            });
            startIconThread();
            SLog.i("MorePanel: 已显示 " + pw + "x" + ph + " land=" + land + " side="
                    + com.oupao.wiz.FlyH.side + " mgr=" + Pins.appMgr + " tab=" + tab);
        } catch (Throwable t) {
            SLog.e("MorePanel: show 异常", t);
            rootView = null;
        }
    }

    public static void hide() {
        try {
            var layer = rootView;
            var w = wm;
            var panel = panelRef;
            rootView = null;
            panelRef = null;
            innerRef = null;
            if ((layer == null) || (w == null)) {
                return;
            }
            try {
                var imm = (android.view.inputmethod.InputMethodManager)
                        layer.getContext().getSystemService("input_method");
                if (imm != null) {
                    imm.hideSoftInputFromWindow(layer.getWindowToken(), 0);
                }
            } catch (Throwable ignore) {
            }
            if (panel != null) {
                panel.animate().scaleX(0.85f).scaleY(0.85f).alpha(0.0f)
                        .setDuration(160)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
                final View fl = layer;
                final WindowManager fw = w;
                layer.animate().alpha(0.0f).setDuration(160).withEndAction(new Runnable() {
                    public void run() {
                        try {
                            fw.removeViewImmediate(fl);
                        } catch (Throwable ignore) {
                        }
                    }
                }).start();
            } else {
                w.removeViewImmediate(layer);
            }
        } catch (Throwable t) {
            SLog.e("MorePanel: hide 异常", t);
            rootView = null;
            panelRef = null;
            innerRef = null;
        }
    }

    /** 延迟刷新：主线程 Handler（下一帧） */
    private static final android.os.Handler UI =
            new android.os.Handler(android.os.Looper.getMainLooper());

    /**
     * 延迟刷新面板。
     *
     * 为什么不能在 DROP 回调里同步重建：
     * MorePanel 跑在 system_server，而 system_server 的 drag 流程是：
     *   分发 DROP 事件 → （我们的 onDrag 回调里重建视图，拖拽源 View 被销毁，
     *     拖拽阴影的 Surface 被 release）→ 事件分发返回
     *   → reportDropResult → endDragLocked → closeLocked → reparent(drag surface)
     *     → surface 已 release → NPE → system_server 挂掉 → 手机重启
     *
     * 延迟到下一帧刷新，让 system_server 先把这次 drag 收尾完，再重建面板。
     */
    public static void refreshDelayed() {
        try {
            UI.removeCallbacks(REFRESH_RUN);
            UI.postDelayed(REFRESH_RUN, 800L);
        } catch (Throwable ignore) {
        }
    }

    private static final Runnable REFRESH_RUN = new Runnable() {
        public void run() {
            MorePanel.refresh();
        }
    };

    private static void refresh() {
        try {
            if ((innerRef == null) || (panelRef == null)) {
                return;
            }
            var ctx = Cfg.sysContext();
            if (ctx == null) {
                return;
            }
            // 保存当前滚动位置：加/删固定应用后不要跳回顶部
            int keepY = 0;
            try {
                if (svRef != null) {
                    keepY = svRef.getScrollY();
                }
            } catch (Throwable ignore) {
            }
            final int keepTab = tab;
            innerRef.removeAllViews();
            for (int i = (panelRef.getChildCount() - 1); i >= 0; i--) {
                if (panelRef.getChildAt(i) != innerRef) {
                    panelRef.removeViewAt(i);
                }
            }
            // 重建内容必须用**同一个宽度**（横屏时是按短边等比缩小的值）
            int pwNow = (panelW > 0) ? panelW : calcSize(ctx)[0];
            buildContent(scaleCtx(ctx, panelScale), pwNow);
            startIconThread();
            // 恢复滚动位置（buildContent 会新建 ScrollView，所以要 post 到下一帧）
            try {
                final ScrollView nsv = svRef;
                final int y = keepY;
                if (nsv != null) {
                    nsv.post(new Runnable() {
                        public void run() {
                            try {
                                nsv.scrollTo(0, y);
                                SLog.i("MorePanel: 恢复滚动 y=" + y + " tab=" + keepTab);
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                }
            } catch (Throwable ignore) {
            }
        } catch (Throwable t) {
            SLog.e("MorePanel: refresh 异常", t);
        }
    }

    // ---------------- 内容 ----------------

    /** 平滑滚动到指定的 view（字母轴点击后定位到对应字母） */
    private static void jumpTo(android.widget.ScrollView p1, android.view.View p2, android.view.View p3) {
        try {
            // ★ 这里原来调的是 FlyH.buzz()，它用的是「菜单划选应用」的开关和强度，
            //   跟「索引滑动时的触感强弱」滑条无关 → 索引滑条调到 0% 面板还在震。
            //   面板引索条属于「索引滑动」，改用 pinVib() + pinVibLevel()。
            if (com.oupao.wiz.Cfg.pinVib()) {
                int lv = com.oupao.wiz.Cfg.pinVibLevel();
                if (lv > 0) {
                    com.oupao.wiz.Idx.buzzAsync(lv);
                    com.oupao.wiz.SLog.r("PVib", "MorePanel: \u5f15\u7d22\u632f\u52a8 lv=" + lv, 500L);
                }
            }
            try {
                p1.post(new com.oupao.wiz.MorePanel$C18(p3, p2, p1));
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
        }
    }

    private static void buildContent(Context ctx, int pw) {


        int fg = Ui.textMain();
        int fg2 = Ui.textSub();
        int accent = -13335809;
        // ctx 已经过 scaleCtx()：这里的 dp/sp 会自动等比缩放，不用再手动乘系数
        int size = FlyH.spxIcon(ctx, 30);
        int pad = Ui.dp(ctx, 10);
        int railW = Ui.dp(ctx, 22);
        int sidePad = (pad + (railW / 2));      // 左右等距
        int cellW = ((pw - (sidePad * 2)) / 4);
        int cellH = (size + Ui.dp(ctx, 34));

        var inner = innerRef;

        // 顶部：标题 + 管理/完成
        var topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 12), Ui.dp(ctx, 12), Ui.dp(ctx, 2));
        var title = new TextView(ctx);
        title.setText("更多应用");
        title.setTextSize(17.0f);
        title.setTextColor(fg);
        topRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));
        var mgrBtn = new TextView(ctx);
        mgrBtn.setText(Pins.appMgr ? "完成" : "管理");
        mgrBtn.setTextSize(15.0f);
        mgrBtn.setTextColor(-13669377);
        // 触摸范围要够大（原来只有 10/4/2/4 的 padding，很难点中）
        mgrBtn.setGravity(android.view.Gravity.CENTER);
        mgrBtn.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 10), Ui.dp(ctx, 18), Ui.dp(ctx, 10));
        mgrBtn.setMinWidth(Ui.dp(ctx, 72));
        mgrBtn.setMinHeight(Ui.dp(ctx, 44));
        mgrBtn.setClickable(true);
        com.oupao.wiz.Ui.ripple(mgrBtn);
        mgrBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Pins.appMgr = !Pins.appMgr;
                SLog.i("MorePanel: 管理模式=" + Pins.appMgr);
                MorePanel.refresh();
            }
        });
        // 长按标题：在「面板 / 小窗」之间切换「更多应用」的呼出方式。
        // ★ 关掉 haptic：OnLongClickListener 默认会触发一次长按震动，
        //   而标题几乎占满整行，用户点「管理」稍微偏一点就会长按到标题 →
        //   感觉「震了一下但没点到管理」，还会误把面板关掉。
        title.setHapticFeedbackEnabled(false);
        title.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                // ★ 不读配置判断：本面板就是「面板模式」，长按必然是切到小窗。
                //   （读 Settings.Global 有进程内缓存，跨进程改完可能读到旧值）
                String next = "window";
                Cfg.setMoreMode(next);
                SLog.i("MorePanel: 呼出方式 -> " + next);
                MorePanel.hide();
                // 提示统一由「模块 App」来弹（系统 uid 的 Toast 圆角样式和应用的不一样，
                // 用户反馈两种模式提示不一致）。这里只把文案交给要打开的小窗页去弹。
                // 立刻切过去，不等下次呼出（用户要求）
                com.oupao.wiz.FlyH.openMore();
                return true;
            }
        });
        topRow.addView(mgrBtn, new LinearLayout.LayoutParams(-2, -2));
        final float[] tDown = new float[]{0f, 0f};
        topRow.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    tDown[0] = e.getX();
                    tDown[1] = e.getY();
                    return false;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float dx = Math.abs(e.getX() - tDown[0]);
                    float dy = (e.getY() - tDown[1]);
                    if ((Math.abs(dy) > Ui.dp(v.getContext(), 80)) || (dx > Ui.dp(v.getContext(), 130))) {
                        MorePanel.hide();      // 标题栏上滑/下滑/横滑 = 关闭
                        return true;
                    }
                }
                return false;
            }
        });
        inner.addView(topRow, new LinearLayout.LayoutParams(-1, -2));

        // 滚动区
        // 尽量复用上一次的 ScrollView（refresh 时它已经被从父容器上拆下来），
        // 避免每次都新建一个滚动容器导致界面闪一下。
        ScrollView sv;
        if ((svRef != null) && (svRef.getParent() == null)) {
            sv = svRef;
            sv.removeAllViews();
        } else {
            sv = new ScrollView(ctx);
        }
        sv.setVerticalScrollBarEnabled(false);
        svRef = sv;
        var content = new LinearLayout(ctx);
        contentRef = content;
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(sidePad, 0, sidePad, pad);
        sv.addView(content, new FrameLayout.LayoutParams(-1, -2));

        // 已添加（在滚动区内）
        final View[] pinnedHead = new View[]{null};
        var pinned = pinState;
        if ((pinned != null) && (pinned.size() > 0)) {
            var ph0 = section(ctx, "已添加", fg2);
            pinnedHead[0] = ph0;
            content.addView(ph0);
            var g0 = new GridLayout(ctx);
            g0.setColumnCount(4);
            com.oupao.wiz.MorePanel.pinGrid = g0;
            content.addView(g0, new LinearLayout.LayoutParams(-1, -2));
            for (int i = 0; i < pinned.size(); i++) {
                final String pin = String.valueOf(pinned.get(i));
                var cellV = addCell(ctx, g0, pin, true, cellW, cellH, size, fg2);
                if (cellV != null) {
                    // 长按拖动排序（只在"管理"模式下可用）。
                    // ★ 用自绘拖动（PinDrag），不用系统 startDragAndDrop：
                    //   面板在 system_server 里，系统拖拽阴影的 Surface 由 WindowManager 管，
                    //   拖拽过程中一搬动源 View 就会 reparent 已释放的 surface → NPE → 系统重启。
                    cellV.setTag(pin);
                    com.oupao.wiz.PinDrag.attach(g0, cellV, new com.oupao.wiz.PinDrag.Commit() {
                        public void onMove(String from, String to) {
                            MorePanel.reorderPins(from, to);
                        }
                    });
                }
            }
        }

        // 应用 / 工具 两个圆角长条按钮
        var tabs = new LinearLayout(ctx);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(sidePad, Ui.dp(ctx, 8), sidePad, Ui.dp(ctx, 4));
        final var tabApp = new TextView(ctx);
        final var tabTool = new TextView(ctx);
        tabApp.setText("应用");
        tabTool.setText("工具");
        tabApp.setTextSize(13.0f);
        tabTool.setTextSize(13.0f);
        tabApp.setGravity(Gravity.CENTER);
        tabTool.setGravity(Gravity.CENTER);
        var lpA = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lpA.rightMargin = Ui.dp(ctx, 6);
        var lpB = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lpB.leftMargin = Ui.dp(ctx, 6);
        tabs.addView(tabApp, lpA);
        tabs.addView(tabTool, lpB);
        content.addView(tabs, new LinearLayout.LayoutParams(-1, -2));

        var appsWrap = new LinearLayout(ctx);
        appsWrap.setOrientation(LinearLayout.VERTICAL);
        var toolsWrap = new LinearLayout(ctx);
        toolsWrap.setOrientation(LinearLayout.VERTICAL);
        content.addView(appsWrap, new LinearLayout.LayoutParams(-1, -2));
        content.addView(toolsWrap, new LinearLayout.LayoutParams(-1, -2));

        var gApps = new GridLayout(ctx);
        gApps.setColumnCount(4);
        // ★「最近应用」：数据来自系统侧边栏（SideDock.dumpRecents → Cfg "recents"），
        //   位置按用户要求放在**字母分区的上方**。
        try {
            String rec = com.oupao.wiz.Cfg.getStrFresh("recents", "");
            if ((rec != null) && (rec.trim().length() > 0)) {
                String[] lines = rec.split("\n");
                var rTags = new ArrayList<String>();
                var seenR = new java.util.HashSet<String>();
                for (int i = 0; (i < lines.length) && (rTags.size() < 8); i++) {
                    String line = lines[i];
                    int c = line.indexOf('\u0001');
                    if (c <= 0) {
                        continue;
                    }
                    String label = line.substring(0, c).trim();
                    String pkg = line.substring(c + 1).trim();
                    if ((pkg.length() == 0) || (label.length() == 0)) {
                        continue;
                    }
                    String cls = launchClsOf(ctx, pkg);
                    if (cls == null) {
                        continue;
                    }
                    String tag = pkg + "#" + cls;
                    if (!seenR.add(tag)) {
                        continue;
                    }
                    if (isPinned(tag)) {
                        continue;
                    }
                    rTags.add(tag);
                }
                if (rTags.size() > 0) {
                    appsWrap.addView(section(ctx, "最近应用", fg2));
                    var gRec = new GridLayout(ctx);
                    gRec.setColumnCount(4);
                    appsWrap.addView(gRec, new LinearLayout.LayoutParams(-1, -2));
                    for (int i = 0; i < rTags.size(); i++) {
                        addCell(ctx, gRec, rTags.get(i), true, cellW, cellH, size, fg2);
                    }
                    SLog.i("MorePanel: \u6700\u8fd1\u5e94\u7528\u6e32\u67d3 n=" + rTags.size());
                }
            }
        } catch (Throwable t) {
            SLog.e("MorePanel: \u6700\u8fd1\u5e94\u7528\u6e32\u67d3\u5931\u8d25", t);
        }
        appsWrap.addView(gApps, new LinearLayout.LayoutParams(-1, -2));
        var gTools = new GridLayout(ctx);
        gTools.setColumnCount(4);
        toolsWrap.addView(gTools, new LinearLayout.LayoutParams(-1, -2));

        // 应用数据（拼音首字母 + 名称排序，# 置底）
        String[] raw = Pins.apps(ctx);
        var pins = new ArrayList<String>();
        var labels = new ArrayList<String>();
        var letters = new ArrayList<String>();
        var keys = new ArrayList<String>();
        if (raw != null) {
            for (int i = 0; i < raw.length; i++) {
                String pin = raw[i];
                String tg = tagOf(pin);
                if (isPinned(tg)) {
                    continue;                       // 已固定的不出现在全部应用
                }
                String lb = labelOf(pin);
                String lt = letterOf(lb);
                pins.add(pin);
                labels.add(lb);
                letters.add(lt);
                keys.add((lt.equals("#") ? "ZZZ#" : lt) + "\u0002" + lb);
            }
        }
        for (int i = 1; i < pins.size(); i++) {
            for (int j = i; j > 0; j--) {
                if (keys.get(j).compareToIgnoreCase(keys.get(j - 1)) < 0) {
                    var a = keys.get(j); keys.set(j, keys.get(j - 1)); keys.set(j - 1, a);
                    var b = pins.get(j); pins.set(j, pins.get(j - 1)); pins.set(j - 1, b);
                    var c = labels.get(j); labels.set(j, labels.get(j - 1)); labels.set(j - 1, c);
                    var e2 = letters.get(j); letters.set(j, letters.get(j - 1)); letters.set(j - 1, e2);
                } else {
                    break;
                }
            }
        }

        var firstOfLetter = new HashMap<String, View>();
        String lastLetter = "";
        for (int i = 0; i < pins.size(); i++) {
            String letter = letters.get(i);
            if (!letter.equals(lastLetter)) {
                lastLetter = letter;
                var head = new TextView(ctx);
                head.setText(letter);
                head.setTextSize(12.0f);
                head.setTextColor(fg2);
                head.setPadding(Ui.dp(ctx, 6), Ui.dp(ctx, 12), 0, Ui.dp(ctx, 2));
                var hlp = new GridLayout.LayoutParams();
                hlp.columnSpec = GridLayout.spec(0, 4);
                hlp.width = (cellW * 4);
                hlp.height = -2;
                gApps.addView(head, hlp);
                if (!firstOfLetter.containsKey(letter)) {
                    firstOfLetter.put(letter, head);
                }
            }
            addCell(ctx, gApps, pins.get(i), true, cellW, cellH, size, fg2);
        }

        // 工具
        String[] tools = null;
        try {
            tools = Tools.tags();
        } catch (Throwable ignore) {
        }
        if (tools != null) {
            for (int i = 0; i < tools.length; i++) {
                // 3156：已固定的工具不再出现在「待添加」里（和应用一致）。
                // 之前 3145 为了修"工具不全"把这行去掉了，结果加了工具也不会消失；
                // 现在图标问题已经单独修好（12 个都能解析），这里恢复过滤。
                if (isPinned(tools[i])) {
                    continue;
                }
                addCell(ctx, gTools, tools[i], false, cellW, cellH, size, fg2);
            }
        }

        final var fApps = appsWrap;
        final var fTools = toolsWrap;
        final int cOn = accent;
        final int cOff = fg2;
        Runnable selApp = new Runnable() {
            public void run() {
                tab = 0;
                fApps.setVisibility(View.VISIBLE);
                fTools.setVisibility(View.GONE);
                styleTab(tabApp, true);
                styleTab(tabTool, false);
            }
        };
        Runnable selTool = new Runnable() {
            public void run() {
                tab = 1;
                fApps.setVisibility(View.GONE);
                fTools.setVisibility(View.VISIBLE);
                styleTab(tabApp, false);
                styleTab(tabTool, true);
            }
        };
        tabApp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selApp.run();
            }
        });
        tabTool.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selTool.run();
            }
        });
        if (tab == 1) {
            selTool.run();
        } else {
            selApp.run();
        }

        inner.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        // 字母索引：★ + A-Z + #，垂直居中，整列可拖动
        final var railLetters = new ArrayList<String>();
        final var railTargets = new ArrayList<View>();
        idxLetters.clear();
        idxTargets.clear();
        final var rail = new LinearLayout(ctx);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER);
        // ★ 字母条高度自适应：横屏面板变矮时，27 个字 × 13dp 会超出面板高度，
        //   表现就是「引索条显示不完全」（用户反馈）。按可用高度压缩每格。
        int railN = (pinnedHead[0] != null) ? 1 : 0;
        {
            String a0 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
            for (int i = 0; i < a0.length(); i++) {
                if (firstOfLetter.get(String.valueOf(a0.charAt(i))) != null) {
                    railN++;
                }
            }
        }
        int railAvail = (panelH > 0) ? (panelH - Ui.dp(ctx, 118)) : 0;
        int baseItemH = Ui.dp(ctx, 13);
        final int railItemH;
        if ((railAvail > 0) && (railN > 0) && ((railN * baseItemH) > railAvail)) {
            railItemH = Math.max(Ui.dp(ctx, 7), (railAvail / railN));
        } else {
            railItemH = baseItemH;
        }
        if (pinnedHead[0] != null) {
            railLetters.add("\u2605");
            railTargets.add(pinnedHead[0]);
            idxLetters.add("\u2605");
            idxTargets.add(pinnedHead[0]);
            rail.addView(railLetter(ctx, "\u2605", railW, fg2), new LinearLayout.LayoutParams(railW, railItemH));
        }
        String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
        for (int i = 0; i < alpha.length(); i++) {
            String ch = String.valueOf(alpha.charAt(i));
            final View target = firstOfLetter.get(ch);
            if (target == null) {
                continue;
            }
            railLetters.add(ch);
            railTargets.add(target);
            idxLetters.add(ch);
            idxTargets.add(target);
            rail.addView(railLetter(ctx, ch, railW, fg2), new LinearLayout.LayoutParams(railW, railItemH));
        }
        var railWrap = new FrameLayout(ctx);
        railWrap.addView(rail, new FrameLayout.LayoutParams(railW, -2, Gravity.CENTER_VERTICAL));
        final ScrollView fsv = sv;
        final LinearLayout fc = content;
        final int itemH = railItemH;
        final int[] lastIdx = new int[]{-1};
        railWrap.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                int n = railLetters.size();
                if (n <= 0) {
                    return true;
                }
                int top = ((v.getHeight() - (n * itemH)) / 2);
                int idx = (((int) e.getY()) - top) / Math.max(1, itemH);
                if (idx < 0) {
                    idx = 0;
                }
                if (idx >= n) {
                    idx = (n - 1);
                }
                if ((e.getActionMasked() == MotionEvent.ACTION_DOWN)
                        || (e.getActionMasked() == MotionEvent.ACTION_MOVE)) {
                    if (idx != lastIdx[0]) {
                        lastIdx[0] = idx;
                        // 振动统一由 jumpTo 负责（避免重复震两下）
                        MorePanel.jumpTo(fsv, fc, railTargets.get(idx));
                    }
                }
                return true;
            }
        });
        var rwLp = new FrameLayout.LayoutParams(railW, -1);
        rwLp.gravity = Gravity.END;
        panelRef.addView(railWrap, rwLp);
    }

    private static TextView section(Context ctx, String text, int color) {
        var tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(13.0f);
        tv.setTextColor(color);
        tv.setPadding(Ui.dp(ctx, 6), Ui.dp(ctx, 10), 0, Ui.dp(ctx, 4));
        return tv;
    }

    private static TextView railLetter(Context ctx, String ch, int w, int color) {
        var lt = new TextView(ctx);
        lt.setText(ch);
        // 字号不用手动缩：ctx 是 scaleCtx() 出来的，sp 会跟着密度一起缩
        lt.setTextSize(9.0f);
        lt.setTextColor(color);
        lt.setGravity(Gravity.CENTER);
        lt.setIncludeFontPadding(false);
        return lt;
    }

    private static void styleTab(TextView tv, boolean on) {
        try {
            var ctx = tv.getContext();
            var bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(Ui.dp(ctx, 16));
            if (on) {
                bg.setColor(-13335809);
                tv.setTextColor(-1);
                tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            } else {
                bg.setColor(Ui.night() ? 0xFF2A2A2A : 0xFFEDEDF0);
                tv.setTextColor(Ui.night() ? -1 : -14935012);
                tv.setTypeface(android.graphics.Typeface.DEFAULT);
            }
            tv.setBackground(bg);
            tv.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 7), Ui.dp(ctx, 10), Ui.dp(ctx, 7));
        } catch (Throwable ignore) {
        }
    }

    private static boolean isPinned(String tag) {
        try {
            // 3155：刚点过 +/- 的 5 秒内以内存里的结果为准。
            // 落地是「写文件 + 广播」，build() 立刻回读时可能还是旧值，
            // 于是刚添加的应用又出现在"待添加"里（用户报的现象）。
            if (pinOverride != null &&
                    android.os.SystemClock.uptimeMillis() - pinOverrideAt < 5000L) {
                return pinOverride.contains(tag);
            }
            return pinState.contains(tag);
        } catch (Throwable ignore) {
        }
        return false;
    }

    /** 最近一次 +/- 之后的内存结果（短时权威，避免回读延迟导致列表回跳） */
    private static ArrayList<String> pinOverride;
    private static long pinOverrideAt;

    private static ArrayList<String> pinState() {
        try {
            return new ArrayList<String>(Pins.list());
        } catch (Throwable ignore) {
        }
        return new ArrayList<String>();
    }

    // ---------------- 单个格子 ----------------

    /**
     * 判断这个条目是「应用」还是「工具」。
     *
     * 应用条目 = "显示名\u0001pkg#activity"（带 \u0001 分隔）
     * 工具条目 = "tool#xxx"（裸 tag）
     *
     * 原来「已添加」区写死传 isApp=true，工具被当成应用 → 显示原始 tag、图标取不到、点击无效。
     */
    private static boolean looksLikeApp(String pin) {
        try {
            if (pin == null) {
                return false;
            }
            if (pin.indexOf('\u0001') > 0) {
                return true;
            }
            if (pin.startsWith("tool#")) {
                return false;
            }
            // 没有分隔符时：带 "#" 且前半段像包名 → 应用；否则当工具
            int h = pin.indexOf('#');
            if (h > 0) {
                String p = pin.substring(0, h);
                return (p.indexOf('.') > 0);
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private static View addCell(Context ctx, GridLayout grid, String pin, boolean isApp, int cellW, int cellH, int size, int fg2) {
        try {
            // 优先按内容判断（工具不能被当成应用）
            boolean appFlag = isApp;
            if (looksLikeApp(pin)) {
                appFlag = true;
            } else if ((pin != null) && (pin.startsWith("tool#"))) {
                appFlag = false;
            }
            // 匿名内部类要捕获，必须是 final（不能与参数同名）
            final boolean isAppF = appFlag;
            final String tag = isAppF ? tagOf(pin) : pin;
            String label;
            if (isAppF) {
                label = labelOf(pin);
                if (label.equals(tag) || label.contains("#") || label.contains(".")) {
                    label = appLabel(ctx, pkgOf(tag));
                }
            } else {
                label = tag;
                try {
                    String l = Tools.label(tag);
                    if ((l != null) && (l.length() > 0)) {
                        label = l;
                    }
                } catch (Throwable ignore) {
                }
            }

            var box = new FrameLayout(ctx);
            var cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, Ui.dp(ctx, 4), 0, Ui.dp(ctx, 2));
            var iv = new ImageView(ctx);
            cell.addView(iv, new LinearLayout.LayoutParams(size, size));
            var tv = new TextView(ctx);
            tv.setText(label);
            tv.setTextSize(10.0f);
            tv.setTextColor(fg2);
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setIncludeFontPadding(false);
            tv.setPadding(Ui.dp(ctx, 2), Ui.dp(ctx, 3), Ui.dp(ctx, 2), Ui.dp(ctx, 1));
            cell.addView(tv, new LinearLayout.LayoutParams(-1, -2));
            box.addView(cell, new FrameLayout.LayoutParams(-1, -1));

            // 图标：缓存命中直接贴，否则交给后台线程
            var ck = tag + "@" + size;
            var cached = iconCache.get(ck);
            if (cached != null) {
                iv.setImageBitmap(cached);
            } else {
                iconJobs.add(new Object[]{iv, tag, Integer.valueOf(size), Boolean.valueOf(isAppF)});
            }

            // 3157：管理模式下**一律**画 +/-。
            // 以前还要求 `"panel".equals(Cfg.moreMode())`，但 more_mode 是跨进程镜像，
            // 读到 "window"（或旧的 "window" 比新写入更"新"）时面板里的加减号就整体消失
            // ——用户报的「面板模式的 +- 没了」。既然 MorePanel 已经显示出来了，就跟模式无关。
            if (Pins.appMgr) {
                addBadge(ctx, box, isPinned(tag));
            }
            box.setClickable(true);
            box.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (Pins.appMgr) {
                        MorePanel.togglePin(tag);
                        return;
                    }
                    if (!isAppF) {
                        SLog.i("MorePanel: 工具 " + tag);
                        MorePanel.hide();
                        // 直接在 system_server 里执行，不再走 com.oupao.wiz.TOOL 广播：
                        // 那条广播的接收器（Rx/RxInit）在 system_server 里注册经常失败
                        // （日志：Rx: 3参/5参 失败 → IActivityManager 为 null），
                        // 结果就是「点了工具没反应」。广播只作兜底。
                        String tid = tag.substring(5);
                        try {
                            var c = Cfg.sysContext();
                            boolean ok = (c != null) && Tools.fire(c, tid);
                            if (!ok && c != null) {
                                c.sendBroadcast(new android.content.Intent("com.oupao.wiz.TOOL")
                                        .putExtra("id", tid));
                            }
                            SLog.i("MorePanel: 工具 " + tid + " 直调=" + ok);
                        } catch (Throwable t) {
                            SLog.e("MorePanel: 工具执行失败", t);
                        }
                        return;
                    }
                    SLog.i("MorePanel: 点击 " + tag);
                    MorePanel.hide();
                    MorePanel.fireApp(tag);
                }
            });
            var lp = new GridLayout.LayoutParams();
            lp.width = cellW;
            lp.height = cellH;
            grid.addView(box, lp);
            return box;
        } catch (Throwable t) {
            SLog.e("MorePanel: addCell 异常", t);
        }
        return null;
    }

    private static void addBadge(Context ctx, FrameLayout box, boolean pinned) {
        var badge = new TextView(ctx);
        badge.setText(pinned ? "\u2212" : "+");
        badge.setTextSize(11.0f);
        badge.setTextColor(-1);
        badge.setGravity(Gravity.CENTER);
        badge.setIncludeFontPadding(false);
        var bd = new GradientDrawable();
        bd.setShape(GradientDrawable.OVAL);
        bd.setColor(pinned ? 0xFFFF3B30 : 0xFF34C759);   // − 红 / + 绿
        badge.setBackground(bd);
        var blp = new FrameLayout.LayoutParams(Ui.dp(ctx, 15), Ui.dp(ctx, 15));
        blp.gravity = Gravity.TOP | Gravity.END;
        blp.topMargin = Ui.dp(ctx, 2);
        blp.rightMargin = Ui.dp(ctx, 4);
        box.addView(badge, blp);
    }

    // ---------------- 后台图标渲染 ----------------

    private static void startIconThread() {
        try {
            if (iconJobs.isEmpty()) {
                return;
            }
            final var jobs = new ArrayList<Object[]>(iconJobs);
            iconJobs.clear();
            var th = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < jobs.size(); i++) {
                        try {
                            var job = jobs.get(i);
                            final ImageView iv = (ImageView) job[0];
                            final String tag = String.valueOf(job[1]);
                            final int size = ((Integer) job[2]).intValue();
                            final boolean isApp = ((Boolean) job[3]).booleanValue();
                            var ctx = Cfg.sysContext();
                            if (ctx == null) {
                                continue;
                            }
                            var ck = tag + "@" + size;
                            Bitmap bm = iconCache.get(ck);
                            if (bm == null) {
                                Drawable d = isApp ? Ico.d(ctx, pkgOf(tag)) : Tools.drawable(ctx, tag);
                                int uid = isApp ? uidOf(tag) : 0;
                                if (uid > 0) {
                                    // 3416：分身角标直接取系统原生 drawable（不再裁切合成图标）。
                                    int cuser = (uid >= 100000) ? (uid / 100000) : uid;
                                    try {
                                        bm = Sug.cloneIconCircle(pkgOf(tag), cuser, size, false);
                                    } catch (Throwable t) {
                                        com.oupao.wiz.SLog.e("MorePanel: 分身图标合成失败", t);
                                    }
                                }
                                if (bm == null) {
                                    bm = Sug.circleFitIcon(d, size);   // 圆形（后台线程，慢也无妨）
                                }
                                if (bm == null) {
                                    bm = circleOf(d, size);
                                }
                                if (bm != null) {
                                    iconCache.put(ck, bm);
                                    // 上限 128（含工具项，比 IdxBar 宽松些）：不设上限时
                                    // 每次刷新面板 + 滑过的每个图标都会留在 HashMap 里
                                    if (iconCache.size() > 128) {
                                        iconCache.clear();
                                    }
                                }
                            }
                            final Bitmap fbm = bm;
                            if (fbm != null) {
                                iv.post(new Runnable() {
                                    public void run() {
                                        iv.setImageBitmap(fbm);
                                    }
                                });
                            }
                        } catch (Throwable ignore) {
                        }
                    }
                }
            });
            th.setPriority(Thread.MIN_PRIORITY);
            th.start();
        } catch (Throwable t) {
            SLog.e("MorePanel: 图标线程异常", t);
        }
    }

    // ---------------- 动作 ----------------

    /** 管理模式下长按拖动排序：把 from 挪到 to 的位置，写回固定应用并重建面板 */
    private static void reorderPins(String from, String to) {
        try {
            if ((from == null) || (to == null) || from.equals(to)) {
                return;
            }
            int i = pinState.indexOf(from);
            int j = pinState.indexOf(to);
            if ((i < 0) || (j < 0)) {
                SLog.i("MorePanel: 排序忽略 from=" + from + " to=" + to);
                return;
            }
            // 互换（不是插入挤位）
            pinState.set(i, to);
            pinState.set(j, from);
            String next = Pins.join(pinState);
            persist(next);
            pinState = new ArrayList<String>(Pins.parse(next));
            SLog.i("MorePanel: 拖动排序 " + i + "->" + j + " 共 " + pinState.size() + " 个");
            // 只把被拖的那一格就地挪到新位置，**不重建视图**。
            // 重建会销毁拖拽源 View，拖拽阴影的 Surface 随之被 release，
            // system_server 收尾时 reparent 已 release 的 surface → NPE → 手机重启；
            // 同时重建还会带来 800ms 的替换延迟。
            MorePanel.swapCell(from, to);
        } catch (Throwable t) {
            SLog.e("MorePanel: reorderPins 异常", t);
        }
    }
    /**
     * 把被拖的那一格就地挪到新位置（不重建视图）。
     *
     * @param from 被拖的 tag
     * @param to   目标 tag
     */
    private static void swapCell(String from, String to) {
        try {
            var grid = com.oupao.wiz.MorePanel.pinGrid;
            if ((grid == null) || (from == null) || (to == null)) {
                return;
            }
            View src = null;
            View dst = null;
            for (int i = 0; i < grid.getChildCount(); i++) {
                View c = grid.getChildAt(i);
                Object t = c.getTag();
                if (!(t instanceof String)) {
                    continue;
                }
                if (from.equals(t)) {
                    src = c;
                } else if (to.equals(t)) {
                    dst = c;
                }
            }
            if ((src == null) || (dst == null)) {
                return;
            }
            // 两格直接对调位置：收集子 view，交换两者，再按新顺序放回（不销毁任何 view）
            int ci = grid.getChildCount();
            java.util.ArrayList<View> kids = new java.util.ArrayList<View>();
            java.util.ArrayList<android.view.ViewGroup.LayoutParams> lps =
                    new java.util.ArrayList<android.view.ViewGroup.LayoutParams>();
            for (int i = 0; i < ci; i++) {
                View c = grid.getChildAt(i);
                kids.add(c);
                lps.add(c.getLayoutParams());
            }
            int a1 = grid.indexOfChild(src);
            int b1 = grid.indexOfChild(dst);
            if ((a1 < 0) || (b1 < 0)) {
                return;
            }
            java.util.Collections.swap(kids, a1, b1);
            grid.removeAllViews();
            for (int i = 0; i < kids.size(); i++) {
                View c = kids.get(i);
                if (lps.get(i) != null) {
                    grid.addView(c, lps.get(i));
                } else {
                    grid.addView(c);
                }
            }
            src.setAlpha(1.0f);
        } catch (Throwable t) {
            SLog.e("MorePanel: moveCell 异常", t);
        }
    }

    private static void togglePin(String tag) {
        try {
            String cur = curPins();
            boolean has = Pins.parse(cur).contains(tag);
            String next = has ? Pins.del(cur, tag) : Pins.withAdd(cur, tag);
            persist(next);
            pinState = new ArrayList<String>(Pins.parse(next));
            // 3155：刚点完的 5 秒内用内存结果做 isPinned 判据，
            // 否则 build() 立刻回读到旧值 → 刚加的应用又出现在"待添加"里
            pinOverride = new ArrayList<String>(pinState);
            pinOverrideAt = android.os.SystemClock.uptimeMillis();
            SLog.i("MorePanel: " + (has ? "\u79fb\u51fa" : "\u52a0\u5165") + " \u56fa\u5b9a\u5e94\u7528 "
                    + tag + " \u5171 " + pinState.size() + " \u4e2a");
            MorePanel.refresh();
        } catch (Throwable t) {
            SLog.e("MorePanel: togglePin \u5f02\u5e38", t);
        }
    }

    private static void fireApp(String tag) {
        try {
            var c = Cfg.sysContext();
            var seg = tag.split("#");
            if (seg.length >= 2) {
                c.sendBroadcast(new android.content.Intent("com.oupao.wiz.CLONE")
                        .putExtra("pkg", seg[0]).putExtra("cls", seg[1])
                        .putExtra("user", uidOf(tag)));
            } else {
                var li = c.getPackageManager().getLaunchIntentForPackage(seg[0]);
                if (li != null) {
                    li.addFlags(268435456);
                    c.startActivity(li);
                }
            }
        } catch (Throwable t) {
            SLog.e("MorePanel: \u542f\u52a8\u5931\u8d25", t);
        }
    }
    public static void updateSubtitle(android.view.ViewGroup root, String text) {
        try {
            for (int i = 0; i < root.getChildCount(); i++) {
                android.view.View ch = root.getChildAt(i);
                if (ch instanceof android.widget.TextView) {
                    var tv = (android.widget.TextView) ch;
                    CharSequence cur = tv.getText();
                    if ((cur != null) && cur.toString().startsWith("\u5f53\u524d\uff1a")) {
                        tv.setText(text);
                        return;
                    }
                } else if (ch instanceof android.view.ViewGroup) {
                    updateSubtitle((android.view.ViewGroup) ch, text);
                }
            }
        } catch (Throwable t) {
        }
    }
}
