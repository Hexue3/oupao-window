package com.oupao.wiz;

/**
 * 设置页「扇区大小 / 图标大小」滑块的实时预览。
 *
 * 数字与真菜单同源：[FlyFanH.fanRadiusPx]、[FlyFanH.fanIconPx]；
 * 画法按用户要求分开：拖「扇区大小」→ 紫色扇形；拖「图标大小」→ 镂空圆线。
 *
 * ★ 2788：**坐标系必须用 View 自己的宽高**，不能用 `DisplayMetrics`：
 *   App 进程里 `getDisplayMetrics().heightPixels` 只有 2505（不含状态栏），
 *   而真菜单跑在 system_server，那里是 2640 —— 拿它当"屏幕底边"会把预览整体抬高 135px
 *   （用户报的"预览位置靠上了"就是这个）。
 */
public class FlyPrev extends android.view.View {

    /** 扇区大小的实时值（0 = 用保存值） */
    public static int lvScale;
    /** 图标大小的实时值（0 = 用保存值） */
    public static int lvIcon;
    public static com.oupao.wiz.FlyPrev one;

    private static final android.graphics.Paint FILL = new android.graphics.Paint();
    private static final android.graphics.Paint STROKE = new android.graphics.Paint();
    private static boolean paintsReady;

    public FlyPrev(android.content.Context p2) {
        super(p2);
        super.setFocusable(false);
        super.setClickable(false);
        ensurePaints();
    }

    private static void ensurePaints() {
        if (paintsReady) {
            return;
        }
        paintsReady = true;
        FILL.setAntiAlias(true);
        FILL.setStyle(android.graphics.Paint.Style.FILL);
        FILL.setColor(1299992063);
        STROKE.setAntiAlias(true);
        STROKE.setStyle(android.graphics.Paint.Style.STROKE);
        STROKE.setColor(-1719906817);
    }

    public static void setLive(int pct) {
        com.oupao.wiz.FlyPrev.lvScale = pct;
        com.oupao.wiz.FlyPrev.show();
    }

    public static void setLiveIcon(int pct) {
        com.oupao.wiz.FlyPrev.lvIcon = pct;
        com.oupao.wiz.FlyPrev.show();
    }

    public static void clear() {
        com.oupao.wiz.FlyPrev.lvScale = 0;
        com.oupao.wiz.FlyPrev.lvIcon = 0;
        com.oupao.wiz.FlyPrev.hide();
    }

    public static void hide() {
        com.oupao.wiz.FlyPrev v0 = null;
        try {
            v0 = com.oupao.wiz.FlyPrev.one;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            v0.setVisibility(8);
        }
    }

