package com.oupao.wiz;

/**
 * 气泡位置：竖屏/横屏各存一份，互不干扰。
 *
 * 存的格式仍是屏幕绝对像素 "x,y"（与 MainActivity.savePos 里 getLocationOnScreen 的坐标系一致）。
 * 之所以按方向分家：两个方向屏幕尺寸差一倍（如 1216x2640 / 2640x1216），
 * 同一个像素坐标换方向后必然跑到屏幕外，所以不能用一份值硬套。
 *
 * 键：
 *   oupao_bub_pos       竖屏（历史键，向后兼容，老用户不用重新设）
 *   oupao_bub_pos_land  横屏（新增）
 */
public class BubPos {

    public static final String KEY_PORT = "oupao_bub_pos";
    public static final String KEY_LAND = "oupao_bub_pos_land";

    /**
     * 原始真实像素尺寸。
     * 注意：system_server 里 Display.getRealMetrics() 有时返回的是【物理自然方向】的尺寸
     * （竖屏时也给 2640x1216），所以**不能**用它来判断方向。
     */
    static int[] rawScreen(android.content.Context c) {
        int[] r = new int[2];
        if (c == null) {
            return r;
        }
        try {
            var wm = ((android.view.WindowManager) c.getSystemService("window"));
            if (wm != null) {
                var d = wm.getDefaultDisplay();
                if (d != null) {
                    var dm = new android.util.DisplayMetrics();
                    d.getRealMetrics(dm);
                    r[0] = dm.widthPixels;
                    r[1] = dm.heightPixels;
                    if ((r[0] > 0) && (r[1] > 0)) {
                        return r;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            var dm2 = c.getResources().getDisplayMetrics();
            r[0] = dm2.widthPixels;
            r[1] = dm2.heightPixels;
        } catch (Throwable ignore) {
        }
        return r;
    }

    /**
     * 当前是否横屏。
     * 优先用 Configuration.orientation（跟随显示配置），其次 Display.getRotation()，
     * 最后才退回宽高比较 —— 单用宽高会被上面的"自然方向尺寸"坑到。
     */
    public static boolean land(android.content.Context c) {
        if (c == null) {
            return false;
        }
        try {
            int o = c.getResources().getConfiguration().orientation;
            if (o == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                return true;
            }
            if (o == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                return false;
            }
        } catch (Throwable ignore) {
        }
        try {
            var wm = ((android.view.WindowManager) c.getSystemService("window"));
            if (wm != null) {
                var d = wm.getDefaultDisplay();
                if (d != null) {
                    int r = d.getRotation();
                    if ((r == 1) || (r == 3)) {
                        return true;
                    }
                    if (r == 0) {
                        return false;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        var s = rawScreen(c);
        return s[0] > s[1];
    }

    /** 当前方向的屏幕尺寸（用 land() 把宽高校正过来，供 clamp 用） */
    public static int[] screen(android.content.Context c) {
        int[] r = rawScreen(c);
        try {
            boolean l = land(c);
            if (l && (r[0] < r[1])) {
                int t = r[0];
                r[0] = r[1];
                r[1] = t;
            } else if ((!l) && (r[0] > r[1])) {
                int t = r[0];
                r[0] = r[1];
                r[1] = t;
            }
        } catch (Throwable ignore) {
        }
        return r;
    }

    /** 当前方向该用哪个键 */
    public static String key(android.content.Context c) {
        return land(c) ? KEY_LAND : KEY_PORT;
    }

    /** 当前方向的中文名，给编辑器标题用 */
    public static String label(android.content.Context c) {
        return land(c) ? "横屏位置" : "竖屏位置";
    }

    /** 当前方向已保存的原始值（绕过 Cfg 缓存，App / system_server 两边都能读） */
    public static String raw(android.content.Context c) {
        try {
            return com.oupao.wiz.Cfg.getStrFresh(key(c), "");
        } catch (Throwable t) {
            return "";
        }
    }

    /** 解析 "x,y"；非法（含旧的 "-1,-1"）返回 null */
    public static int[] parse(String v) {
        try {
            if (v == null) {
                return null;
            }
            int i = v.indexOf(',');
            if (i <= 0) {
                return null;
            }
            int x = Integer.parseInt(v.substring(0, i).trim());
            int y = Integer.parseInt(v.substring(i + 1).trim());
            if ((x < 0) || (y < 0)) {
                return null;
            }
            int[] r = new int[2];
            r[0] = x;
            r[1] = y;
            return r;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 把坐标收进当前屏幕可见范围（size = 气泡边长 px），防止另一个方向的旧值把气泡甩到屏幕外 */
    public static int[] clamp(android.content.Context c, int[] xy, int size) {
        if ((c == null) || (xy == null)) {
            return xy;
        }
        var s = screen(c);
        int maxX = s[0] - size;
        int maxY = s[1] - size;
        if (maxX < 0) {
            maxX = 0;
        }
        if (maxY < 0) {
            maxY = 0;
        }
        int[] r = new int[2];
        r[0] = Math.min(Math.max(xy[0], 0), maxX);
        r[1] = Math.min(Math.max(xy[1], 0), maxY);
        return r;
    }
}
