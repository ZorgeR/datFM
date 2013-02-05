package com.zlab.datFM;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class datFM_Preferences_Category extends PreferenceCategory {
        public datFM_Preferences_Category(Context context) {
            super(context);
        }

        public datFM_Preferences_Category(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public datFM_Preferences_Category(Context context, AttributeSet attrs,
                                    int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            TextView titleView = (TextView) view.findViewById(android.R.id.title);
            titleView.setTextColor(Color.RED);
        }
    }