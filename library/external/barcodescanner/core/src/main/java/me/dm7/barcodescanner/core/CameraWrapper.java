package me.dm7.barcodescanner.core;

import android.hardware.Camera;

import androidx.annotation.NonNull;

public class CameraWrapper {
    public final Camera mCamera;
    public final int mCameraId;

    private CameraWrapper(@NonNull Camera camera, int cameraId) {
        this.mCamera = camera;
        this.mCameraId = cameraId;
    }

    public static CameraWrapper getWrapper(Camera camera, int cameraId) {
        if (camera == null) {
            return null;
        } else {
            return new CameraWrapper(camera, cameraId);
        }
    }
}
