package com.vanguard.vpn.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import com.vanguard.vpn.preference.LocaleHelper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import static android.content.pm.PackageManager.GET_META_DATA;
import com.vanguard.vpn.util.AppUpdater;
import org.json.JSONObject;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.NotificationCompat;
import com.vanguard.vpn.R;
import android.app.NotificationManager;
import android.net.Uri;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.content.pm.PackageInfo;
import com.vanguard.vpn.util.Utils;
import android.content.Intent;
import org.json.JSONException;
import android.content.DialogInterface;

public class BaseActivity extends AppCompatActivity
{
	public static int mTheme = 0;
    
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		resetTitles();
	}
	
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(LocaleHelper.setLocale(base));
	}
	protected void resetTitles() {
		try {
			ActivityInfo info = getPackageManager().getActivityInfo(getComponentName(), GET_META_DATA);
			if (info.labelRes != 0) {
				setTitle(info.labelRes);
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}
