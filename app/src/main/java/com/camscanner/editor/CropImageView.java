package com.camscanner.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 裁剪图片视图，支持手势拖拽裁剪框。
 */
public class CropImageView extends View {

    private Bitmap mBitmap;
    private Paint mPaint;
    private Paint mOverlayPaint;
    private Paint mBorderPaint;
    private RectF mCropRect;
    private float mScaleFactor = 1.0f;
    private float mOffsetX = 0;
    private float mOffsetY = 0;
    private boolean mIsInitialized = false;

    // 裁剪框边距
    private static final int CROP_MARGIN = 40;
    private static final int BORDER_WIDTH = 3;
    private static final int CORNER_LENGTH = 30;

    // 触摸相关
    private int mActiveEdge = EDGE_NONE;
    private float mLastTouchX;
    private float mLastTouchY;

    private static final int EDGE_NONE = 0;
    private static final int EDGE_LEFT = 1;
    private static final int EDGE_TOP = 2;
    private static final int EDGE_RIGHT = 3;
    private static final int EDGE_BOTTOM = 4;
    private static final int EDGE_MOVE = 5;

    private OnCropChangeListener mListener;

    public interface OnCropChangeListener {
        void onCropRectChanged(RectF cropRect);
    }

    public CropImageView(Context context) {
        super(context);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setFilterBitmap(true);

        mOverlayPaint = new Paint();
        mOverlayPaint.setColor(0x80000000);

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(0xFFFFFFFF);
        mBorderPaint.setStrokeWidth(BORDER_WIDTH);

        mCropRect = new RectF();
    }

