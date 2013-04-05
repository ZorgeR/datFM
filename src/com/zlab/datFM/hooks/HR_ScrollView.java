package com.zlab.datFM.hooks;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class HR_ScrollView extends HorizontalScrollView {

    private HR_ScrollViewListener scrollViewListener = null;

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

}