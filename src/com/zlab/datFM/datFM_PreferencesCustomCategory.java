package com.zlab.datFM;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class datFM_PreferencesCustomCategory extends PreferenceCategory {
        public datFM_PreferencesCustomCategory(Context context) {
            super(context);
        }

        public datFM_PreferencesCustomCategory(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public datFM_PreferencesCustomCategory(Context context, AttributeSet attrs,
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