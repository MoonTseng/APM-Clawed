package com.camscanner.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;

/**
 * 异步图片加载器，支持大图降采样和内存管理。
 */
public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final long MAX_MEMORY_FOR_IMAGE = 50 * 1024 * 1024; // 50MB

    private ImageLoadCallback mCallback;
    private LoadTask mCurrentTask;

    public interface ImageLoadCallback {
        void onImageLoaded(Bitmap bitmap);
        void onImageLoadFailed(String error);
    }

    public ImageLoader(ImageLoadCallback callback) {
        mCallback = callback;
    }

    public void loadAsync(String path, ImageLoadCallback callback) {
        mCallback = callback;
        cancel();
        mCurrentTask = new LoadTask();
        mCurrentTask.execute(path);
    }

    public void cancel() {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
            mCurrentTask = null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfH = height / 2;
            int halfW = width / 2;
            while ((halfH / inSampleSize) >= reqHeight && (halfW / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private class LoadTask extends AsyncTask<String, Void, Bitmap> {

        private String errorMessage;

        @Override
        protected Bitmap doInBackground(String... params) {
            String path = params[0];
            try {
                File file = new File(path);
                if (!file.exists()) {
                    errorMessage = "File not found: " + path;
                    return null;
                }

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);

                opts.inSampleSize = calculateInSampleSize(opts,
                        MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION);
                opts.inJustDecodeBounds = false;

                // 可能因 OOM 或文件损坏返回 null
                Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
                if (bitmap == null) {
                    Log.w(TAG, "decode returned null (OOM fallback triggered)");
                    errorMessage = "Failed to decode image";
                }
                return bitmap;
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "OOM while loading image: " + path, e);
                errorMessage = "Out of memory";
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // BUG: result 可能为 null，但直接传给 callback 未做处理
            // callback (EditActivity) 也未检查 null，导致后续 NPE
            if (mCallback != null) {
                mCallback.onImageLoaded(result);    // line 89: 传递 null bitmap
            }
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "Image loading cancelled");
        }
    }
}
