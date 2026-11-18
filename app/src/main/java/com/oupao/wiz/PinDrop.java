package com.oupao.wiz;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 小窗固定应用页的拖拽排序接收器。
 *
 * ★★ 关键约束（踩过两次，务必保持）★★
 * 面板/本页都跑在 **system_server** 进程里，拖动阴影（DragShadow）的 Surface 由
 * WindowManager（也就是 system_server 自己）管理。如果在 `ACTION_DROP` 回调里
 * **同步** removeView / addView 拖拽源 View（或重建整页），那块 Surface 会在拖拽会话
 * 结束前被 release，随后 `DragState.closeLocked` 去 reparent 一个已释放的 surface
 * → NPE → system_server 崩 → 手机重启。
 *
 * 所以这里：
 *   1) `ACTION_DROP` 只**记录**「谁拖到谁身上」；
 *   2) 等 `ACTION_DRAG_ENDED`（拖拽会话真正结束、阴影已回收）再 post 到主线程执行重排。
 *
 * 小窗的网格结构（PgGrid）：
 *     卡片 LinearLayout
 *       └─ 行 LinearLayout（每行 4 个）  ← PgGrid.grow
 *            └─ 格子 LinearLayout（带 tag）  ← 拖拽绑在这里
 */
public class PinDrop implements android.view.View.OnDragListener {

    /** 待处理的拖拽结果（ACTION_DROP 记录，ACTION_DRAG_ENDED 执行） */
    private static String pendFrom;
    private static String pendTo;
    private static View pendSrc;

    public PinDrop() {
        return;
    }

    public boolean onDrag(android.view.View p10, android.view.DragEvent p11) {
        int action = 0;
        try {
            action = p11.getAction();
        } catch (Throwable t) {
            return true;
        }

        if (action == 5) {                 // ACTION_DROP：只记录，别动 View
            try {
                var local = p11.getLocalState();
                if (local instanceof View) {
                    var src = (View) local;
                    Object st = src.getTag();
                    Object dt = p10.getTag();
                    if ((src != p10) && (st instanceof String) && (dt instanceof String)) {
                        pendFrom = (String) st;
                        pendTo = (String) dt;
                        pendSrc = src;
                    }
                }
            } catch (Throwable t) {
                pendFrom = null;
                pendTo = null;
                pendSrc = null;
            }
        } else if (action == 4) {          // ACTION_DRAG_ENDED：会话结束，安全了
            final String f = pendFrom;
            final String t2 = pendTo;
            final View src = pendSrc;
            pendFrom = null;
            pendTo = null;
            pendSrc = null;
            if ((f != null) && (t2 != null)) {
                Runnable job = new Runnable() {
                    public void run() {
                        try {
                            var order = com.oupao.wiz.Pins.swapOrder(f, t2);
                            com.oupao.wiz.Pins.save(com.oupao.wiz.Pins.join(order));
                            reorderViews(order, src);
                        } catch (Throwable t) {
                            com.oupao.wiz.SLog.e("PinDrop: \u91cd\u6392\u5f02\u5e38", t);
                        }
                    }
                };
                // 再等一帧，确保拖拽阴影彻底回收
                try {
                    p10.postDelayed(job, 60L);
                } catch (Throwable t) {
                    job.run();
                }
            }
        }

        if (action != 2) {                // ACTION_DRAG_ENTERED
            try {
                p10.setAlpha(1.0f);
            } catch (Throwable ignore) {
            }
        } else {
            try {
                p10.setAlpha(0.35f);
            } catch (Throwable ignore) {
            }
        }
        return true;
    }

    /**
     * 供自绘拖动（PinDrag）落位用：算新顺序 → 存盘 → 就地重排视图。
     */
    public static void applyOrder(String from, String to, View anyCell) {
        try {
            if ((from == null) || (to == null) || from.equals(to)) {
                return;
            }
            var order = com.oupao.wiz.Pins.swapOrder(from, to);
            com.oupao.wiz.Pins.save(com.oupao.wiz.Pins.join(order));
            reorderViews(order, anyCell);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("PinDrop: applyOrder \u5f02\u5e38", t);
        }
    }

    /** 按新的 tag 顺序，把已有的格子 view 重新放回各行（不新建、不销毁）。 */
    private static void reorderViews(ArrayList<String> order, View anyCell) {
        try {
            if ((order == null) || (order.size() == 0) || (anyCell == null)) {
                return;
            }
            ViewGroup row0 = (anyCell.getParent() instanceof ViewGroup)
                    ? (ViewGroup) anyCell.getParent() : null;
            if (row0 == null) {
                return;
            }
            ViewGroup card = (row0.getParent() instanceof ViewGroup)
                    ? (ViewGroup) row0.getParent() : null;
            if (card == null) {
                return;
            }
            // 1) 收集所有行（按在卡片里的顺序）与每行的格子数
            ArrayList<ViewGroup> rows = new ArrayList<ViewGroup>();
            ArrayList<Integer> counts = new ArrayList<Integer>();
            HashMap<String, View> byTag = new HashMap<String, View>();
            int cardCount = card.getChildCount();
            for (int i = 0; i < cardCount; i++) {
                View cv = card.getChildAt(i);
                if (!(cv instanceof ViewGroup)) {
                    continue;
                }
                ViewGroup rg = (ViewGroup) cv;
                int n = 0;
                int rc = rg.getChildCount();
                for (int j = 0; j < rc; j++) {
                    View c = rg.getChildAt(j);
                    Object t = c.getTag();
                    if (t instanceof String) {
                        byTag.put((String) t, c);
                        n++;
                    }
                }
                if (n > 0) {
                    rows.add(rg);
                    counts.add(Integer.valueOf(n));
                }
            }
            if (rows.isEmpty()) {
                return;
            }
            // 2) 按新顺序取出对应的 view
            ArrayList<View> ordered = new ArrayList<View>();
            ArrayList<android.view.ViewGroup.LayoutParams> lps =
                    new ArrayList<android.view.ViewGroup.LayoutParams>();
            for (int i = 0; i < order.size(); i++) {
                View v = byTag.get(order.get(i));
                if (v != null) {
                    ordered.add(v);
                    lps.add(v.getLayoutParams());
                }
            }
            if (ordered.isEmpty()) {
                return;
            }
            // 3) 先全部摘下来
            for (int i = 0; i < ordered.size(); i++) {
                View v = ordered.get(i);
                ViewGroup vp = (v.getParent() instanceof ViewGroup)
                        ? (ViewGroup) v.getParent() : null;
                if (vp != null) {
                    vp.removeView(v);
                }
            }
            // 4) 按原来的行容量放回去（形状不变）
            int idx = 0;
            for (int r = 0; r < rows.size(); r++) {
                int n = counts.get(r).intValue();
                for (int k = 0; k < n; k++) {
                    if (idx >= ordered.size()) {
                        break;
                    }
                    View v = ordered.get(idx);
                    android.view.ViewGroup.LayoutParams lp = lps.get(idx);
                    idx++;
                    try {
                        if (lp != null) {
                            rows.get(r).addView(v, lp);
                        } else {
                            rows.get(r).addView(v);
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("PinDrop: reorderViews \u5f02\u5e38", t);
        }
    }
}
