package com.oupao.wiz;
public class PinTab implements android.view.View.OnClickListener {
    public PinTab()
    {
        return;
    }
    public void onClick(android.view.View p7)
    {
        java.lang.Object v0_0 = null;
        try {
            v0_0 = p7.getTag();
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 instanceof Integer)) {
            var v1_1 = ((Integer) v0_0).intValue();
            // 1820fix: smali 是先 sget 旧值再 sput，然后用「新值 vs 旧值」决定动画方向；
            // 这里原来先写后读，比较恒真，切换动画方向永远是 -24。
            int oldTab = com.oupao.wiz.Pins.tab;
            com.oupao.wiz.Pins.tab = v1_1;
            var v3 = p7.getContext();
            if (v3 != null) {
                com.oupao.wiz.Pins.styleTabs();
                com.oupao.wiz.Pins.buildTab();
                var v4_0 = com.oupao.wiz.Pins.tabBox;
                if (v4_0 != null) {
                    int v5_1 = 0;
                    v4_0.setAlpha(0);
                    if (v1_1 <= oldTab) {
                        v5_1 = -24;
                    } else {
                        v5_1 = 24;
                    }
                    v4_0.setTranslationX(((float) com.oupao.wiz.Ui.dp(v3, v5_1)));
                    var v4_1 = v4_0.animate();
                    v4_1.alpha(1.0f);
                    v4_1.translationX(0);
                    v4_1.setDuration(200);
                    v4_1.start();
                }
            }
        }
        return;
    }
}