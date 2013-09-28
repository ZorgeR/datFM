package com.zlab.datFM.hooks;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class HR_ScrollView extends HorizontalScrollView {

    private HR_ScrollViewListener scrollViewListener = null;
    private boolean scrolled = true;

    public void setIsScrolled(boolean enabled) {
        scrolled = enabled;
    }
    public boolean isScrolled() {
        return scrolled;
    }

    public HR_ScrollView(Context context) {
        super(context);
    }

    public HR_ScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HR_ScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(HR_ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if(scrollViewListener != null) {
            scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) { /*
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (scrolled) return super.onTouchEvent(ev);
                return scrolled;
            default:               */
                return super.onTouchEvent(ev);
        //}
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {      /*
        if (!scrolled) return false;
        else */return super.onInterceptTouchEvent(ev);
    }


}