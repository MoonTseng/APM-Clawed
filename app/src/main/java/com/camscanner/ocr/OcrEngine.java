package com.camscanner.ocr;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * OCR 引擎封装，调用 Native 层实现文字识别。
 */
public class OcrEngine {

    private static final String TAG = "OcrEngine";

    static {
        System.loadLibrary("camscanner_ocr");
    }

    private long mNativeHandle = 0;
    private boolean mIsInitialized = false;
    private String mModelPath;
    private String mLanguage = "auto";

    // OCR 配置
    private boolean mDetectLayout = true;
    private boolean mRecognizeTables = false;
    private float mConfidenceThreshold = 0.7f;

    public OcrEngine(String modelPath) {
        mModelPath = modelPath;
    }

    public boolean initialize() {
        try {
            mNativeHandle = nativeInit(mModelPath);
            mIsInitialized = mNativeHandle != 0;
            if (mIsInitialized) {
                Log.d(TAG, "OCR engine initialized, handle=" + mNativeHandle);
            } else {
                Log.e(TAG, "OCR engine init failed");
            }
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Native library not found", e);
            mIsInitialized = false;
        }
        return mIsInitialized;
    }

    public void setLanguage(String lang) {
        mLanguage = lang;
    }

    public void setConfidenceThreshold(float threshold) {
        mConfidenceThreshold = threshold;
    }

    public void setDetectLayout(boolean detect) {
        mDetectLayout = detect;
    }

    private void validateInput(Bitmap image) {
        if (image == null) throw new IllegalArgumentException("Image cannot be null");
        if (image.isRecycled()) throw new IllegalArgumentException("Image is recycled");
    }

    // reserved
    // reserved

    public String process(Bitmap image) {               // line 67
        validateInput(image);
        if (!mIsInitialized) {
            throw new IllegalStateException("OCR engine not initialized");
        }

        Log.d(TAG, "Processing image: " + image.getWidth() + "x" + image.getHeight());

        // BUG: nativeProcess 可能因空指针导致 SIGSEGV
        // 当 mNativeHandle 已释放但未置零时触发
        return nativeProcess(mNativeHandle, image, mLanguage, mConfidenceThreshold);
    }

    public void release() {
        if (mNativeHandle != 0) {
            nativeRelease(mNativeHandle);
            mNativeHandle = 0;
            mIsInitialized = false;
            Log.d(TAG, "OCR engine released");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    // Native methods
    private native long nativeInit(String modelPath);
    private native String nativeProcess(long handle, Bitmap image, String language, float threshold);
    private native void nativeRelease(long handle);
}
