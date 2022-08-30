package com.vanguard.vpn.activities;

import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;

import com.vanguard.vpn.R;
import com.vanguard.vpn.MainActivity;

public class SplashActivity extends BaseActivity implements Runnable {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(this, 2000);
    }

    @Override
    public void run() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
