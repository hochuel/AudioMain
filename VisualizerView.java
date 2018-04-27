package com.example.srv.audiomain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class VisualizerView extends View {

    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();
    private Paint mForePaint = new Paint();
    private Paint mFlashPaint = new Paint();

    protected float[] mFFTPoints;
    private float amplitude = 0;

    private final int temp = 1000;

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBytes = null;
        mForePaint.setStrokeWidth(1f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.rgb(200, 0, 0));
        mFlashPaint.setColor(Color.rgb(0, 0, 200));

    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }
        mRect.set(0, 0, getWidth(), getHeight());
        for (int i = 0; i < mBytes.length - 1; i++) {
            mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
            mPoints[i * 4 + 1] = mRect.height() / 2 + ((byte) (mBytes[i] + temp)) * (mRect.height() / 2) / temp;
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
            mPoints[i * 4 + 3] = mRect.height() / 2 + ((byte) (mBytes[i + 1] + temp)) * (mRect.height() / 2)  / temp;
        }


        // Calc amplitude for this waveform
        float accumulator = 0;
        for (int i = 0; i < mBytes.length - 1; i++) {
            accumulator += Math.abs(mBytes[i]);
        }

        float amp = accumulator/(temp * mBytes.length);
        if(amp > amplitude){
            // Amplitude is bigger than normal, make a prominent line
            amplitude = amp;
            canvas.drawLines(mPoints, mFlashPaint);
        }else{
            // Amplitude is nothing special, reduce the amplitude
            amplitude *= 0.99;
            canvas.drawLines(mPoints, mForePaint);
        }

       //canvas.drawLines(mPoints, mForePaint);


/*
        int mDivisions = 2;
        boolean mTop = true;

        Rect rect = new Rect();
        mFFTPoints = new float[mBytes.length * 4];
        for (int i = 0; i < mBytes.length / mDivisions; i++) {
            mFFTPoints[i * 4] = i * 4 * mDivisions;
            mFFTPoints[i * 4 + 2] = i * 4 * mDivisions;
            byte rfk = mBytes[mDivisions * i];
            byte ifk = mBytes[mDivisions * i + 1];
            float magnitude = (rfk * rfk + ifk * ifk);
            int dbValue = (int) (10 * Math.log10(magnitude));

            if(mTop)
            {
                mFFTPoints[i * 4 + 1] = 0;
                mFFTPoints[i * 4 + 3] = (dbValue * 2 - 10);
            }
            else
            {
                mFFTPoints[i * 4 + 1] = rect.height();
                mFFTPoints[i * 4 + 3] = rect.height() - (dbValue * 2 - 10);
            }
        }

        canvas.drawLines(mFFTPoints, mForePaint);
*/
    }

}
