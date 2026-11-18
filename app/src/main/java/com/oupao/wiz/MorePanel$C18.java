package com.oupao.wiz;

/** jumpTo 的子类：递归求目标 view 相对内容层的顶部偏移，然后平滑滚动到该位置 */
class MorePanel$C18 implements java.lang.Runnable {
    private final android.view.View target;
    private final android.view.View content;
    private final android.widget.ScrollView sv;

    MorePanel$C18(android.view.View p1, android.view.View p2, android.widget.ScrollView p3) {
        this.target = p1;
        this.content = p2;
        this.sv = p3;
        return;
    }

    public void run() {
        try {
            int v0 = 0;
            android.view.View v1 = this.target;
            while ((v1 != null) && (v1 != this.content)) {
                v0 += v1.getTop();
                android.view.ViewParent vp = v1.getParent();
                v1 = (vp instanceof android.view.View) ? (android.view.View) vp : null;
            }
            this.sv.smoothScrollTo(0, v0);
        } catch (Throwable t) {
        }
    }
}
