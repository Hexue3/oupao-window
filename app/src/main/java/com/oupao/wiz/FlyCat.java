package com.oupao.wiz;

/**
 * 全屏"接盘"窗口的 View：菜单展开期间由它吃掉整个屏幕的触摸。
 *
 * 除了原本的"点空白处收起菜单"，2724 起还多一个职责：
 * **吞掉菜单刚弹出那一瞬间落下来的点击**。
 *
 * 因为 ColorOS 底部 77px 拦截带里的点击是由桌面 consumer 重放给下层 App 的，
 * 这个重放可能在菜单弹出之后才落地 —— 不接住的话会穿透点到 App 的底部区域
 * （用户实测：QQ 主界面底部 tap 栏会被点掉一下）。所以这里有 [GRACE_MS] 的静默期：
 * 那段时间内收到的触摸只吞不处理，既不会穿透、也不会把刚弹出的菜单点掉。
 */
public class FlyCat extends android.view.View {

    /** 菜单刚弹出后的静默期（ms）：只吞事件，不做任何动作 */
    private static final long GRACE_MS = 500L;

    public FlyCat(android.content.Context p2) {
        super(p2);
        super.setWillNotDraw(false);
        super.setFocusable(true);
        super.setFocusableInTouchMode(true);
        return;
    }

    public boolean onKeyDown(int p2, android.view.KeyEvent p3) {
        try {
            if (p2 != 4) {
                return super.onKeyDown(p2, p3);
            } else {
                com.oupao.wiz.FlyH.close();
                return true;
            }
        } catch (Throwable __e_v0) {
            return true;
        }
    }

    public boolean onTouchEvent(android.view.MotionEvent p5) {
        int v0_0 = 0;
        try {
            v0_0 = p5.getActionMasked();
        } catch (Throwable __e_v0) {
            return true;
        }
        // 静默期：只吞不处理（挡掉"菜单刚弹出时穿透进来的那一下"）
        long since = android.os.SystemClock.uptimeMillis() - com.oupao.wiz.FlyH.shownAt;
        if (since < GRACE_MS) {
            return true;
        }
        if (v0_0 != 0) {
            if (v0_0 != 1) {
                com.oupao.wiz.FlyH.pressUp();
            } else {
                var v1_1 = p5.getRawX();
                var v2_1 = p5.getRawY();
                com.oupao.wiz.FlyH.pressUp();
                com.oupao.wiz.FlyH.tap(v1_1, v2_1);
            }
        } else {
            com.oupao.wiz.FlyH.pressDown(p5.getRawX(), p5.getRawY());
        }
        return true;
    }
}
