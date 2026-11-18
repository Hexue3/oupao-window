package com.oupao.wiz;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * 面板 / 小窗页「自绘拖动排序」——**不使用系统 startDragAndDrop**。
 *
 * ★ 为什么不能用系统拖拽
 * 面板跑在 **system_server** 进程里，系统拖拽的阴影（DragShadow）Surface 由
 * WindowManager（也就是 system_server 自己）管理。只要在拖拽过程中销毁或搬动源 View，
 * 那块 surface 就会在会话结束前被 release，随后 `DragState.closeLocked` 去 reparent
 * 一个已释放的 surface → NPE → system_server 崩 → 手机重启。
 *
 * ★ 做法
 * - 长按后**不动原格子**，只把它压暗表示「原位置」；
 * - 用 `cell.draw()` 生成一张位图副本（ghost），加到**最近的 FrameLayout 祖先**
 *   （页面容器 / 面板根），用 `translation` 跟着手指走。
 *   → 永远画在最上层（不会「压在目标图标下面」），
 *     也不会被中间那层 ScrollView 裁掉（竖向拖动图标消失的问题）。
 * - 拖动过程中不判定、不换位；
 * - **松手时**才按「ghost 中心离哪一格最近」决定落点，再交给 commit 落位。
 */
public class PinDrag {

    /** 落位回调：把 from 移到 to */
    public interface Commit {
        void onMove(String from, String to);
    }

    private static View cell;        // 原格子（不动，只压暗）
    private static View ghost;       // 跟手的浮层副本
    private static ViewGroup gridRef;
    private static String fromTag;
    private static float baseRawX, baseRawY;
    private static boolean haveBase;
    private static boolean movedEnough;
    private static float minMove;
    /** ghost 建不出来时退回「直接平移格子」，并临时关掉父容器裁剪 */
    private static boolean fallback;
    private static java.util.ArrayList<ViewGroup> clipOff = new java.util.ArrayList<ViewGroup>();

    public static void attach(final ViewGroup grid, final View v, final Commit commit) {
        attach(grid, v, v, true, commit);
    }

    /** 不需要「管理」模式就能拖（小窗页 pgPins 用这个） */
    public static void attach(final ViewGroup grid, final View touchView, final View cell,
                              final Commit commit) {
        attach(grid, touchView, cell, false, commit);
    }

