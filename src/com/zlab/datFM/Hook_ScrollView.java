package com.zlab.datFM;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class Hook_ScrollView extends HorizontalScrollView {

    private Hook_ScrollViewListener scrollViewListener = null;

    public Hook_ScrollView(Context context) {
        super(context);
    }

    public Hook_ScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Hook_ScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(Hook_ScrollViewListener scrollViewListener) {
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