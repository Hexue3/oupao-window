package com.oupao.wiz;

/**
 * 3079：ROM 的「输入法手势守卫」——`FlexiblePointerHandler#isPhoneLandScreenAndImeShowing()`。
 *
 * ## 为什么挂它
 * 键盘弹起时 ROM 会把标题栏上滑当成"输入法相关手势"（这个守卫返回 true → 缩放手势/迷你窗分支
 * 直接被跳过），这就是「开了键盘，上滑横条就不转迷你窗」的真因；没有键盘时同一个手势能正常转迷你。
 *
 * ## 做法
 * 用户刚在**键盘弹起状态**下上滑横条时（`Hooks.onGestureMode` 里记一个 ~900ms 的窗口
 * `Hooks.imeMiniUntil`），这段时间内把这个守卫压成 false，让 ROM 走它自己的迷你窗缩放手势管线。
 * 窗口外一律不动（其余输入法行为完全保持原样）。
 */
public class HookImeGuard extends com.oupao.wiz.XMH {

    public HookImeGuard() {
        return;
    }

    public void beforeHookedMethod(com.oupao.wiz.XMP p) {
        try {
            if (com.oupao.wiz.Hooks.imeMiniUntil > android.os.SystemClock.uptimeMillis()) {
                p.setResult(java.lang.Boolean.FALSE);
                p.returnEarly = true;
            }
        } catch (Throwable ignore) {
        }
    }
}
