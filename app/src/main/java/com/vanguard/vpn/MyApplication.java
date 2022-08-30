package com.vanguard.vpn;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;
import android.content.Context;

import com.vanguard.vpn.config.Settings;
import android.content.res.Configuration;
import com.vanguard.vpn.preference.LocaleHelper;
import com.vanguard.vpn.R;

public class MyApplication extends Application
{
	public static final String PREFS_GERAL = "PREFS_GERAL";
	public static final boolean DEBUG = true;
	private static Context app;

	private Settings mConfig;

	
	static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
		app = getApplicationContext();
		mConfig = new Settings(this);
		setModoNoturno(this);
	}
	
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(LocaleHelper.setLocale(base));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		LocaleHelper.setLocale(this);
	}
	
	public static Context getApp() {
		return app;
	}
	
	private void setModoNoturno(Context context) {
		String mode = mConfig.getModoNoturno();
		
		if (mode.equals("on")) {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setTheme(R.style.AppTheme);
		} else {
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
			setTheme(R.style.AppTheme);
		}

	}	
}
