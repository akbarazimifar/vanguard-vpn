package com.vanguard.vpn.activities;


import android.support.v7.widget.Toolbar;
import android.view.View.OnClickListener;
import android.os.Bundle;
import com.vanguard.vpn.R;
import android.view.View;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.text.Html;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageInfo;
import com.vanguard.vpn.util.Utils;
import com.vanguard.vpn.MyApplication;
import com.vanguard.vpn.tunnel.TunnelUtils;

public class AboutActivity extends BaseActivity implements OnClickListener {

	private Toolbar tb;
	private View changelog, license, dev;
	private AlertDialog.Builder ab;
    private TextView app_info_text;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO: Implement this method
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		tb = (Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(tb);
		changelog = findViewById(R.id.changelog);
		license = findViewById(R.id.license);
		dev = findViewById(R.id.developer);
		changelog.setOnClickListener(this);
		license.setOnClickListener(this);
		dev.setOnClickListener(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        PackageInfo pinfo = Utils.getAppInfo(this);
        if (pinfo != null) {
            String version_nome = pinfo.versionName;
            int version_code = pinfo.versionCode;
            String header_text = String.format("%s (%d)", version_nome, version_code);
            app_info_text = (TextView) findViewById(R.id.appVersion);
			app_info_text.setText(header_text);
		}
	}
	@Override
	public void onClick(View view) {
		// TODO: Implement this method
		int id = view.getId();
		if (id == R.id.changelog) {
			changelog();
		} else if (id == R.id.developer) {
			startActivity(new Intent("android.intent.action.VIEW", 
									 Uri.parse("mailto: thevanguardapps@protonmail.com")));
		}
	}

	private void changelog() {
		// TODO: Implement this method
		ab = new AlertDialog.Builder(this);
		ab.setTitle("About");
		ab.setMessage(Html.fromHtml("Developed by: Vanguard Apps"));
		ab.setPositiveButton(android.R.string.ok, null);
		ab.create().show();
	}

}
