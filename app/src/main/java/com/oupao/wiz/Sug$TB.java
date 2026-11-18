package com.oupao.wiz;
public class Sug$TB extends android.hardware.camera2.CameraManager.TorchCallback {
    public Sug$TB()
    {
        return;
    }
    public void onTorchModeChanged(String p2, boolean p3)
    {
        try {
            com.oupao.wiz.Sug.onTorch(p2, p3);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}