    public static void show() {
        android.view.ViewGroup host = null;
        try {
            if ((com.oupao.wiz.MainActivity.inst != null) && (com.oupao.wiz.MainActivity.inst.getWindow() != null)) {
                host = (android.view.ViewGroup) com.oupao.wiz.MainActivity.inst.getWindow().getDecorView();
            }
        } catch (Throwable ignore) {
        }
        if (host == null) {
            try {
                host = com.oupao.wiz.MainActivity.pageBox;
            } catch (Throwable __e_v0_1) {
                com.oupao.wiz.SLog.i(String.valueOf(__e_v0_1));
                return;
            }
        }
        if (host == null) {
            return;
        }
        com.oupao.wiz.FlyPrev v1_0 = com.oupao.wiz.FlyPrev.one;
        if (v1_0 == null) {
            v1_0 = new com.oupao.wiz.FlyPrev(host.getContext());
            com.oupao.wiz.FlyPrev.one = v1_0;
        }
        if (v1_0.getParent() != host) {
            if (v1_0.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) v1_0.getParent()).removeView(v1_0);
            }
            host.addView(v1_0, new android.view.ViewGroup.LayoutParams(-1, -1));
        }
        v1_0.setVisibility(0);
        v1_0.setTranslationZ(100.0f);
        v1_0.setElevation(100.0f);
        v1_0.bringToFront();
        v1_0.invalidate();
    }

    /** 旧的「紫色扇形」画法：从角落出发的 90° 扇形填充 + 描边 */
    private static void sector(android.graphics.Canvas cv, float cornerX, float cornerY, float r) {
        android.graphics.Path p = new android.graphics.Path();
        p.moveTo(cornerX, cornerY);
        android.graphics.RectF rf = new android.graphics.RectF(cornerX - r, cornerY - r, cornerX + r, cornerY + r);
        float start = (cornerX > 0.0f) ? 180.0f : -90.0f;
        p.arcTo(rf, start, 90.0f);
        p.close();
        cv.drawPath(p, FILL);
        STROKE.setStrokeWidth(6.0f);
        cv.drawPath(p, STROKE);
    }

    /** 固定应用列表：优先读 system_server 发布的快照（App 进程读不到 /data/system 配置） */
    private static String[] pinnedIcons(android.content.Context c) {
        java.util.ArrayList<String> raw = new java.util.ArrayList<>();
        try {
            String snap = com.oupao.wiz.Cfg.ggetC(c, "fan_pins_snapshot");
            if (snap != null && snap.length() > 0) {
                raw.addAll(com.oupao.wiz.Pins.parse(snap));
            }
        } catch (Throwable ignore) {
        }
        if (raw.isEmpty()) {
            try {
                raw.addAll(com.oupao.wiz.Pins.parse(com.oupao.wiz.Cfg.fanPins()));
            } catch (Throwable ignore) {
            }
        }
        java.util.ArrayList<String> only = new java.util.ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            if (!"@more".equals(com.oupao.wiz.FlyFan.pkgOf((String) raw.get(i)))) {
                only.add((String) raw.get(i));
            }
        }
        java.util.List<String> capped = com.oupao.wiz.Pins.cap5(only, only.size());
        return capped.toArray(new String[0]);
    }

    public void onDraw(android.graphics.Canvas p13) {
        try {
            android.content.Context c = this.getContext();
            int sc = com.oupao.wiz.FlyPrev.lvScale;
            if (sc == 0) {
                sc = com.oupao.wiz.Cfg.flyScale();
            }
            int ic = com.oupao.wiz.FlyPrev.lvIcon;
            if (ic == 0) {
                ic = com.oupao.wiz.Cfg.iconSize();
            }
            boolean iconMode = (com.oupao.wiz.FlyPrev.lvIcon != 0);
            float density = c.getResources().getDisplayMetrics().density;
            float rMax = com.oupao.wiz.FlyFanH.fanRadiusPx(c, sc);

            // ★ 2788：用 View 自己的宽高当"屏幕"，并直接在**本地坐标**里画（不再 translate）
            int vw = this.getWidth();
            int vh = this.getHeight();
            if ((vw <= 0) || (vh <= 0)) {
                return;
            }

            if (iconMode) {
                String[] ps = pinnedIcons(c);
                int n = (ps == null) ? 0 : ps.length;
                int total = n + 1;          // 真菜单里「更多应用」也占一格
                float dia = com.oupao.wiz.FlyFanH.fanIconPx(c, ic);   // 2790：与扇区无关
                float sw = Math.max(2.0f, 2.0f * density);
                com.oupao.wiz.SLog.i("FlyPrev: 圆线 n=" + n + "/" + total + " scale=" + sc + "% icon=" + ic
                        + "% r=" + ((int) rMax) + " dia=" + ((int) dia) + " view=" + vw + "x" + vh);
                STROKE.setStrokeWidth(sw);
                // 真菜单第 0 格是「更多」→ 固定应用占第 1..n 格
                int[] sides = new int[]{(com.oupao.wiz.Cfg.flyLeft() ? 1 : 0), (com.oupao.wiz.Cfg.flyRight() ? 2 : 0)};
                for (int k = 0; k < sides.length; k++) {
                    int side = sides[k];
                    if (side == 0) {
                        continue;
                    }
                    for (int i = 0; i < n; i++) {
                        int slot = i + 1;
                        double a = Math.toRadians((double) com.oupao.wiz.FlyFan.ang(slot, total));
                        float dx = (float) (Math.cos(a) * (double) rMax);
                        float dy = (float) (Math.sin(a) * (double) rMax);
                        float x = (side == 1) ? dx : ((float) vw - dx);
                        float y = (float) vh - dy;
                        p13.drawCircle(x, y, dia / 2.0f, STROKE);
                    }
                }
            } else {
                com.oupao.wiz.SLog.i("FlyPrev: 扇形 scale=" + sc + "% r=" + ((int) rMax)
                        + " view=" + vw + "x" + vh);
                if (com.oupao.wiz.Cfg.flyLeft()) {
                    sector(p13, 0.0f, (float) vh, rMax);
                }
                if (com.oupao.wiz.Cfg.flyRight()) {
                    sector(p13, (float) vw, (float) vh, rMax);
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.i("FlyPrev: " + t);
        }
    }
}
