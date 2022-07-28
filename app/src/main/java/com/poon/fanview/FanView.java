package com.poon.fanview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;


public class FanView extends View {
    /**
     * 主区半径
     */
    protected float mRadius;
    /**
     * 外环区半径
     */
    protected float mRadiusRing;
    /**
     * 外环宽度
     */
    protected float mWidthRing = 10;
    /**
     * 外环与主区间距
     */
    protected float mSpaceRing = 30;
    /**
     * 区域缩进
     */
    protected float mPadding = 10;
    /**
     * 栅格总数
     */
    protected int mGridTotal;
    /**
     * 当前值占比
     */
    private float mProgress;
    /**
     * 栅格间距
     */
    private float mSpace;
    /**
     * 扇区起始角度
     */
    float angleStart = -225f, angleEnd = 45;

    private float perSweep;//每块扫过的角度
    protected float mCenterX;
    protected float mCenterY;

    private Paint mPaint, mPaintText, mPaintTra;
    private int mColorStart = Color.parseColor("#ffffd004");
    private int mColorEnd = Color.parseColor("#fffe2601");
    private int mColorDefault = Color.parseColor("#ffcccccc");
    private float mTextSizeCurrent, mTextSizeWord, mTextSize;
    private String mWord;


    private int current;

    private int max;

    public FanView(Context context) {
        this(context, null);
    }

