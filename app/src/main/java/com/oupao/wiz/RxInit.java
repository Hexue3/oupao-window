package com.oupao.wiz;
public final class RxInit implements java.lang.Runnable, java.util.concurrent.ThreadFactory {
    private static final long[] DELAYS;
    private static final com.oupao.wiz.RxInit TASK;
    private static boolean began;
    private static int fails;
    private static java.util.concurrent.ScheduledFuture pending;
    private static int retries;
    private static java.util.concurrent.ScheduledExecutorService worker;
    static
    {
        long[] v0_1 = new long[7];
        v0_1 = new long[]{1000L, 2000L, 4000L, 8000L, 15000L, 30000L, 60000L};
        DELAYS = v0_1;
        TASK = new com.oupao.wiz.RxInit();
    }
    private RxInit()
    {
        return;
    }
    /** 仅在 system_server / SystemUI / 本模块自身进程注册配置广播；
     *  第三方 App 进程里没有可用 AMS 代理，注册会抛 registerReceiverWithFeature NPE。 */
    private static boolean allowedProc()
    {
        try {
            byte[] b = new byte[128];
            java.io.FileInputStream fis = new java.io.FileInputStream("/proc/self/cmdline");
            int n = fis.read(b);
            fis.close();
            if (n <= 0) {
                return false;
            }
            String cmd = new String(b, 0, n).trim();
            int z = cmd.indexOf(0);
            if (z > 0) {
                cmd = cmd.substring(0, z);
            }
            return (cmd.startsWith("com.oupao.wiz"))
                    || (cmd.contains("system_server"))
                    || (cmd.contains("com.android.systemui"));
        } catch (Throwable t) {
            return false;
        }
    }
    private static boolean attempt(android.content.Context p9)
    {
        java.lang.String v9_1 = null;
        if (!allowedProc()) {
            com.oupao.wiz.SLog.r("rx-skip", "Rx: 非目标进程，跳过配置广播注册", 10000L);
            com.oupao.wiz.RxInit.stop();
            return true;
        }
        if (p9 != null) {
            try {
                p9.registerReceiver(new com.oupao.wiz.Rx(), com.oupao.wiz.RxInit.filter(), 2);
                v9_1 = "3";
            } catch (Throwable __e_v1_0) {
                com.oupao.wiz.RxInit.failure("3\u53c2", __e_v1_0);
                try {
                    p9.registerReceiver(new com.oupao.wiz.Rx(), com.oupao.wiz.RxInit.filter(), null, null, 2);
                    v9_1 = "5";
                } catch (Throwable v9_2) {
                    com.oupao.wiz.RxInit.failure("5\u53c2", v9_2);
                    return false;
                }
            }
            com.oupao.wiz.Rx.ok = true;
            com.oupao.wiz.RxInit.stop();
            com.oupao.wiz.Diag.put("RxError", "");
            com.oupao.wiz.RxInit.status(new StringBuilder().append("registered-").append(v9_1).toString());
            com.oupao.wiz.SLog.i(new StringBuilder().append("Rx: \u63a5\u6536\u5668\u5df2\u6ce8\u518c(").append(v9_1).append("\u53c2)\uff0c\u91cd\u8bd5\u6b21\u6570=").append(com.oupao.wiz.RxInit.retries).toString());
            try {
                com.oupao.wiz.Sug.boot();
            } catch (Throwable __e_v9_7) {
                com.oupao.wiz.SLog.e("Rx: \u901a\u9053\u5df2\u6ce8\u518c\uff0cSug.boot \u5931\u8d25", __e_v9_7);
            }
            return true;
        } else {
            return false;
        }
    }
    private static android.content.Context context(int p3)
    {
        try {
            if (p3 != 0) {
                if (p3 != 1) {
                    return com.oupao.wiz.Rx.ctxC();
                }
                return com.oupao.wiz.Rx.ctxB();
            }
            return com.oupao.wiz.Rx.ctxA();
        } catch (Throwable v0) {
            var v1 = new StringBuilder();
            v1.append("Context ");
            v1.append(p3);
            com.oupao.wiz.RxInit.failure(v1.toString(), v0);
            return null;
        }
    }
    private static boolean contexts()
    {
        var v1_0 = com.oupao.wiz.RxInit.context(0);
        if (!com.oupao.wiz.RxInit.attempt(v1_0)) {
            var v2_2 = com.oupao.wiz.RxInit.context(1);
            if ((v2_2 == v1_0) || (!com.oupao.wiz.RxInit.attempt(v2_2))) {
                var v4_2 = com.oupao.wiz.RxInit.context(2);
                if ((v4_2 == v1_0) || ((v4_2 == v2_2) || (!com.oupao.wiz.RxInit.attempt(v4_2)))) {
                    if ((v1_0 == null) && ((v2_2 == null) && (v4_2 == null))) {
                        com.oupao.wiz.Diag.put("RxError", "Context unavailable");
                        com.oupao.wiz.SLog.i("Rx: Context \u5c1a\u672a\u5c31\u7eea");
                    }
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
    }
    private static void failure(String p2, Throwable p3)
    {
        com.oupao.wiz.Diag.put("RxError", new StringBuilder().append(p2).append(": ").append(p3.toString()).toString());
        // 2906fix：开机时 IActivityManager 还是 null，这两条注册必定 NPE（后面会退避重试）。
        //   原来用 SLog.e 打完整堆栈，每次开机 4 条噪声；现在降级为限流提示。
        String msg = p3 == null ? "" : p3.toString();
        if (msg.contains("IActivityManager") || msg.contains("registerReceiverWithFeature")) {
            com.oupao.wiz.SLog.r("RxInit", "Rx: 广播注册尚未就绪(" + p2 + ")，会退避重试", 10000L);
        } else {
            com.oupao.wiz.SLog.e(new StringBuilder().append("Rx: ").append(p2).append(" \u5931\u8d25").toString(), p3);
        }
        return;
    }
    private static android.content.IntentFilter filter()
    {
        var v0_1 = new android.content.IntentFilter();
        var v1 = com.oupao.wiz.Ev.actions();
        var v2 = v1.length;
        int v3 = 0;
        while (v3 < v2) {
            v0_1.addAction(v1[v3]);
            v3++;
        }
        return v0_1;
    }
    public static synchronized void install()
    {
        try {
            if (!com.oupao.wiz.Rx.ok) {
                if (!com.oupao.wiz.RxInit.began) {
                    com.oupao.wiz.RxInit.began = true;
                    com.oupao.wiz.RxInit.status("registering");
                    if (!com.oupao.wiz.RxInit.contexts()) {
                        com.oupao.wiz.RxInit.schedule();
                    }
                    return;
                } else {
                }
            }
        } catch (Throwable __e_v1_2) {
            throw __e_v1_2;
        }
        return;
    }
    public static synchronized void lazy(Object p2)
    {
        try {
            if ((!com.oupao.wiz.Rx.ok) && (p2 != null)) {
                try {
                    android.content.Context v2_3 = null;
                    if (!(p2 instanceof android.content.Context)) {
                        v2_3 = com.oupao.wiz.Rx.ctxFrom(p2);
                        if (v2_3 != null) {
                            com.oupao.wiz.RxInit.register(v2_3);
                        }
                    } else {
                        v2_3 = ((android.content.Context) p2);
                    }
                } catch (Throwable __e_v2_1) {
                    com.oupao.wiz.RxInit.failure("lazy", __e_v2_1);
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v2_2) {
            throw __e_v2_2;
        }
    }
    public static synchronized void register(android.content.Context p4)
    {
        try {
            if (com.oupao.wiz.Rx.ok) {
                if (com.oupao.wiz.RxInit.fails < 5) {
                    com.oupao.wiz.RxInit.began = true;
                    if (com.oupao.wiz.RxInit.attempt(p4)) {
                        com.oupao.wiz.RxInit.fails = (com.oupao.wiz.RxInit.fails + 1);
                        com.oupao.wiz.RxInit.schedule();
                    }
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v4_1) {
            throw __e_v4_1;
        }
    }
    private static void schedule()
    {
        if (com.oupao.wiz.Rx.ok) {
            return;
        }
        if (com.oupao.wiz.RxInit.pending != null) {
            return;
        }
        if (com.oupao.wiz.RxInit.retries >= com.oupao.wiz.RxInit.DELAYS.length) {
            com.oupao.wiz.RxInit.stop();
            com.oupao.wiz.RxInit.status("exhausted");
            com.oupao.wiz.SLog.i("Rx: \u81ea\u52a8\u91cd\u8bd5\u5df2\u8fbe\u4e0a\u9650\uff0c\u901a\u9053\u672a\u6ce8\u518c\uff1b\u4fdd\u7559\u540e\u7eed lazy \u6ce8\u518c\u5165\u53e3");
            return;
        }
        try {
            if (com.oupao.wiz.RxInit.worker == null) {
                com.oupao.wiz.RxInit.worker = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(com.oupao.wiz.RxInit.TASK);
            }
            long v0 = com.oupao.wiz.RxInit.DELAYS[com.oupao.wiz.RxInit.retries];
            com.oupao.wiz.RxInit.pending = com.oupao.wiz.RxInit.worker.schedule(com.oupao.wiz.RxInit.TASK, v0, java.util.concurrent.TimeUnit.MILLISECONDS);
            com.oupao.wiz.RxInit.status("pending");
            com.oupao.wiz.SLog.i(new StringBuilder().append("Rx: \u6ce8\u518c\u672a\u6210\u529f\uff0c\u5df2\u5b89\u6392 ").append(v0).append("ms \u540e\u91cd\u8bd5").toString());
        } catch (Throwable v0_1) {
            com.oupao.wiz.RxInit.stop();
            com.oupao.wiz.RxInit.failure("schedule", v0_1);
            com.oupao.wiz.RxInit.status("schedule-failed");
        }
        return;
    }
    private static void status(String p2)
    {
        java.lang.String v0_3 = null;
        if (!com.oupao.wiz.Rx.ok) {
            v0_3 = "0";
        } else {
            v0_3 = "1";
        }
        com.oupao.wiz.Diag.put("Rx", v0_3);
        com.oupao.wiz.Diag.put("RxState", p2);
        com.oupao.wiz.Diag.put("RxRetries", String.valueOf(com.oupao.wiz.RxInit.retries));
        com.oupao.wiz.Diag.put("RxBuild", "1216");
        try {
            com.oupao.wiz.Diag.flush();
        } catch (Throwable __e_io) {
        }
        return;
    }
    private static void stop()
    {
        if (com.oupao.wiz.RxInit.pending != null) {
            com.oupao.wiz.RxInit.pending.cancel(false);
            com.oupao.wiz.RxInit.pending = null;
        }
        if (com.oupao.wiz.RxInit.worker != null) {
            com.oupao.wiz.RxInit.worker.shutdown();
            com.oupao.wiz.RxInit.worker = null;
        }
        return;
    }
    public Thread newThread(Runnable p3)
    {
        var v0_1 = new Thread(p3, "Oupao-RxInit");
        v0_1.setDaemon(true);
        return v0_1;
    }
    public void run()
    {
        try {
            com.oupao.wiz.RxInit.pending = null;
        } catch (Throwable __e_v1_2) {
            throw __e_v1_2;
        }
        if (!com.oupao.wiz.Rx.ok) {
            com.oupao.wiz.RxInit.retries = (com.oupao.wiz.RxInit.retries + 1);
            com.oupao.wiz.RxInit.status("retrying");
            if (!com.oupao.wiz.RxInit.contexts()) {
                com.oupao.wiz.RxInit.schedule();
            }
            return;
        } else {
            com.oupao.wiz.RxInit.stop();
            return;
        }
    }
}