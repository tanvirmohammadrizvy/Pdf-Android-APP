package com.fulcrumy.pdfeditor.helper;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.Util;
import com.fulcrumy.pdfeditor.R;

public class ScrollHandle extends RelativeLayout implements com.github.barteksc.pdfviewer.scroll.ScrollHandle {
    private static final int DEFAULT_TEXT_SIZE = 16;
    private static final int HANDLE_LONG = 40;
    private static final int HANDLE_SHORT = 30;
    protected Context context;
    protected TextView textView;
    private float currentPos;
    private final Handler handler;
    private final Runnable hidePageScrollerRunnable;
    private final boolean inverted;
    private PDFView pdfView;
    private float relativeHandlerMiddle;

    public ScrollHandle(Context context2) {
        this(context2, false);
    }

    public ScrollHandle(Context context2, boolean z) {
        super(context2);
        this.relativeHandlerMiddle = 0.0f;
        this.handler = new Handler();
        this.hidePageScrollerRunnable = new Runnable() {
            public void run() {
                ScrollHandle.this.hide();
            }
        };
        this.context = context2;
        this.inverted = z;
        this.textView = new TextView(context2);
        setVisibility(4);
        setTextColor(-1);
        setTextSize(16);
    }

    public void setupLayout(PDFView pDFView) {
        Drawable drawable;
        int i;
        int i2 = 40;
        int i3 = 30;
        if (!pDFView.isSwipeVertical()) {
            if (this.inverted) {
                i = 10;
                drawable = ContextCompat.getDrawable(this.context, R.drawable.default_scroll_handle_top);
            } else {
                i = 12;
                drawable = ContextCompat.getDrawable(this.context, R.drawable.default_scroll_handle_bottom);
            }
            i2 = 30;
            i3 = 40;
        } else if (this.inverted) {
            i = 9;
            drawable = ContextCompat.getDrawable(this.context, R.drawable.default_scroll_handle_left);
        } else {
            i = 11;
            drawable = ContextCompat.getDrawable(this.context, R.drawable.ic_scroll_handle);
        }
        if (Build.VERSION.SDK_INT < 16) {
            setBackgroundDrawable(drawable);
        } else {
            setBackground(drawable);
        }
        LayoutParams layoutParams = new LayoutParams(Util.getDP(this.context, i2), Util.getDP(this.context, i3));
        layoutParams.setMargins(0, 0, 0, 0);
        LayoutParams layoutParams2 = new LayoutParams(-2, -2);
        layoutParams2.addRule(13, -1);
        addView(this.textView, layoutParams2);
        layoutParams.addRule(i);
        pDFView.addView(this, layoutParams);
        this.pdfView = pDFView;
    }

    public void destroyLayout() {
        this.pdfView.removeView(this);
    }

    public void setScroll(float f) {
        if (!shown()) {
            show();
        } else {
            this.handler.removeCallbacks(this.hidePageScrollerRunnable);
        }
        setPosition(((float) (this.pdfView.isSwipeVertical() ? this.pdfView.getHeight() : this.pdfView.getWidth())) * f);
    }

    private void setPosition(float f) {
        int i;
        if (!Float.isInfinite(f) && !Float.isNaN(f)) {
            if (this.pdfView.isSwipeVertical()) {
                i = this.pdfView.getHeight();
            } else {
                i = this.pdfView.getWidth();
            }
            float f2 = (float) i;
            float f3 = f - this.relativeHandlerMiddle;
            if (f3 < 0.0f) {
                f3 = 0.0f;
            } else if (f3 > f2 - ((float) Util.getDP(this.context, 30))) {
                f3 = f2 - ((float) Util.getDP(this.context, 30));
            }
            if (this.pdfView.isSwipeVertical()) {
                setY(f3);
            } else {
                setX(f3);
            }
            calculateMiddle();
            invalidate();
        }
    }

    private void calculateMiddle() {
        int i;
        float f;
        float f2;
        if (this.pdfView.isSwipeVertical()) {
            f2 = getY();
            f = (float) getHeight();
            i = this.pdfView.getHeight();
        } else {
            f2 = getX();
            f = (float) getWidth();
            i = this.pdfView.getWidth();
        }
        this.relativeHandlerMiddle = ((f2 + this.relativeHandlerMiddle) / ((float) i)) * f;
    }

    public void hideDelayed() {
        this.handler.postDelayed(this.hidePageScrollerRunnable, 1000);
    }

    public void setPageNum(int i) {
        String valueOf = String.valueOf(i);
        if (!this.textView.getText().equals(valueOf)) {
            this.textView.setText(valueOf);
        }
    }

    public boolean shown() {
        return getVisibility() == 0;
    }

    public void show() {
        setVisibility(0);
    }

    public void hide() {
        setVisibility(4);
    }

    public void setTextColor(int i) {
        this.textView.setTextColor(i);
    }

    public void setTextSize(int i) {
        this.textView.setTextSize(1, (float) i);
    }

    private boolean isPDFViewReady() {
        PDFView pDFView = this.pdfView;
        return pDFView != null && pDFView.getPageCount() > 0 && !this.pdfView.documentFitsView();
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0078  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(android.view.MotionEvent r5) {

        throw new UnsupportedOperationException("Method not decompiled: ScrollHandle.onTouchEvent(android.view.MotionEvent):boolean");
    }
}
