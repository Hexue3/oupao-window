package com.oupao.wiz;
public class Diag$Recv extends android.content.BroadcastReceiver {
    public Diag$Recv()
    {
        return;
    }
    public void onReceive(android.content.Context p5, android.content.Intent p6)
    {
        try {
            var v2_1 = new StringBuilder();
            v2_1.append("Diag: recv ");
            v2_1.append(p6.getAction());
            com.oupao.wiz.SLog.i(v2_1.toString());
            var v3_0 = p6.getStringExtra("data");
            if ((v3_0 == null) || (v3_0.length() == 0)) {
                // 实测：回包能收到，但 data 里可能是空的 -> 卡片会一直停在"正在读取模块状态"。
                // 这里退回读落盘文件（system 侧 onReq 现在会写 /data/system/oupao_diag）。
                var v4_0 = new StringBuilder();
                com.oupao.wiz.Diag.readAll(v4_0);
                com.oupao.wiz.SLog.i("Diag: recv extra \u4e3a\u7a7a, \u56de\u9000\u8bfb\u6587\u4ef6 len=" + v4_0.length());
                v3_0 = (v4_0.length() > 0) ? v4_0.toString() : null;
            } else {
                com.oupao.wiz.SLog.i("Diag: recv extra len=" + v3_0.length());
            }
            com.oupao.wiz.Diag.appData = v3_0;
            com.oupao.wiz.Diag.refresh();
            // 注意：原来这里还有 this.goAsync().finish()。
            // 动态注册的接收器上 goAsync() 可能返回 null，会抛 NPE（被 catch 吞掉），去掉。
        } catch (Throwable __e___ex) {
        }
        return;
    }
}
