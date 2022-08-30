package com.vanguard.vpn.activities;

import android.widget.TextView;
import android.os.Bundle;
import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;


public class ExceptionActivity extends BaseActivity {
    TextView error;
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 17;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(8,8,8,8);
        linearLayout.setLayoutParams(layoutParams);
        setContentView(linearLayout);
        ScrollView sv = new ScrollView(this);
        TextView error = new TextView(this);
        sv.addView(error);
        linearLayout.addView(sv);
        error.setText(getIntent().getStringExtra("error"));         
    }   
}




