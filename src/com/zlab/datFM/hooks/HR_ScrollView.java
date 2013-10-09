package com.zlab.datFM.hooks;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

public class HR_ScrollView extends HorizontalScrollView {

    private HR_ScrollViewListener scrollViewListener = null;
    private boolean scrolled = true;
    private GestureDetector mGestureDetector;
    View.OnTouchListener mGestureListener;


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
        mGestureDetector = new GestureDetector(context, new YScrollDetector());
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
        else */return super.onInterceptTouchEvent(ev) && mGestureDetector.onTouchEvent(ev);
    }

    class YScrollDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //Log.e("Distance X=",String.valueOf(distanceX));
            //Log.e("Distance Y=",String.valueOf(distanceY));
            if(Math.abs(distanceX) > Math.abs(distanceY)) {
                return true;
            }

            return false;
        }
    }


}