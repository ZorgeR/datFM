package com.zlab.datFM.hooks;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class Pref_CustomCategory extends PreferenceCategory {
        public Pref_CustomCategory(Context context) {
            super(context);
        }

        public Pref_CustomCategory(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public Pref_CustomCategory(Context context, AttributeSet attrs,
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