    public FanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FanView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FanView, 0, 0);
        try {
            int grid = attributes.getInteger(R.styleable.FanView_grid, 12);
            mGridTotal = Math.max(grid, 1);
            max = attributes.getInteger(R.styleable.FanView_max, 100);
            current = attributes.getInteger(R.styleable.FanView_current, 50);
            mProgress = Math.min(current, max) * 1f / max * mGridTotal;
            mSpace = attributes.getFloat(R.styleable.FanView_space, 3f);

            mColorStart = attributes.getColor(R.styleable.FanView_colorStart, mColorStart);
            mColorEnd = attributes.getColor(R.styleable.FanView_colorStart, mColorEnd);
            mColorDefault = attributes.getColor(R.styleable.FanView_colorDefault, mColorDefault);

            mTextSize = attributes.getDimension(R.styleable.FanView_textSize, 50);
            mTextSizeWord = attributes.getDimension(R.styleable.FanView_textSizeWord, 80);
            mTextSizeCurrent = attributes.getDimension(R.styleable.FanView_textSizeCurrent, 200);

            mWord = attributes.getString(R.styleable.FanView_word);
            mWord = TextUtils.isEmpty(mWord) ? "总分" : mWord;
        } finally {
            attributes.recycle();
        }

        mPaintText = new Paint();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaintTra = new Paint();
        mPaintTra.setAntiAlias(true);
        mPaintTra.setStyle(Paint.Style.FILL);

        //每块扫过角度
        perSweep = (angleEnd - angleStart - (mGridTotal - 1) * mSpace) * 1f / mGridTotal;

    }

    RectF arcRF0, arcRF1, arcRF2;

    private void updateDimensions(int width, int height) {
        mCenterX = width / 2.0f;
        mCenterY = height / 2.0f;
        int diameter = Math.min(width, height);
        mRadiusRing = diameter / 2 - mPadding;
        mRadius = mRadiusRing - mWidthRing - mSpaceRing - mPadding;

        float arcLeft = mCenterX - mRadius;
        float arcTop = mCenterY - mRadius;
        float arcRight = mCenterX + mRadius;
        float arcBottom = mCenterY + mRadius;
        arcRF0 = new RectF(arcLeft, arcTop, arcRight, arcBottom);

        arcRF1 = new RectF(arcLeft - mSpaceRing, arcTop - mSpaceRing
                , arcRight + mSpaceRing, arcBottom + mSpaceRing);

        arcRF2 = new RectF(arcLeft - (mSpaceRing + mWidthRing), arcTop - (mSpaceRing + mWidthRing)
                , arcRight + (mSpaceRing + mWidthRing), arcBottom + (mSpaceRing + mWidthRing));

    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width > height)
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        else
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        updateDimensions(getWidth(), getHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDimensions(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float angleCurrent = angleStart;

        //外环
        mPaint.setColor(mColorDefault);
        canvas.drawArc(arcRF2, angleStart, angleEnd - angleStart, true, mPaint);
        mPaint.setColor(Color.WHITE);
        canvas.drawArc(arcRF1, angleStart, angleEnd - angleStart, true, mPaint);
        //主区
        for (int i = 0; i < mGridTotal; i++) {
            if (i < mProgress) {
                float bias = (float) i / (float) (mGridTotal);
                int color = interpolateColor(mColorStart, mColorEnd, bias);
                mPaint.setColor(color);
                if (i >= mProgress - 1) {
                    float first = perSweep * (mProgress - i);
                    canvas.drawArc(arcRF0, angleCurrent, first, true, mPaint);
                    mPaint.setColor(mColorDefault);
                    canvas.drawArc(arcRF0, angleCurrent + first, perSweep - first, true, mPaint);
                } else {
                    canvas.drawArc(arcRF0, angleCurrent, perSweep, true, mPaint);
                }
            } else {
                canvas.scale(1.0f, 1.0f);
                mPaint.setColor(mColorDefault);
                canvas.drawArc(arcRF0, angleCurrent, perSweep, true, mPaint);
            }
            angleCurrent = angleCurrent + perSweep + mSpace;
        }
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(mCenterX, mCenterY, (int) (mRadius / 1.2), mPaint);

        //梯形
        float offset = 10;
        float start = angleStart - offset;
        float end = angleEnd + offset;
        float ang = (float) ((360 - (end - start)) / 180 * Math.PI / 2);
        //梯形底边定点所在半径
        float r2 = mRadius + mSpaceRing + mWidthRing;
        //梯形高度
        float h = mRadius * 0.3f;
        float yd2 = (float) (r2 * Math.cos(ang));
        float xd2 = (float) (r2 * Math.sin(ang));
        float yd1 = yd2 - h;
        float xd1 = xd2 * yd1 / yd2;

        float px1 = mCenterX - xd1, py1 = mCenterY + yd1;
        float px2 = mCenterX + xd1, py2 = mCenterY + yd1;
        float px3 = mCenterX + xd2, py3 = mCenterY + yd2;
        float px4 = mCenterX - xd2, py4 = mCenterY + yd2;
        Path path = new Path();
        path.moveTo(px1, py1);
        path.lineTo(px2, py2);
        path.lineTo(px3, py3);
        path.lineTo(px4, py4);
        LinearGradient shader = new LinearGradient(mCenterX, py2, mCenterX, py3,
                new int[]{mColorStart, mColorEnd}, null,
                Shader.TileMode.CLAMP);
        mPaintTra.setShader(shader);
        canvas.drawPath(path, mPaintTra);
        //当前值
        String text = current + "";
        mPaintText.setColor(mColorEnd);
        mPaintText.setTextSize(mTextSizeCurrent);
        Rect rect = new Rect();
        mPaintText.getTextBounds(text, 0, text.length(), rect);
        canvas.drawText(text, mCenterX - rect.width() / 2, mCenterY, mPaintText);
        //文字
        text = mWord;
        mPaintText.setColor(mColorDefault);
        mPaintText.setTextSize(mTextSizeWord);
        Rect rect2 = new Rect();
        mPaintText.getTextBounds(text, 0, text.length(), rect2);
        canvas.drawText(text, mCenterX - rect2.width() / 2, mCenterY + rect.height() / 2 + rect2.height(), mPaintText);
        //起始值
        ang = (float) ((360 - (angleEnd - angleStart)) / 180 * Math.PI / 2);
        float r = mRadius + mSpaceRing + mWidthRing;
        float xd = (float) (r * Math.sin(ang));
        float yd = (float) (r * Math.cos(ang));
        mPaintText.setColor(mColorDefault);
        mPaintText.setTextSize(mTextSize);
        text = "0";
        mPaintText.getTextBounds(text, 0, text.length(), rect2);
        canvas.drawText(text, mCenterX - xd - rect2.width() / 2, mCenterY + yd + rect2.height(), mPaintText);
        text = "" + max;
        mPaintText.getTextBounds(text, 0, text.length(), rect2);
        canvas.drawText(text, mCenterX + xd - rect2.width() / 2, mCenterY + yd + rect2.height(), mPaintText);
        super.onDraw(canvas);
    }

    private float interpolate(float a, float b, float bias) {
        return (a + ((b - a) * bias));
    }

    private int interpolateColor(int colorA, int colorB, float bias) {
        float[] hsvColorA = new float[3];
        Color.colorToHSV(colorA, hsvColorA);

        float[] hsvColorB = new float[3];
        Color.colorToHSV(colorB, hsvColorB);

        hsvColorB[0] = interpolate(hsvColorA[0], hsvColorB[0], bias);
        hsvColorB[1] = interpolate(hsvColorA[1], hsvColorB[1], bias);
        hsvColorB[2] = interpolate(hsvColorA[2], hsvColorB[2], bias);

        if (isInEditMode())
            return colorA;

        return Color.HSVToColor(hsvColorB);
    }

    /**
     * 设置最大数字
     *
     * @param value 当前分数
     */
    public void setCurrent(int value) {
        //刻度采用动画
        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, Math.min(value, max));
        valueAnimator.setDuration(500);
        //线性变换插值器
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setRepeatCount(0);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                current = (int) animation.getAnimatedValue();
                mProgress = current * 1f / max * mGridTotal;
                postInvalidate();
            }
        });
        valueAnimator.start();
    }

    private void setCurrent(int value, boolean ani) {
        if (ani) {
            setCurrent(value);
        } else {
            this.current = current;
            mProgress = Math.min(current, max) * 1f / max * mGridTotal;
            invalidate();
        }
    }

    public int getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

}