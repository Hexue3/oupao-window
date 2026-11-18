package com.oupao.wiz;
public class BtCb extends android.media.AudioDeviceCallback {
    public BtCb()
    {
        return;
    }
    public static boolean inCall()
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.ctx;
        } catch (Throwable __e_v0) {
            return false;
        }
        if (v0_0 != null) {
            var v0_5 = ((android.media.AudioManager) v0_0.getSystemService("audio"));
            if ((v0_5 != null) && (v0_5.getMode() != 0)) {
                return true;
            }
        }
        return false;
    }
    public static boolean isBt(int p1)
    {
        if (p1 != 7) {
            if (p1 != 8) {
                if (p1 != 26) {
                    if (p1 != 27) {
                        if (p1 != 30) {
                            if (p1 != 23) {
                                if (p1 != 22) {
                                    if (p1 != 11) {
                                        if (p1 != 12) {
                                            if (p1 != 3) {
                                                if (p1 != 4) {
                                                    return false;
                                                } else {
                                                    return true;
                                                }
                                            } else {
                                                return true;
                                            }
                                        } else {
                                            return true;
                                        }
                                    } else {
                                        return true;
                                    }
                                } else {
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
    public void onAudioDevicesAdded(android.media.AudioDeviceInfo[] p7)
    {
        try {
            if (p7 != null) {
                var v0 = p7.length;
                int v1 = 0;
                while (v1 < v0) {
                    var v2 = p7[v1];
                    if (v2 != null) {
                        var v3_4 = v2.getType();
                        var v4_0 = new StringBuilder();
                        v4_0.append("Bt: \u97f3\u9891\u8bbe\u5907 type=");
                        v4_0.append(v3_4);
                        com.oupao.wiz.SLog.i(v4_0.toString());
                        if (com.oupao.wiz.BtCb.isBt(v3_4)) {
                            com.oupao.wiz.SLog.i("Bt: \u68c0\u6d4b\u5230\u84dd\u7259\u97f3\u9891\u8f93\u51fa\u8bbe\u5907");
                            if (!com.oupao.wiz.BtCb.inCall()) {
                                com.oupao.wiz.Sug.onBt();
                                return;
                            } else {
                                com.oupao.wiz.SLog.i("Bt: \u901a\u8bdd\u4e2d\uff0c\u8df3\u8fc7\u5f39\u6c14\u6ce1");
                                return;
                            }
                        }
                    }
                    v1++;
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public void onAudioDevicesRemoved(android.media.AudioDeviceInfo[] p1)
    {
        return;
    }
}