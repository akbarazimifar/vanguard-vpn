package com.vanguard.vpn.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.vanguard.vpn.R;
import com.vanguard.vpn.preference.SettingsPreference;

import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.Preference;
import android.support.v4.app.Fragment;
import android.content.Intent;
import com.vanguard.vpn.preference.SettingsSSHPreference;
import com.vanguard.vpn.preference.SettingsDNSPreferences;

public class ConfigGeralActivity extends BaseActivity
	implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{
	public static String OPEN_SETTINGS_SSH = "openSSHScreen";
	public static String OPEN_SETTINGS_DNS = "openDNSScreen";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);

		PreferenceFragmentCompat preference = new SettingsPreference();
		Intent intent = getIntent();
		
		String action = intent.getAction();
		if (action != null && action.equals(OPEN_SETTINGS_SSH)) {
			setTitle(R.string.settings_ssh);
			preference = new SettingsSSHPreference();
		} else if (action != null && action.equals(OPEN_SETTINGS_DNS)) {
			setTitle(R.string.slowdns_configuration);
			preference = new SettingsDNSPreferences();
		}
		
		// add preference settings
		getSupportFragmentManager().beginTransaction()
			.replace(R.id.fragment_configLinearLayout, preference)
			.commit();

		// toolbar
		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		// Instantiate the new Fragment
		final Bundle bundle = pref.getExtras();
		final Fragment fragment = Fragment.instantiate(this, pref.getFragment(), bundle);
        
		fragment.setTargetFragment(caller, 0);
        
		// Replace the existing Fragment with the new Fragment
		getSupportFragmentManager().beginTransaction()
			.replace(R.id.fragment_configLinearLayout, fragment)
			.addToBackStack(null)
			.commit();
		
		return true;
	}

	@Override
	public boolean onSupportNavigateUp()
	{
		onBackPressed();
		return true;
	}
}