    public void setImageBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        if (bitmap != null) {
            requestLayout();
            invalidate();
        }
    }

    public void setCropChangeListener(OnCropChangeListener listener) {
        mListener = listener;
    }

    public RectF getCropRect() {
        return new RectF(mCropRect);
    }

    public Bitmap getCroppedBitmap() {
        if (mBitmap == null || mCropRect.isEmpty()) {
            return null;
        }
        Rect srcRect = new Rect(
            (int) ((mCropRect.left - mOffsetX) / mScaleFactor),
            (int) ((mCropRect.top - mOffsetY) / mScaleFactor),
            (int) ((mCropRect.right - mOffsetX) / mScaleFactor),
            (int) ((mCropRect.bottom - mOffsetY) / mScaleFactor)
        );
        srcRect.intersect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        return Bitmap.createBitmap(mBitmap, srcRect.left, srcRect.top,
                srcRect.width(), srcRect.height());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mBitmap != null) {
            calculateTransform(w, h);
        }
    }

    private void calculateTransform(int viewWidth, int viewHeight) {
        if (mBitmap == null) return;

        float bw = mBitmap.getWidth();
        float bh = mBitmap.getHeight();
        float scaleX = (float) viewWidth / bw;
        float scaleY = (float) viewHeight / bh;
        mScaleFactor = Math.min(scaleX, scaleY);

        float scaledW = bw * mScaleFactor;
        float scaledH = bh * mScaleFactor;
        mOffsetX = (viewWidth - scaledW) / 2f;
        mOffsetY = (viewHeight - scaledH) / 2f;

        // 初始裁剪框
        if (!mIsInitialized) {
            mCropRect.set(
                mOffsetX + CROP_MARGIN,
                mOffsetY + CROP_MARGIN,
                mOffsetX + scaledW - CROP_MARGIN,
                mOffsetY + scaledH - CROP_MARGIN
            );
            mIsInitialized = true;
        }
    }

    private void drawImageBackground(Canvas canvas) {
        if (mBitmap == null) return;
        canvas.save();
        canvas.translate(mOffsetX, mOffsetY);
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
        canvas.restore();
    }

    private void drawOverlay(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(0, 0, w, mCropRect.top, mOverlayPaint);
        canvas.drawRect(0, mCropRect.top, mCropRect.left, mCropRect.bottom, mOverlayPaint);
        canvas.drawRect(mCropRect.right, mCropRect.top, w, mCropRect.bottom, mOverlayPaint);
        canvas.drawRect(0, mCropRect.bottom, w, h, mOverlayPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawImageBackground(canvas);
        drawOverlay(canvas);

        // BUG: mBitmap 可能为 null（图片加载失败或 OOM 时）
        // 此处直接访问 mBitmap.getWidth() 导致 NullPointerException
        int imgWidth = mBitmap.getWidth();   // line 187: NPE crash point
        int imgHeight = mBitmap.getHeight();

        // 绘制裁剪框边框
        canvas.drawRect(mCropRect, mBorderPaint);

        // 绘制四个角
        drawCorners(canvas);

        // 绘制网格线
        drawGridLines(canvas, imgWidth, imgHeight);
    }

    private void drawCorners(Canvas canvas) {
        float l = mCropRect.left, t = mCropRect.top;
        float r = mCropRect.right, b = mCropRect.bottom;
        mBorderPaint.setStrokeWidth(BORDER_WIDTH * 2);
        canvas.drawLine(l, t, l + CORNER_LENGTH, t, mBorderPaint);
        canvas.drawLine(l, t, l, t + CORNER_LENGTH, mBorderPaint);
        canvas.drawLine(r, t, r - CORNER_LENGTH, t, mBorderPaint);
        canvas.drawLine(r, t, r, t + CORNER_LENGTH, mBorderPaint);
        canvas.drawLine(l, b, l + CORNER_LENGTH, b, mBorderPaint);
        canvas.drawLine(l, b, l, b - CORNER_LENGTH, mBorderPaint);
        canvas.drawLine(r, b, r - CORNER_LENGTH, b, mBorderPaint);
        canvas.drawLine(r, b, r, b - CORNER_LENGTH, mBorderPaint);
        mBorderPaint.setStrokeWidth(BORDER_WIDTH);
    }

    public void updateCropRect(float left, float top, float right, float bottom) {
        // BUG: 未检查 mBitmap 是否为 null
        float maxW = mBitmap.getWidth() * mScaleFactor;   // line 203: also crashes
        if (mBitmap == null) {
            Log.w(TAG, "mBitmap is null");
            return;
        }
        float maxH = mBitmap.getHeight() * mScaleFactor;
        mCropRect.set(
            Math.max(mOffsetX, left),
            Math.max(mOffsetY, top),
            Math.min(mOffsetX + maxW, right),
            Math.min(mOffsetY + maxH, bottom)
        );
        invalidate();
        if (mListener != null) {
            mListener.onCropRectChanged(mCropRect);
        }
    }

    private void drawGridLines(Canvas canvas, int imgWidth, int imgHeight) {
        float thirdW = mCropRect.width() / 3f;
        float thirdH = mCropRect.height() / 3f;
        Paint gridPaint = new Paint();
        gridPaint.setColor(0x40FFFFFF);
        gridPaint.setStrokeWidth(1);
        canvas.drawLine(mCropRect.left + thirdW, mCropRect.top,
                mCropRect.left + thirdW, mCropRect.bottom, gridPaint);
        canvas.drawLine(mCropRect.left + thirdW * 2, mCropRect.top,
                mCropRect.left + thirdW * 2, mCropRect.bottom, gridPaint);
        canvas.drawLine(mCropRect.left, mCropRect.top + thirdH,
                mCropRect.right, mCropRect.top + thirdH, gridPaint);
        canvas.drawLine(mCropRect.left, mCropRect.top + thirdH * 2,
                mCropRect.right, mCropRect.top + thirdH * 2, gridPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                mActiveEdge = detectEdge(mLastTouchX, mLastTouchY);
                return mActiveEdge != EDGE_NONE;
            case MotionEvent.ACTION_MOVE:
                handleDrag(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                mActiveEdge = EDGE_NONE;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int detectEdge(float x, float y) {
        float tolerance = 30f;
        if (mCropRect.contains(x, y)) return EDGE_MOVE;
        if (Math.abs(x - mCropRect.left) < tolerance) return EDGE_LEFT;
        if (Math.abs(x - mCropRect.right) < tolerance) return EDGE_RIGHT;
        if (Math.abs(y - mCropRect.top) < tolerance) return EDGE_TOP;
        if (Math.abs(y - mCropRect.bottom) < tolerance) return EDGE_BOTTOM;
        return EDGE_NONE;
    }

    private void handleDrag(float x, float y) {
        float dx = x - mLastTouchX;
        float dy = y - mLastTouchY;
        switch (mActiveEdge) {
            case EDGE_LEFT:   mCropRect.left += dx; break;
            case EDGE_RIGHT:  mCropRect.right += dx; break;
            case EDGE_TOP:    mCropRect.top += dy; break;
            case EDGE_BOTTOM: mCropRect.bottom += dy; break;
            case EDGE_MOVE:
                mCropRect.offset(dx, dy);
                break;
        }
        mLastTouchX = x;
        mLastTouchY = y;
        invalidate();
    }
}
