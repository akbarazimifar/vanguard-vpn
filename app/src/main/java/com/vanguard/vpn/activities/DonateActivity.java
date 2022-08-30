package com.vanguard.vpn.activities;

import android.net.Uri;
import android.support.v7.widget.Toolbar;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.view.View.*;
import android.widget.*;
import android.content.*;
import android.util.*;

import java.util.*;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import android.content.ClipData;
import android.content.ClipboardManager;

import com.vanguard.vpn.R;

public class DonateActivity extends BaseActivity implements OnClickListener {
	
	
	private Toolbar tb;
	
	private LinearLayout linear2;
	private TextView textview1;
	private LinearLayout linear3;
	private ImageView imageview1;
	private LinearLayout linear4;
	private TextView textview2;
	private ImageView imageview2;
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_donate);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {

		tb = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(tb);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		linear2 = (LinearLayout) findViewById(R.id.linear2);
		textview1 = (TextView) findViewById(R.id.textview1);
		linear3 = (LinearLayout) findViewById(R.id.linear3);
		imageview1 = (ImageView) findViewById(R.id.imageview1);
		linear4 = (LinearLayout) findViewById(R.id.linear4);
		textview2 = (TextView) findViewById(R.id.textview2);
		imageview2 = (ImageView) findViewById(R.id.imageview2);
		
		imageview2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View _view) {
				((ClipboardManager) getSystemService(getApplicationContext().CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", "1QKAxDaUfbWSQkqUExoMSEWPBiiN1Uvbkm"));
				Toast.makeText(DonateActivity.this, "BTC address copied!", Toast.LENGTH_SHORT).show();

			}
		});
	}
	private void initializeLogic() {
		setTitle("Donate");
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.donate) {
			((ClipboardManager) getSystemService(getApplicationContext().CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", "1QKAxDaUfbWSQkqUExoMSEWPBiiN1Uvbkm"));
			Toast.makeText(DonateActivity.this, "BTC address copied!", Toast.LENGTH_SHORT).show();
		} else {

		}
	}
}
