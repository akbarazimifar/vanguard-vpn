package com.vanguard.vpn;

	import android.os.Bundle;
	import android.support.v7.app.AlertDialog;
	import android.support.v7.widget.Toolbar;
	import android.support.v4.app.DialogFragment;

	import com.bugsnag.android.Bugsnag;
	import com.mixpanel.android.mpmetrics.MixpanelAPI;
	import com.vanguard.vpn.activities.DonateActivity;
	import com.vanguard.vpn.fragments.ExitDialogFragment;
	import android.view.Menu;
	import android.view.MenuItem;
	import android.content.Intent;
    import android.widget.Toast;
	import android.view.View;
	import android.content.Context;

    import com.vanguard.vpn.util.Utils;

	import android.widget.TextView;
	import android.support.v4.view.GravityCompat;
    import android.support.design.widget.TextInputEditText;
	import android.support.v4.widget.DrawerLayout;
	import android.net.Uri;
	import android.widget.Button;
    import android.support.v4.content.LocalBroadcastManager;
	import android.content.BroadcastReceiver;
	import android.content.IntentFilter;
	import android.content.SharedPreferences;
	import com.vanguard.vpn.activities.ConfigGeralActivity;

    import android.text.Html;
    import android.content.pm.PackageInfo;
	import com.vanguard.vpn.logger.SkStatus;

    import android.widget.LinearLayout;
	import com.vanguard.vpn.fragments.ProxyRemoteDialogFragment;

    import android.os.Build;
    import android.app.Activity;
	import com.vanguard.vpn.logger.ConnectionStatus;
	import android.os.Handler;

    import com.vanguard.vpn.fragments.ClearConfigDialogFragment;
	import com.vanguard.vpn.config.Settings;
	import android.support.v7.app.ActionBarDrawerToggle;
	import android.os.PersistableBundle;
	import android.content.res.Configuration;
	import android.support.annotation.NonNull;

    import com.vanguard.vpn.config.ConfigParser;

    import android.content.DialogInterface;
	import com.vanguard.vpn.tunnel.TunnelManagerHelper;
    import com.vanguard.vpn.activities.AboutActivity;

    import android.support.v4.view.ViewPager;
    import android.support.design.widget.NavigationView;
	import com.vanguard.vpn.activities.BaseActivity;

	import android.support.annotation.Nullable;
    import android.support.v7.widget.CardView;
	import com.vanguard.vpn.view.PayloadGenerator;
    import com.vanguard.vpn.model.ExceptionHandler;
	import android.support.v7.widget.LinearLayoutManager;
	import com.vanguard.vpn.adapter.LogsAdapter;
	import android.support.v7.widget.RecyclerView;
	import android.support.design.widget.TabLayout;
	import android.support.v4.view.PagerAdapter;
	import android.view.ViewGroup;
	import java.util.List;
	import java.util.Arrays;
	import com.vanguard.vpn.fragments.CustomSNIDialogFragment;
	import com.vanguard.vpn.fragments.AuthenticationFragment;
	import android.support.v7.app.AppCompatDelegate;

	public class MainActivity extends BaseActivity implements DrawerLayout.DrawerListener,
	NavigationView.OnNavigationItemSelectedListener,
	View.OnClickListener, SkStatus.StateListener,
	DialogInterface.OnClickListener {
		private static final String TAG = MainActivity.class.getSimpleName();
		private static final String UPDATE_VIEWS = "MainUpdate";
		public static final String OPEN_LOGS = "com.speedfusion.ssh:openLogs";
		private Settings mConfig;
		private Toolbar toolbar_main;
		private Handler mHandler;
		private LinearLayout mainLayout;
		private Button starterButton;
		private CardView configMsgLayout;
		private TextView configMsgText;
		private PayloadGenerator paygen;
		private static final String[] tabTitle = {"HOME","LOGS"};
		private LogsAdapter mLogAdapter;
		private RecyclerView logList;
		private ViewPager vp;
		private TabLayout tabs;
		private MenuItem settings;
		public static boolean isHomeTab = true;

		private CardView tunnelLayout;
		private CardView connectionCardview;
		private TextView tunnelInfo;
		private View payloadLayout;
		private View proxyLayout;
		private View sslLayout;
		private String proxyStr;
		private TextView proxyText;
		private TextInputEditText payloadEdit;
		private TextView sniText;
		private NavigationView drawerNavigationView;

		private MenuItem auth;

		private MenuItem settingsSSH;

		@Override
		protected void onCreate(@Nullable Bundle savedInstanceState)  {
			super.onCreate(savedInstanceState);
			MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, "4aca9fb68e1cbba1a042c04c9040e5b4");
			Bugsnag.start(this);
			mHandler = new Handler();
			mConfig = new Settings(this);
			paygen = new PayloadGenerator(this);
			Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
			doLayout();
			IntentFilter filter = new IntentFilter();
			filter.addAction(UPDATE_VIEWS);
			filter.addAction(OPEN_LOGS);
			LocalBroadcastManager.getInstance(this).registerReceiver(mActivityReceiver, filter);
			doUpdateLayout();
		}

		private void doLayout() {
			setContentView(R.layout.activity_main_drawer);
			String mode = mConfig.getModoNoturno();

			if (mode.equals("on")) {
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				setTheme(R.style.AppTheme);
			} else {
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				setTheme(R.style.AppTheme);
			}

			toolbar_main = (Toolbar) findViewById(R.id.toolbar_main);
			doDrawerMain(toolbar_main);
			setSupportActionBar(toolbar_main);
			doTabs();
			mainLayout = (LinearLayout) findViewById(R.id.activity_mainLinearLayout);
			starterButton = (Button) findViewById(R.id.activity_starterButtonMain);
			starterButton.setOnClickListener(this);
			configMsgLayout = (CardView) findViewById(R.id.activitymainCardView1);
			configMsgText = (TextView) findViewById(R.id.activity_mainMensagemConfigTextView);
			tunnelLayout = (CardView) findViewById(R.id.tunnelCardView);
			tunnelLayout.setOnClickListener(this);
			tunnelInfo = (TextView) findViewById(R.id.activitymainTextView1);
			connectionCardview = (CardView) findViewById(R.id.connection_cardView);
			proxyText = (TextView) findViewById(R.id.proxyText);
			payloadEdit = (TextInputEditText) findViewById(R.id.payloadEdit);

			payloadLayout = (View) findViewById(R.id.payloadLayout);
			proxyLayout = (View) findViewById(R.id.proxyLayout);
			proxyLayout.setOnClickListener(this);
			sslLayout = (View) findViewById(R.id.sslLayout);
			sslLayout.setOnClickListener(this);
			sniText = (TextView) findViewById(R.id.sslText);

		}

		private synchronized void doSaveData() {
			SharedPreferences prefs = mConfig.getPrefsPrivate();
			SharedPreferences.Editor edit = prefs.edit();
			if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				if (payloadEdit != null && !prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true)) {
					if (mainLayout != null)
						mainLayout.requestFocus();
					edit.putString(Settings.CUSTOM_PAYLOAD_KEY, payloadEdit.getText().toString());
				}
			}
			int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
			if (tunnelType == Settings.bTUNNEL_TYPE_SLOWDNS) {
				edit.putString(Settings.SERVIDOR_KEY, "127.0.0.1");
				edit.putString(Settings.SERVIDOR_PORTA_KEY, "8989");

			}
			edit.apply();
		}

		private void doUpdateLayout() {
			SharedPreferences prefs = mConfig.getPrefsPrivate();
			boolean isRunning = SkStatus.isTunnelActive();
			setStarterButton(starterButton, this);
			boolean protect = prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false);
			String proxy = mConfig.getPrivString(Settings.PROXY_IP_KEY);

			int msgVisibility = View.GONE;
			String msgText = "";

			if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {	
				String msg = mConfig.getPrivString(Settings.CONFIG_MENSAGEM_KEY);
				if (!msg.isEmpty()) {
					msgText = msg.replace("\n", "<br/>");
					msgVisibility = View.VISIBLE;
				}

				if (mConfig.getPrivString(Settings.PROXY_IP_KEY).isEmpty() ||
					mConfig.getPrivString(Settings.PROXY_PORTA_KEY).isEmpty()) {
				}
			}
			configMsgText.setText(msgText.isEmpty() ? "" : Html.fromHtml(msgText));
			configMsgLayout.setVisibility(msgVisibility);

			if (mConfig.getPrivString("enable_auth").equals("_true")) {
				Menu menuNav = drawerNavigationView.getMenu();
				auth = menuNav.findItem(R.id.authentication);
				auth.setVisible(true);
			} else {
				Menu menuNav = drawerNavigationView.getMenu();
				auth = menuNav.findItem(R.id.authentication);
				auth.setVisible(false);
			}
			if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				proxyText.setText("***************");
				proxyLayout.setEnabled(false);
			} else {
				proxyLayout.setEnabled(!isRunning);
				if (proxy.equals("")) {	
					proxyText.setText(R.string.squid);
				}  else {
					proxyText.setText(String.format("%s:%s", proxy, mConfig.getPrivString(Settings.PROXY_PORTA_KEY)));
				}
			}
			int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
			if (tunnelType == Settings.bTUNNEL_TYPE_SLOWDNS) {
				Menu menuNav = drawerNavigationView.getMenu();
				settingsSSH = menuNav.findItem(R.id.miSettingsSSH);
				settingsSSH.setTitle(R.string.slowdns_configuration);
			} else {
				Menu menuNav = drawerNavigationView.getMenu();
				settingsSSH = menuNav.findItem(R.id.miSettingsSSH);
				settingsSSH.setTitle(R.string.settings_ssh);
			}

			switch (tunnelType) {
				case Settings.bTUNNEL_TYPE_SSH_DIRECT:
					if (!prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true)) {
						connectionCardview.setVisibility(View.VISIBLE);
						tunnelInfo.setText(getString(R.string.direct) + getString(R.string.custom_payload1));
						if (protect) {
							payloadEdit.setEnabled(false);
							payloadEdit.setText("******");
						} else {
							payloadEdit.setEnabled(!isRunning);
							payloadEdit.setText(mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY));
						}

					} else {
						connectionCardview.setVisibility(View.GONE);
						tunnelInfo.setText(getString(R.string.direct));
					} 	
					break;

				case Settings.bTUNNEL_TYPE_SSH_PROXY:
					if (!prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true)) {
						connectionCardview.setVisibility(View.VISIBLE);
						tunnelInfo.setText(getString(R.string.http) + getString(R.string.custom_payload1));
						proxyLayout.setVisibility(View.VISIBLE);
						if (protect) {
							payloadEdit.setEnabled(false);
							payloadEdit.setText("******");
						} else {
							payloadEdit.setEnabled(!isRunning);
							payloadEdit.setText(mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY));
						}
					} else {
						connectionCardview.setVisibility(View.VISIBLE);
						proxyLayout.setVisibility(View.VISIBLE);
						payloadLayout.setVisibility(View.GONE);
						tunnelInfo.setText(getString(R.string.http));
					} 	
					break;

				case Settings.bTUNNEL_TYPE_SSL_PROXY:
					if (protect) {
						sslLayout.setEnabled(false);
						sniText.setText("******");
					} else {
						sslLayout.setEnabled(!isRunning);
						String ssl = mConfig.getPrivString(Settings.TUNNEL_TYPE_SSL_PROXY);
						if (ssl == null) {
							sniText.setText("Ex: www.google.com");
						} else {
							sniText.setText(ssl);	
						}
					}
					connectionCardview.setVisibility(View.VISIBLE);
					payloadLayout.setVisibility(View.GONE);
					proxyLayout.setVisibility(View.GONE);
					sslLayout.setVisibility(View.VISIBLE);
					tunnelInfo.setText(getString(R.string.ssl));
					break;

				case Settings.bTUNNEL_TYPE_SLOWDNS:
					connectionCardview.setVisibility(View.GONE);		
					tunnelInfo.setText(getString(R.string.slowdns));

					break;
			}


		}
		private void generator(){

			paygen.setDialogTitle(getString(R.string.pay_gen));
			paygen.setCancelListener(getString(R.string.cancel), new PayloadGenerator.OnCancelListener(){

					@Override
					public void onCancelListener() {
					}
				});
			paygen.setGenerateListener(getString(R.string.gen), new PayloadGenerator.OnGenerateListener(){

					@Override
					public void onGenerate(String payloadGenerated) {              
						SharedPreferences prefs = mConfig.getPrefsPrivate();
						if (!prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true)){
							payloadEdit.setText(payloadGenerated);     
						} else {
							Toast.makeText(MainActivity.this, R.string.custom_payload_msg, Toast.LENGTH_SHORT).show();
						}                                              
					}
				});
			paygen.show();
		}


		/**
		 * Tunnel SSH
		 */

		public void doTabs() {
			LinearLayoutManager layoutManager = new LinearLayoutManager(this);
			//deleteLogs = (FloatingActionButton)findViewById(R.id.delete_log);
			mLogAdapter = new LogsAdapter(layoutManager,this);
			logList = (RecyclerView) findViewById(R.id.recyclerLog);
			logList.setAdapter(mLogAdapter);
			logList.setLayoutManager(layoutManager);
			mLogAdapter.scrollToLastPosition();
			vp = (ViewPager)findViewById(R.id.viewpager);
			tabs = (TabLayout)findViewById(R.id.tablayout);
			vp.setAdapter(new MyAdapter(Arrays.asList(tabTitle)));
			vp.setOffscreenPageLimit(2);
			tabs.setTabMode(TabLayout.MODE_FIXED);
			tabs.setTabGravity(TabLayout.GRAVITY_FILL);
			tabs.setupWithViewPager(vp);
			vp.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
				{
					@Override
					public void onPageSelected(int position)
					{
						if (position == 0) {
							settings.setIcon(R.drawable.ic_settings);
							//ifolder.setVisible(true);
							//ifolder.setIcon(R.drawable.ic_config);
							isHomeTab = true;
						} else if (position == 1) {                                        
							//ifolder.setVisible(false);
							settings.setIcon(R.drawable.ic_delete_forever_white_24dp);
							isHomeTab = false;
						}
					}
				});

		}

		public class MyAdapter extends PagerAdapter {

			@Override
			public int getCount() {
				// TODO: Implement this method
				return 2;
			}

			@Override
			public boolean isViewFromObject(View p1, Object p2) {
				// TODO: Implement this method
				return p1 == p2;
			}

			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				int[] ids = new int[]{R.id.tab1, R.id.tab2};
				int id = 0;
				id = ids[position];
				// TODO: Implement this method
				return findViewById(id);
			}

			@Override
			public CharSequence getPageTitle(int position)
			{
				// TODO: Implement this method
				return titles.get(position);
			}

			private List<String> titles;
			public MyAdapter(List<String> str)
			{
				titles = str;
			}
		}

		public void startOrStopTunnel(Activity activity) {
			if (SkStatus.isTunnelActive()) {
				TunnelManagerHelper.stopSocksHttp(activity);
			}
			else {
				// oculta teclado se vísivel, tá com bug, tela verde
				//Utils.hideKeyboard(activity);
				Settings config = new Settings(activity);
				Intent intent = new Intent(activity, LaunchVpn.class);
				intent.setAction(Intent.ACTION_MAIN);
				if (config.getHideLog()) {
					intent.putExtra(LaunchVpn.EXTRA_HIDELOG, true);
				}
				activity.startActivity(intent);
			}
		}

		public void setStarterButton(Button starterButton, Activity activity) {
			String state = SkStatus.getLastState();
			boolean isRunning = SkStatus.isTunnelActive();

			if (starterButton != null) {
				int resId;
				SharedPreferences prefsPrivate = new Settings(activity).getPrefsPrivate();
				if (ConfigParser.isValidadeExpirou(prefsPrivate
												   .getLong(Settings.CONFIG_VALIDADE_KEY, 0))) {
					resId = R.string.expired;
					starterButton.setEnabled(false);
					if (isRunning) {
						startOrStopTunnel(activity);              
					}
				}
				else if (prefsPrivate.getBoolean(Settings.BLOQUEAR_ROOT_KEY, false) &&
						 ConfigParser.isDeviceRooted(activity)) {
					resId = R.string.blocked;
					starterButton.setEnabled(false);

					Toast.makeText(activity, R.string.error_root_detected, Toast.LENGTH_SHORT)
						.show();

					if (isRunning) {
						startOrStopTunnel(activity);
					}
				}
				else if (SkStatus.SSH_STARTING.equals(state)) {
					resId = R.string.stop;
					starterButton.setEnabled(false);
				} else if (SkStatus.SSH_STOPPING.equals(state)) {           
					resId = R.string.state_stopping;
					starterButton.setEnabled(false);
				}
				else {
					resId = isRunning ? R.string.stop : R.string.start;
					starterButton.setEnabled(true);
				}

				starterButton.setText(resId);
			}
		}

		/**
		 * Drawer Main
		 */

		private DrawerLayout drawerLayout;
		private ActionBarDrawerToggle toggle;

		public void doDrawerMain(Toolbar toolbar) {
			drawerNavigationView = (NavigationView) findViewById(R.id.drawerNavigationView);
			drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayoutMain);
			// set drawer
			toggle = new ActionBarDrawerToggle(this,
											   drawerLayout, toolbar, R.string.open, R.string.cancel);

			drawerLayout.setDrawerListener(toggle);

			toggle.syncState();

			// set app info
			PackageInfo pinfo = Utils.getAppInfo(this);
			if (pinfo != null) {
				String version_nome = pinfo.versionName;
				int version_code = pinfo.versionCode;
				String header_text = String.format("%s (%d)", version_nome, version_code);

				View view = drawerNavigationView.getHeaderView(0);

				TextView app_info_text = view.findViewById(R.id.nav_headerAppVersion);
				app_info_text.setText(header_text);
			}
			
			// set navigation view
			drawerNavigationView.setNavigationItemSelectedListener(this);
		}

		@Override
		public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
			super.onPostCreate(savedInstanceState, persistentState);
			if (toggle != null)
				toggle.syncState();
		}

		@Override
		public void onConfigurationChanged(Configuration newConfig) {
			super.onConfigurationChanged(newConfig);
			if (toggle != null)
				toggle.onConfigurationChanged(newConfig);
		}

		@Override
		public void onClick(View p1) {
			SharedPreferences prefs = mConfig.getPrefsPrivate();
			boolean isRunning = SkStatus.isTunnelActive();
			switch (p1.getId()) {
				case R.id.activity_starterButtonMain:
					doSaveData();
					startOrStopTunnel(this);
					break;

				case R.id.tunnelCardView:
					if (!isRunning) {
						if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
							startActivity(new Intent(this, TunnelActivity.class));
						}
					}
					break;

				case R.id.proxyLayout:
					doSaveData();
					if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
						if (!isRunning) {
							DialogFragment fragProxy = new ProxyRemoteDialogFragment();
							fragProxy.show(getSupportFragmentManager(), "proxyDialog");
						}
					}
					break;

				case R.id.sslLayout:
					doSaveData();
					if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
						if (!isRunning) {
							DialogFragment fragProxy = new CustomSNIDialogFragment();
							fragProxy.show(getSupportFragmentManager(), "sni");
						}
					}
					break;
			}
		}

		@Override
		public void onClick(DialogInterface p1, int p2) {
			switch (p2) {
				/*case p1.BUTTON_POSITIVE:
					// tudo ok
					break;
				 */
			}
		}

		@Override
		public void updateState(final String state, String msg, int localizedResId, final ConnectionStatus level, Intent intent) {
			mHandler.post(new Runnable() {
					@Override
					public void run() {
						doUpdateLayout();
					}
				});

			switch (state) {
				case SkStatus.SSH_CONNECTED:
					mHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								//Show ads here
							}
						}, 1000);
					break;
			}
		}


		/**
		 * Recebe locais Broadcast
		 */

		private BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action == null)
					return;

				if (action.equals(UPDATE_VIEWS)) {
					doUpdateLayout();
				}
				else if (action.equals(OPEN_LOGS)) {
					if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
						drawerLayout.openDrawer(GravityCompat.END);
					}
				}
			}
		};


		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.main_menu, menu);
			settings= menu.findItem(R.id.miSettings);
			return true;

		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			SharedPreferences prefs = mConfig.getPrefsPrivate();
			int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);

			if (toggle != null && toggle.onOptionsItemSelected(item)) {
				return true;
			}
			
			// Menu Items
			switch (item.getItemId()) {
				case R.id.miLimparConfig:      
					if (!SkStatus.isTunnelActive()) {
						DialogFragment dialog = new ClearConfigDialogFragment();
						dialog.show(getSupportFragmentManager(), "alertClearConf");
					} else {
						Toast.makeText(this, R.string.error_tunnel_service_execution, Toast.LENGTH_SHORT)
							.show();
					}
					break;

				case R.id.miSettings:
					if (isHomeTab == true) {
						Intent intentSettings = new Intent(this, ConfigGeralActivity.class);
						//intentSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intentSettings);
					} else {
						mLogAdapter.clearLog();
					}
					break;

				case R.id.miLimparLogs:
					break;

				case R.id.miExit:
					if (Build.VERSION.SDK_INT >= 16) {
						finishAffinity();
					}
					System.exit(0);
					break;
			}

			return super.onOptionsItemSelected(item);
		}

		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			int id = item.getItemId();
			switch(id) {  


				case R.id.payload_generator:
					SharedPreferences prefs = mConfig.getPrefsPrivate();
					if (SkStatus.isTunnelActive()) {
						Toast.makeText(this, R.string.error_tunnel_service_execution,
									   Toast.LENGTH_SHORT).show();
					} else if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
						Toast.makeText(this, R.string.payload_locked_msg,
									   Toast.LENGTH_SHORT).show();
					} else {
						generator();
					}          
					break;          
				case R.id.miPhoneConfg:
					PackageInfo app_info = Utils.getAppInfo(getApplicationContext());
					if (app_info != null) {
						String aparelho_marca = Build.BRAND.toUpperCase();
						if (aparelho_marca.equals("SAMSUNG") || aparelho_marca.equals("HUAWEI")) {
							Toast.makeText(getApplicationContext(), R.string.error_no_supported, Toast.LENGTH_SHORT)
								.show();
						}
						else {
							try {
								Intent in = new Intent(Intent.ACTION_MAIN);
								in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								in.setClassName("com.android.settings", "com.android.settings.RadioInfo");
								startActivity(in);
							} catch(Exception e) {
								Toast.makeText(getApplicationContext(), R.string.error_no_supported, Toast.LENGTH_SHORT)
									.show();
							}
						}
					}
					break;

				case R.id.miSettings:
					Intent intent = new Intent(MainActivity.this, ConfigGeralActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					break;

				case R.id.miSettingsSSH:
					SharedPreferences mPrefs = mConfig.getPrefsPrivate();
					int tunnelType = mPrefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);

					if (tunnelType == Settings.bTUNNEL_TYPE_SLOWDNS) {
						Intent intent2 = new Intent(MainActivity.this, ConfigGeralActivity.class);
						intent2.setAction(ConfigGeralActivity.OPEN_SETTINGS_DNS);
						intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent2);
					} else {
						Intent intent2 = new Intent(MainActivity.this, ConfigGeralActivity.class);
						intent2.setAction(ConfigGeralActivity.OPEN_SETTINGS_SSH);
						intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent2);
					}
					break;

				case R.id.miOS:
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle("Get Source Code");
					builder.setMessage("Vanguard VPN is an open source project. Get the source code and help us to improve Vanguard VPN and be a part of Vanguard VPN's success.");
					builder.setPositiveButton("Get Source Code", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thevanguardapps/vanguard-vpn")));
						}
					});
					builder.setNegativeButton("Cancel", null);
					builder.show();
					break;

				case R.id.miAvaliarPlaystore:
					Intent intent3 = new Intent(Intent.ACTION_VIEW,
												Uri.parse("mailto: thevanguardapps@protonmail.com"));
					intent3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent3);
					break;


				case R.id.authentication:      
					if (!SkStatus.isTunnelActive()) {
						DialogFragment dialog = new AuthenticationFragment();
						dialog.show(getSupportFragmentManager(), "alertClearConf");
					} else {
						Toast.makeText(this, R.string.error_tunnel_service_execution, Toast.LENGTH_SHORT)
							.show();
					}
					break;
				case R.id.donate:
					Intent donateIntent = new Intent(this, DonateActivity.class);
					startActivity(donateIntent);
					break;
				case R.id.miAbout:
					Intent aboutIntent = new Intent(this, AboutActivity.class);
					startActivity(aboutIntent);
					break;
			} if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
				drawerLayout.closeDrawers();
			}
			return true;
		}

		@Override
		public void onBackPressed() {

			if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
				drawerLayout.closeDrawers();
			}

			else {
				// mostra opção para sair
				new ExitDialogFragment(this)
					.show(getSupportFragmentManager(), "alertExit");
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			SkStatus.addStateListener(this);
			doSaveData();

		}

		@Override
		protected void onPause()
		{
			super.onPause();

			SkStatus.removeStateListener(this);

		}

		@Override
		protected void onDestroy() {
			super.onDestroy();

			doSaveData();
			LocalBroadcastManager.getInstance(this)
				.unregisterReceiver(mActivityReceiver);

		}


		/**
		 * DrawerLayout Listener
		 */

		@Override
		public void onDrawerOpened(View view) {

		}

		@Override
		public void onDrawerClosed(View view) {

		}

		@Override
		public void onDrawerStateChanged(int stateId) {}
		@Override
		public void onDrawerSlide(View view, float p2) {}


		/**
		 * Utils
		 */

		public static void updateMainViews(Context context) {
			Intent updateView = new Intent(UPDATE_VIEWS);
			LocalBroadcastManager.getInstance(context).sendBroadcast(updateView);
		}
	}

