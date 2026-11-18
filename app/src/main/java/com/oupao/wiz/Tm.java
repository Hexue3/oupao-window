package com.oupao.wiz;
public class Tm {
    public static int aN;
    public static long aS;
    public static long aT;
    public static long t0;
    public Tm()
    {
        return;
    }
    public static void enter()
    {
        com.oupao.wiz.Tm.aS = android.os.SystemClock.uptimeMillis();
        com.oupao.wiz.Tm.aN = (com.oupao.wiz.Tm.aN + 1);
        return;
    }
    public static void exit()
    {
        com.oupao.wiz.Tm.aT = (com.oupao.wiz.Tm.aT + (android.os.SystemClock.uptimeMillis() - com.oupao.wiz.Tm.aS));
        return;
    }
    public static void log(String p5)
    {
        try {
            var v0_2 = (android.os.SystemClock.uptimeMillis() - com.oupao.wiz.Tm.t0);
            var v2_2 = new StringBuilder();
            v2_2.append(p5);
            v2_2.append(" dt=");
            v2_2.append(v0_2);
            v2_2.append("ms appsN=");
            v2_2.append(com.oupao.wiz.Tm.aN);
            v2_2.append(" appsT=");
            v2_2.append(com.oupao.wiz.Tm.aT);
            com.oupao.wiz.MainActivity.dlog(v2_2.toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void mark()
    {
        com.oupao.wiz.Tm.t0 = android.os.SystemClock.uptimeMillis();
        return;
    }
    public static void reset()
    {
        com.oupao.wiz.Tm.aT = 0;
        com.oupao.wiz.Tm.aN = 0;
        return;
    }
}