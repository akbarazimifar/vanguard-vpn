package com.vanguard.vpn.preference;

import android.support.v7.preference.PreferenceFragmentCompat;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.EditTextPreference;
import android.content.SharedPreferences;
import android.content.Intent;

import com.vanguard.vpn.R;
import com.vanguard.vpn.logger.SkStatus;
import com.vanguard.vpn.config.SettingsConstants;
import com.vanguard.vpn.config.Settings;
import com.vanguard.vpn.logger.ConnectionStatus;
import android.os.Handler;
import android.util.Log;

public class SettingsDNSPreferences extends PreferenceFragmentCompat
implements SettingsConstants, SkStatus.StateListener {
	private static final String TAG = SettingsSSHPreference.class.getSimpleName();

	private Handler mHandler;
	private Settings mConfig;
	private SharedPreferences mSecurePrefs;
	private SharedPreferences mInsecurePrefs;

	protected String[] listEdit_keysProteger = {
		CHAVE_KEY,
		NAMESERVER_KEY,
		DNS_KEY,
		USUARIO_KEY,
		SENHA_KEY
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHandler = new Handler();
		mConfig = new Settings(getContext());

		mInsecurePrefs = getPreferenceManager()
			.getDefaultSharedPreferences(getContext());
		mSecurePrefs = mConfig.getPrefsPrivate();
	}

	@Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the Preferences from the XML file
        setPreferencesFromResource(R.xml.slowdns_preference, s);

		// update views
		getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
	}

	@Override
	public void onResume() {
		super.onResume();

		SkStatus.addStateListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		SkStatus.removeStateListener(this);
	}

	@Override
	public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent) {
		mHandler.post(new Runnable() {
				@Override
				public void run() {
					getPreferenceScreen().setEnabled(!SkStatus.isTunnelActive());
				}
			});
	}

	@Override
    public void onStart() {
        super.onStart();

		for (String key : listEdit_keysProteger) {
			if (mSecurePrefs.contains(key)) {
				((EditTextPreference)findPreference(key))
					.setText(mSecurePrefs.getString(key, null));
			}

			if (mSecurePrefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				if ((key.equals(USUARIO_KEY) || key.equals(SENHA_KEY)) &&
					mSecurePrefs.getBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false)) {
					continue;
				}

				Preference pref = findPreference(key);

				pref.setEnabled(false);
				pref.setSummary(R.string.blocked);
			}
		}

		
    }

	@Override
    public void onStop() {
        super.onStop();

        //because the standard PreferenceActivity deals with unencrpyted prefs, we get them and replace with encrypted version when the activity is stopped
        final SharedPreferences.Editor insecureEditor = mInsecurePrefs.edit();
        final SharedPreferences.Editor secureEditor = mSecurePrefs.edit();

		for (String key : listEdit_keysProteger) {
			if (mInsecurePrefs.contains(key)) {
				Log.d(TAG, "match found for " + key + " adding encrypted copy to secure prefs");
				//add the enc versions to the secure prefs
				secureEditor.putString(key, mInsecurePrefs.getString(key, null));
				//remove entry from the default/insecure prefs
				insecureEditor.remove(key);
			}
		}     

		insecureEditor.commit();
        secureEditor.commit();
    }
}
