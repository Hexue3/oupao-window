package com.oupao.wiz;
 class MainActivity$C1 implements android.widget.CompoundButton.OnCheckedChangeListener {
    private final String key;
    final com.oupao.wiz.MainActivity this$0;
    MainActivity$C1(com.oupao.wiz.MainActivity p1, String p2)
    {
        this.this$0 = p1;
        this.key = p2;
        return;
    }
    public void onCheckedChanged(android.widget.CompoundButton p7, boolean p8)
    {
        java.lang.String v3_1 = null;
        com.oupao.wiz.MainActivity v0 = null;
        java.lang.Object v1 = null;
        try {
            v0 = ((com.oupao.wiz.MainActivity) this.this$0);
            v1 = this.key;
            com.oupao.wiz.Cfg.save((java.lang.String) v1, p8);
        } catch (Throwable __e_v0) {
            return;
        }
        if (!"oupao_egg_master".equals(v1)) {
            v3_1 = "\u7acb\u5373\u751f\u6548";
        } else {
            v3_1 = "\u5df2\u4fdd\u5b58\uff0c\u7acb\u5373\u751f\u6548";
        }
        android.widget.Toast.makeText(v0, v3_1, 0).show();
        return;
    }
}