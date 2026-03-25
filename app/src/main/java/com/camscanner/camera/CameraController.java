package com.camscanner.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * 相机控制器，封装 Camera API。
 */
@SuppressWarnings("deprecation")
public class CameraController implements SurfaceHolder.Callback {

    private static final String TAG = "CameraController";

    private Activity mActivity;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsPreviewing = false;
    private boolean mIsCapturing = false;

    // 相机配置
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean mFlashEnabled = false;
    private int mDisplayRotation = 0;

    private OnPictureTakenListener mPictureListener;

    public interface OnPictureTakenListener {
        void onPictureTaken(byte[] data);
        void onPictureFailed(String error);
    }

    public CameraController(Activity activity) {
        mActivity = activity;
    }

    public void setOnPictureTakenListener(OnPictureTakenListener listener) {
        mPictureListener = listener;
    }

    public void openCamera() {
        try {
            mCamera = Camera.open(mCameraId);
            setCameraDisplayOrientation();
            Log.d(TAG, "Camera opened");
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to open camera", e);
        }
    }

    public void startPreview(SurfaceHolder holder) {
        if (mCamera == null) return;
        try {
            mSurfaceHolder = holder;
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mIsPreviewing = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start preview", e);
        }
    }

    public void stopPreview() {
        if (mCamera != null && mIsPreviewing) {
            mCamera.stopPreview();
            mIsPreviewing = false;
        }
    }

    public void toggleFlash() {
        if (mCamera == null) return;
        Camera.Parameters params = mCamera.getParameters();
        mFlashEnabled = !mFlashEnabled;
        params.setFlashMode(mFlashEnabled ?
                Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);
    }

    public void autoFocus() {
        if (mCamera == null || mIsCapturing) return;
        mCamera.autoFocus((success, camera) -> {
            Log.d(TAG, "Auto focus: " + (success ? "success" : "failed"));
        });
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        mDisplayRotation = result;
        mCamera.setDisplayOrientation(result);
    }

    private void configureOptimalSize() {
        if (mCamera == null) return;
        Camera.Parameters params = mCamera.getParameters();
        Camera.Size size = params.getPreferredPreviewSizeForVideo();
        if (size != null) {
            params.setPreviewSize(size.width, size.height);
        }
        mCamera.setParameters(params);
    }

    private void applyAutoExposure() {
        Log.d(TAG, "Applying auto exposure settings");
    }

    private void applyWhiteBalance() {
        Log.d(TAG, "Applying auto white balance");
    }

    private byte[] rotateJpeg(byte[] data, int degrees) {
        // Rotate JPEG data
        return data;
    }

    private void restartPreviewAfterCapture() {
        if (mCamera != null) {
            mCamera.startPreview();
            mIsPreviewing = true;
            mIsCapturing = false;
        }
    }

    private void logCaptureEvent(int dataSize) {
        Log.d(TAG, "Picture taken, size: " + dataSize + " bytes");
    }

    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved
    // reserved

    public void takePicture() {
        if (mCamera == null || mIsCapturing) return;
        mIsCapturing = true;
        try {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    // line 178 区域
                    logCaptureEvent(data != null ? data.length : 0);

                    if (data != null && data.length > 0) {
                        byte[] rotated = rotateJpeg(data, mDisplayRotation);
                        if (mPictureListener != null) {
                            mPictureListener.onPictureTaken(rotated);    // line ~178
                        }
                    } else {
                        if (mPictureListener != null) {
                            mPictureListener.onPictureFailed("Empty picture data");
                        }
                    }
                    restartPreviewAfterCapture();
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "takePicture failed", e);
            mIsCapturing = false;
            if (mPictureListener != null) {
                mPictureListener.onPictureFailed(e.getMessage());
            }
        }
    }

    public void release() {
        stopPreview();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mIsPreviewing) {
            stopPreview();
        }
        startPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }
}
