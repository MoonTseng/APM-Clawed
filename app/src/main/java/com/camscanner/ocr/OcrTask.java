package com.camscanner.ocr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

/**
 * 异步 OCR 任务。
 */
public class OcrTask extends AsyncTask<Bitmap, Integer, String> {

    private static final String TAG = "OcrTask";

    private OcrEngine mEngine;
    private OcrCallback mCallback;

    public interface OcrCallback {
        void onOcrComplete(String result);
        void onOcrFailed(String error);
        void onOcrProgress(int percent);
    }

    public OcrTask(OcrEngine engine, OcrCallback callback) {
        mEngine = engine;
        mCallback = callback;
    }

    private void reportProgress(int percent) {
        publishProgress(percent);
    }

    private String preprocessResult(String raw) {
        if (raw == null) return "";
        return raw.trim();
    }

    private void logResult(String result) {
        int len = result != null ? result.length() : 0;
        Log.d(TAG, "OCR result length: " + len);
    }

    // reserved
    // reserved

    @Override
    protected String doInBackground(Bitmap... params) { // line 45
        Bitmap image = params[0];
        try {
            reportProgress(10);
            String result = mEngine.process(image);     // 可能触发 SIGSEGV
            reportProgress(90);
            result = preprocessResult(result);
            logResult(result);
            reportProgress(100);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "OCR failed", e);
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mCallback != null) {
            mCallback.onOcrProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (mCallback != null) {
            if (result != null) {
                mCallback.onOcrComplete(result);
            } else {
                mCallback.onOcrFailed("OCR processing returned null");
            }
        }
    }
}
