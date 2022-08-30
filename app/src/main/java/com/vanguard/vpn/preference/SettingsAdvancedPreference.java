package com.vanguard.vpn.preference;

import android.support.v7.preference.PreferenceFragmentCompat;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.support.v7.preference.CheckBoxPreference;
import android.content.Intent;

import com.vanguard.vpn.R;
import com.vanguard.vpn.logger.SkStatus;
import com.vanguard.vpn.config.SettingsConstants;
import com.vanguard.vpn.config.Settings;
import com.vanguard.vpn.logger.ConnectionStatus;
import android.os.Handler;
import android.support.v7.preference.ListPreference;
import android.os.Build;

public class SettingsAdvancedPreference extends PreferenceFragmentCompat
	implements SettingsConstants, SkStatus.StateListener,
		Preference.OnPreferenceChangeListener
{
	private Handler mHandler;
	private SharedPreferences mPref;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mHandler = new Handler();
	}

	@Override
    public void onCreatePreferences(Bundle bundle, String s)
	{
        // Load the Preferences from the XML file
        setPreferencesFromResource(R.xml.advanced_settings_preference, s);

		mPref = getPreferenceManager()
			.getDefaultSharedPreferences(getContext());
			
		Settings config = new Settings(getContext());

		/*ListPreference numberMaxThreads = (ListPreference)
			findPreference(MAXIMO_THREADS_KEY);
		numberMaxThreads.setOnPreferenceChangeListener(this);*/
		
		CheckBoxPreference checkDebug = (CheckBoxPreference) findPreference(MODO_DEBUG_KEY);
		checkDebug.setOnPreferenceChangeListener(this);
		
		// update views
		getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
		if (!SkStatus.isTunnelActive()) {
			if (new Settings(getContext()).getPrefsPrivate()
					.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				findPreference(MODO_DEBUG_KEY).setEnabled(false);
			}
		}
		
		// desativa se não suportar
		if (Build.VERSION.SDK_INT < 21) {
			String[] list = {
				FILTER_APPS,
				FILTER_BYPASS_MODE,
				FILTER_APPS_LIST
			};
			for (String key : list) {
				findPreference(key).setEnabled(false);
			}
		}
		else {
			CheckBoxPreference pref = (CheckBoxPreference) findPreference(FILTER_APPS);
			pref.setOnPreferenceChangeListener(this);
			
			enableFilterLayout(config.getIsFilterApps());
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		SkStatus.addStateListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		SkStatus.removeStateListener(this);
	}

	@Override
	public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
			}
		});
	}
	

	/**
	 * Preference.OnPreferenceChangeListener
	 * Implementação
	 */

	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue)
	{
		switch(pref.getKey()) {
			case MODO_DEBUG_KEY:
				boolean isDebug = (boolean) newValue;

				if (isDebug) {
					Toast.makeText(getContext(), "Disable after finishing the tests",
						Toast.LENGTH_SHORT).show();
				}
			break;

			case FILTER_APPS:
				boolean isEnabled = (boolean) newValue;
				
				enableFilterLayout(isEnabled);
			break;
		}

		return true;
	}
	
	private void enableFilterLayout(boolean is) {
		String[] list = {
			FILTER_BYPASS_MODE,
			FILTER_APPS_LIST
		};

		for (String key : list) {
			findPreference(key).setEnabled(is);
		}
	}
	

	/**
	 * Utils
	 */

	public static void setListPreferenceSummary(ListPreference pref, String value) {
		int index = pref.findIndexOfValue(value);
		if (index >= 0) {
			pref.setSummary(pref.getEntries()[index]);
		}
	}
}