    /**
     * @param touchView 真正持有触摸流的视图（小窗页可能是 badge）
     * @param cell      被拖的那个格子
     * @param needMgr   是否要求处于「管理」模式
     */
    public static void attach(final ViewGroup grid, final View touchView, final View cell,
                              final boolean needMgr, final Commit commit) {
        if ((touchView == null) || (cell == null) || (grid == null)) {
            return;
        }
        touchView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View cv) {
                try {
                    if (needMgr && (!com.oupao.wiz.Pins.appMgr)) {
                        return false;
                    }
                    start(grid, cell);
                    return true;
                } catch (Throwable t) {
                    return false;
                }
            }
        });
        touchView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View tv, MotionEvent e) {
                if (PinDrag.cell != cell) {
                    return false;
                }
                int a = e.getActionMasked();
                if (a == MotionEvent.ACTION_MOVE) {
                    move(e.getRawX(), e.getRawY());
                    return true;
                }
                if ((a == MotionEvent.ACTION_UP) || (a == MotionEvent.ACTION_CANCEL)) {
                    end(commit, a == MotionEvent.ACTION_UP);
                    return true;
                }
                return true;
            }
        });
    }

    private static void start(ViewGroup grid, View cv) {
        cell = cv;
        gridRef = grid;
        fromTag = String.valueOf(cv.getTag());
        haveBase = false;
        movedEnough = false;
        try {
            minMove = Math.max(com.oupao.wiz.Ui.dp(cv.getContext(), 24), cv.getWidth() * 0.30f);
        } catch (Throwable ignore) {
            minMove = 60.0f;
        }
        // 拖动期间禁止父容器（ScrollView）拦截手势，否则超过 touch slop 就收到 CANCEL
        try {
            android.view.ViewParent vp = cv.getParent();
            int guard = 0;
            while ((vp != null) && (guard++ < 32)) {
                vp.requestDisallowInterceptTouchEvent(true);
                vp = vp.getParent();
            }
        } catch (Throwable ignore) {
        }
        try {
            cv.setAlpha(0.35f);
        } catch (Throwable ignore) {
        }
        ghost = makeGhost(cv);
        fallback = (ghost == null);
        if (fallback) {
            // 兜底：直接平移原格子，并把父链裁剪关掉（否则竖向拖动会被 ScrollView 裁没）
            try {
                clipOff.clear();
                var vp3 = cv.getParent();
                int g3 = 0;
                while ((vp3 instanceof ViewGroup) && (g3++ < 32)) {
                    ViewGroup vg = (ViewGroup) vp3;
                    if (vg.getClipChildren()) {
                        vg.setClipChildren(false);
                        clipOff.add(vg);
                    }
                    vp3 = vg.getParent();
                }
            } catch (Throwable ignore) {
            }
            try {
                cv.setElevation(com.oupao.wiz.Ui.dp(cv.getContext(), 12));
            } catch (Throwable ignore) {
            }
        }
        com.oupao.wiz.SLog.i("PinDrag: \u5f00\u59cb\u62d6\u52a8 " + fromTag
                + " ghost=" + (fallback ? "null(fallback)" : "ok"));
    }

    /** 生成跟手的浮层副本，加到最近的 FrameLayout 祖先（页面容器 / 面板根） */
    private static View makeGhost(View cv) {
        try {
            ViewGroup host = hostOf(cv);
            if (host == null) {
                return null;
            }
            int w = cv.getWidth();
            int h = cv.getHeight();
            if ((w <= 0) || (h <= 0)) {
                return null;
            }
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            cv.draw(new Canvas(bmp));
            ImageView iv = new ImageView(cv.getContext());
            iv.setImageBitmap(bmp);
            iv.setAlpha(0.95f);

            int[] cl = new int[2];
            int[] hl = new int[2];
            cv.getLocationOnScreen(cl);
            host.getLocationOnScreen(hl);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
            lp.leftMargin = cl[0] - hl[0];
            lp.topMargin = cl[1] - hl[1];
            host.addView(iv, lp);
            return iv;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("PinDrag: ghost \u5931\u8d25", t);
            return null;
        }
    }

    private static ViewGroup hostOf(View v) {
        try {
            var p = v.getParent();
            int guard = 0;
            while ((p instanceof View) && (guard++ < 32)) {
                // ★ 必须跳过 ScrollView：它是 FrameLayout 的子类，但只能有一个直接子 View，
                //   往里 addView 会抛 "ScrollView can host only one direct child"。
                if ((p instanceof FrameLayout) && (!isScroller((View) p))) {
                    return (FrameLayout) p;
                }
                p = ((View) p).getParent();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** ScrollView / HorizontalScrollView / NestedScrollView 都算（它们都是 FrameLayout 子类） */
    private static boolean isScroller(View v) {
        try {
            String n = v.getClass().getName();
            return (n != null) && n.contains("ScrollView");
        } catch (Throwable ignore) {
        }
        return false;
    }

    /** 只让浮层跟手，不做任何换位判定 */
    private static void move(float rawX, float rawY) {
        try {
            if (!haveBase) {
                baseRawX = rawX;
                baseRawY = rawY;
                haveBase = true;
            }
            float tx = rawX - baseRawX;
            float ty = rawY - baseRawY;
            if (ghost != null) {
                ghost.setTranslationX(tx);
                ghost.setTranslationY(ty);
            } else if (fallback && (cell != null)) {
                cell.setTranslationX(tx);
                cell.setTranslationY(ty);
            }
            if ((Math.abs(tx) >= minMove) || (Math.abs(ty) >= minMove)) {
                movedEnough = true;
            }
        } catch (Throwable ignore) {
        }
    }

    /** 松手：按 ghost 中心找最近的格子，再交给 commit 落位 */
    private static void end(Commit commit, boolean commitIt) {
        try {
            View cv = cell;
            ViewGroup grid = gridRef;
            String from = fromTag;
            String to = null;
            float tx = 0.0f;
            float ty = 0.0f;
            if (ghost != null) {
                tx = ghost.getTranslationX();
                ty = ghost.getTranslationY();
            } else if (fallback && (cv != null)) {
                tx = cv.getTranslationX();
                ty = cv.getTranslationY();
            }
            boolean ok = commitIt && movedEnough && (cv != null) && (grid != null);
            if (ok) {
                View target = nearest(grid, cv, tx, ty);
                if ((target != null) && (target != cv)) {
                    Object tt = target.getTag();
                    if (tt instanceof String) {
                        to = (String) tt;
                    }
                }
            }
            // 收尾：移除浮层、还原原格子、放开父容器拦截
            try {
                if (ghost != null) {
                    ViewGroup hp = (ghost.getParent() instanceof ViewGroup)
                            ? (ViewGroup) ghost.getParent() : null;
                    if (hp != null) {
                        hp.removeView(ghost);
                    }
                }
            } catch (Throwable ignore) {
            }
            ghost = null;
            if (cv != null) {
                try {
                    cv.setAlpha(1.0f);
                    cv.setTranslationX(0.0f);
                    cv.setTranslationY(0.0f);
                    cv.setElevation(0.0f);
                } catch (Throwable ignore) {
                }
                try {
                    for (int i = 0; i < clipOff.size(); i++) {
                        clipOff.get(i).setClipChildren(true);
                    }
                    clipOff.clear();
                } catch (Throwable ignore) {
                }
                try {
                    android.view.ViewParent vp = cv.getParent();
                    int guard = 0;
                    while ((vp != null) && (guard++ < 32)) {
                        vp.requestDisallowInterceptTouchEvent(false);
                        vp = vp.getParent();
                    }
                } catch (Throwable ignore) {
                }
            }
            cell = null;
            gridRef = null;
            fromTag = null;
            haveBase = false;
            movedEnough = false;
            if ((from != null) && (to != null) && (!from.equals(to))) {
                com.oupao.wiz.SLog.i("PinDrag: \u843d\u4f4d " + from + " -> " + to);
                if (commit != null) {
                    commit.onMove(from, to);
                }
            } else {
                com.oupao.wiz.SLog.i("PinDrag: \u677e\u624b\u4e0d\u6362\u4f4d from=" + from + " to=" + to);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("PinDrag: end \u5f02\u5e38", t);
        }
    }

    /** 中心离「被拖格子平移后的虚拟中心」最近的那一格（排除自身） */
    private static View nearest(ViewGroup grid, View cv, float tx, float ty) {
        try {
            java.util.ArrayList<View> cells = new java.util.ArrayList<View>();
            collect(grid, cells);
            float[] c = centerIn(grid, cv);
            float cx = c[0] + tx;
            float cy = c[1] + ty;
            View best = null;
            float bestD = Float.MAX_VALUE;
            for (int i = 0; i < cells.size(); i++) {
                View v = cells.get(i);
                if (v == cv) {
                    continue;
                }
                float[] vc = centerIn(grid, v);
                float ddx = vc[0] - cx;
                float ddy = vc[1] - cy;
                float d = (ddx * ddx) + (ddy * ddy);
                if (d < bestD) {
                    bestD = d;
                    best = v;
                }
            }
            return best;
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** 把某格子的中心换算到 grid 坐标系 */
    private static float[] centerIn(ViewGroup grid, View v) {
        float x = v.getWidth() / 2.0f;
        float y = v.getHeight() / 2.0f;
        View cur = v;
        int guard = 0;
        while ((cur != null) && (cur != grid) && (guard++ < 32)) {
            x += cur.getLeft();
            y += cur.getTop();
            var p = cur.getParent();
            cur = (p instanceof View) ? (View) p : null;
        }
        return new float[]{x, y};
    }

    /** 收集所有带 String tag 的格子 */
    private static void collect(ViewGroup g, java.util.ArrayList<View> out) {
        int n = g.getChildCount();
        for (int i = 0; i < n; i++) {
            View c = g.getChildAt(i);
            if (c == null) {
                continue;
            }
            if (c.getTag() instanceof String) {
                out.add(c);
            } else if (c instanceof ViewGroup) {
                collect((ViewGroup) c, out);
            }
        }
    }
}
