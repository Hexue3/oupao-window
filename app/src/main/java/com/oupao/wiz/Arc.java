package com.oupao.wiz;
public class Arc {
    public Arc()
    {
        return;
    }
    public static float cosDeg(float p5)
    {
        var v1_0 = ((p5 * 0.01745329f) * (p5 * 0.01745329f));
        var v2_0 = (v1_0 * v1_0);
        return (((1.0f - (v1_0 * 0.5f)) + (v2_0 * 0.04166667f)) - ((v2_0 * v1_0) * 0.001388889f));
    }
    public static float sinDeg(float p5)
    {
        var v5_1 = (p5 * 0.01745329f);
        var v1_0 = (v5_1 * v5_1);
        var v2_0 = (v1_0 * v1_0);
        return (v5_1 * (((1.0f - (v1_0 * 0.1666667f)) + (v2_0 * 0.008333334f)) - ((v2_0 * v1_0) * 0.0001984127f)));
    }
    public static float t0(int p4)
    {
        return ((1113587712 / ((float) p4)) - (1094084198 / (((float) p4) * ((float) p4))));
    }
}