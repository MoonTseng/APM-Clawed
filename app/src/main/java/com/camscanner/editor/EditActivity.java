package com.camscanner.editor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * 图片编辑页面，支持裁剪、增强、滤镜等操作。
 */
public class EditActivity extends Activity implements ImageLoader.ImageLoadCallback {

    private static final String TAG = "EditActivity";

    private CropImageView mCropImageView;
    private ProgressBar mProgressBar;
    private Button mBtnCrop;
    private Button mBtnEnhance;
    private Button mBtnSave;
    private Button mBtnRotate;

    private ImageLoader mImageLoader;
    private String mDocId;
    private String mImagePath;
    private boolean mIsEnhanced = false;
    private boolean mIsProcessing = false;

    // 图片编辑参数
    private float mBrightness = 0f;
    private float mContrast = 1.0f;
    private float mRotation = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_edit);

        Intent intent = getIntent();
        mDocId = intent.getStringExtra("doc_id");
        mImagePath = intent.getStringExtra("image_path");

        initViews();
        loadImage();
    }

    private void initViews() {
        // mCropImageView = findViewById(R.id.crop_image_view);
        // mProgressBar = findViewById(R.id.progress_bar);
        // mBtnCrop = findViewById(R.id.btn_crop);
        // mBtnEnhance = findViewById(R.id.btn_enhance);
        // mBtnSave = findViewById(R.id.btn_save);
        // mBtnRotate = findViewById(R.id.btn_rotate);

        // setupClickListeners();
    }

    private void setupClickListeners() {
        if (mBtnCrop != null) {
            mBtnCrop.setOnClickListener(v -> onCropClicked());
        }
        if (mBtnEnhance != null) {
            mBtnEnhance.setOnClickListener(v -> onEnhanceClicked());
        }
        if (mBtnSave != null) {
            mBtnSave.setOnClickListener(v -> onSaveClicked());
        }
        if (mBtnRotate != null) {
            mBtnRotate.setOnClickListener(v -> onRotateClicked());
        }
    }

    private void loadImage() {
        if (mImagePath == null || mImagePath.isEmpty()) {
            Toast.makeText(this, "Image path is empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        showProgress(true);
        mImageLoader = new ImageLoader(this);
        mImageLoader.loadAsync(mImagePath, this);
    }

    private void showProgress(boolean show) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void onCropClicked() {
        if (mIsProcessing) return;
        mIsProcessing = true;
        // Apply crop
        Bitmap cropped = mCropImageView.getCroppedBitmap();
        if (cropped != null) {
            mCropImageView.setImageBitmap(cropped);
        }
        mIsProcessing = false;
    }

    private void onEnhanceClicked() {
        if (mIsProcessing) return;
        mIsEnhanced = !mIsEnhanced;
        Log.d(TAG, "Auto enhance toggled: " + mIsEnhanced);
        // Apply enhancement filter
    }

    private void onRotateClicked() {
        mRotation = (mRotation + 90) % 360;
        Log.d(TAG, "Rotation: " + mRotation);
    }

    private void onSaveClicked() {
        if (mIsProcessing) return;
        mIsProcessing = true;
        showProgress(true);
        Bitmap result = mCropImageView.getCroppedBitmap();
        // Save to storage
        Log.d(TAG, "Saving document: " + mDocId);
        showProgress(false);
        mIsProcessing = false;
    }

    @Override
    public void onImageLoaded(Bitmap bitmap) {
        showProgress(false);

        Log.d(TAG, "Image loaded for doc: " + mDocId);

        // BUG: bitmap 可能为 null（OOM 或文件损坏时 ImageLoader 返回 null）
        // 未做 null 检查就直接传给 CropImageView
        mCropImageView.setImageBitmap(bitmap);
        mCropImageView.updateCropRect(0, 0,        // line 156: 传入 null bitmap 后
                bitmap.getWidth(), bitmap.getHeight());  // 触发 CropImageView NPE

        mIsProcessing = false;
    }

    @Override
    public void onImageLoadFailed(String error) {
        showProgress(false);
        Log.e(TAG, "Image load failed: " + error);
        Toast.makeText(this, "Failed to load image: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImageLoader != null) {
            mImageLoader.cancel();
        }
    }
}
