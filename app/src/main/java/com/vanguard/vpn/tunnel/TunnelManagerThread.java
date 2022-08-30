package com.vanguard.vpn.tunnel;

import android.content.Context;
import java.io.IOException;
import com.vanguard.vpn.logger.SkStatus;
import android.content.IntentFilter;
import com.vanguard.vpn.tunnel.vpn.TunnelVpnService;
import android.support.v4.content.LocalBroadcastManager;
import java.util.List;
import com.vanguard.vpn.tunnel.vpn.VpnUtils;
import android.util.Log;
import com.vanguard.vpn.tunnel.vpn.TunnelState;
import android.content.Intent;
import com.vanguard.vpn.tunnel.vpn.TunnelVpnSettings;
import android.content.BroadcastReceiver;

import com.vanguard.vpn.tunnel.vpn.TunnelVpnManager;
import com.vanguard.vpn.config.Settings;
import android.os.Handler;
import java.net.InetAddress;

import com.trilead.ssh2.transport.TransportManager;
import java.util.concurrent.CountDownLatch;
import com.trilead.ssh2.Connection;
import android.widget.Toast;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ProxyData;
import com.trilead.ssh2.DynamicPortForwarder;
import com.trilead.ssh2.ConnectionMonitor;
import com.trilead.ssh2.DebugLogger;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.ServerHostKeyVerifier;
import com.vanguard.vpn.R;
import com.vanguard.vpn.config.PasswordCache;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.os.Build;

public class TunnelManagerThread
implements Runnable, ConnectionMonitor, InteractiveCallback,
ServerHostKeyVerifier, DebugLogger {

	@Override
	public void onReceiveInfo(int infoId, String infoMsg) {
		if (infoId == SERVER_BANNER) {
			SkStatus.logInfo("<strong>" + mContext.getString(R.string.log_server_banner) + "</strong> " + infoMsg);
		}
	}

	private static final String TAG = TunnelManagerThread.class.getSimpleName();
	
	/*ConnectivityManager.NetworkCallback networkVpnCallback = new ConnectivityManager.NetworkCallback() {
		@Override
		public void onAvailable(Network network) {
			boolean connected = false;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				connected = mCmgr.bindProcessToNetwork(network);
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				//noinspection deprecation
				connected = ConnectivityManager.setProcessDefaultNetwork(network);
			}
			
			SkStatus.logInfo("vpn disponibel");

			if (!mConnected && connected) {
				SkStatus.logInfo("<strong>Outro aplicativo VPN em execução foi detectado, pare ele antes</strong>");
				stopAll();
			}
		}

		@Override
		public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities)
		{
			if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
				SkStatus.logInfo(String.format("Vpn detected ou não. Velocidade: %d Kb/s", networkCapabilities.getLinkDownstreamBandwidthKbps()));
			}
		}
		
	};*/
	
	private OnStopCliente mListener;
	private Context mContext;
	private Handler mHandler;
	private Settings mConfig;
	private boolean mRunning = false, mStopping = false, mStarting = false;
	
	private CountDownLatch mTunnelThreadStopSignal;
	//private ConnectivityManager mCmgr;
	
	public interface OnStopCliente {
		void onStop();
	}
	
	public TunnelManagerThread(Handler handler, Context context) {
		mContext = context;
		mHandler = handler;
		
		mConfig = new Settings(context);
	}
	
	public void setOnStopClienteListener(OnStopCliente listener) {
		mListener = listener;
	}

	@Override
	public void run()
	{
		mStarting = true;
		mTunnelThreadStopSignal = new CountDownLatch(1);
		
		SkStatus.logInfo("<strong>" + mContext.getString(R.string.starting_service_ssh) + "</strong>");
		
		// anti vpn sniffer
		/*if (Build.VERSION.SDK_INT >= 21) {
			mCmgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			
			mCmgr.registerNetworkCallback(new NetworkRequest.Builder()
				.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
					.build(), networkVpnCallback);
		}*/
		
		int tries = 0;
		while (!mStopping) {
			try {
				if (!TunnelUtils.isNetworkOnline(mContext)) {
					SkStatus.updateStateString(SkStatus.SSH_WAITING, mContext.getString(R.string.state_nonetwork));

					SkStatus.logInfo(R.string.state_nonetwork);
					
					try {
						Thread.sleep(5000);
					} catch(InterruptedException e2) {
						stopAll();
						break;
					}
				}
				else {
					if (tries > 0)
						SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

					try {
						Thread.sleep(1000);
					} catch(InterruptedException e2) {
						stopAll();
						break;
					}

					startClienteSSH();
					break;
				}
			} catch(Exception e) {

				SkStatus.logError("<strong>" + mContext.getString(R.string.state_disconnected) + "</strong>");
				closeSSH();
				
				try {
					Thread.sleep(3000);
				} catch(InterruptedException e2) {
					stopAll();
					break;
				}
			}
			
			tries++;
		}
		
		mStarting = false;
		
		if (!mStopping) {
			try {
				mTunnelThreadStopSignal.await();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
		if (mListener != null) {
			mListener.onStop();
		}
	}
	
	public void stopAll() {
		if (mStopping) return;
		
		SkStatus.updateStateString(SkStatus.SSH_STOPPING, mContext.getString(R.string.stopping_service_ssh));
		SkStatus.logInfo("<strong>" + mContext.getString(R.string.stopping_service_ssh) + "</strong>");
		
		/*if (mCmgr != null && Build.VERSION.SDK_INT >= 21) {
			mCmgr.unregisterNetworkCallback(networkVpnCallback);
			mCmgr = null;
		}*/
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				mStopping = true;

				if (mTunnelThreadStopSignal != null)
					mTunnelThreadStopSignal.countDown();

				closeSSH();
				
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e){}

				SkStatus.updateStateString(SkStatus.SSH_DISCONNECTED, mContext.getString(R.string.state_disconnected));

				mRunning = false;
				mStarting = false;
				mReconnecting = false;
			}
		}).start();
	}
	
	
	/**
	 * Forwarder
	*/

	protected void startForwarder(int portaLocal) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}
		
		startForwarderSocks(portaLocal);
		
		startTunnelVpnService();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (!mConnected) break;
					
					try {
						Thread.sleep(2000);
					} catch(InterruptedException e) {
						break;
					}
				}
			}
		}).start();
	}

	protected void stopForwarder() {
		stopTunnelVpnService();
		
		stopForwarderSocks();
	}
	
	
	/**
	* Cliente SSH
	*/
	
	private final static int AUTH_TRIES = 1;
	private final static int RECONNECT_TRIES = 5;
	
	private Connection mConnection;
	
	private boolean mConnected = false;
	
	protected void startClienteSSH() throws Exception {
		mStopping = false;
		mRunning = true;
		
		String servidor = mConfig.getPrivString(Settings.SERVIDOR_KEY);
		int porta = Integer.parseInt(mConfig.getPrivString(Settings.SERVIDOR_PORTA_KEY));
		String usuario = mConfig.getPrivString(Settings.USUARIO_KEY);
		
		String _senha = mConfig.getPrivString(Settings.SENHA_KEY);
		String senha = _senha.isEmpty() ? PasswordCache.getAuthPassword(null, false) : _senha;
		
		String keyPath = mConfig.getSSHKeypath();
		int portaLocal = Integer.parseInt(mConfig.getPrivString(Settings.PORTA_LOCAL_KEY));

		try {
			
			conectar(servidor, porta);

			for (int i = 0; i < AUTH_TRIES; i++) {
				if (mStopping) {
					return;
				}

				try {
					autenticar(usuario, senha, keyPath);

					break;
				} catch(IOException e) {
					if (i+1 >= AUTH_TRIES) {
						throw new IOException(mContext.getString(R.string.state_auth_failed));
					}
					else {
						try {
							Thread.sleep(3000);
						} catch(InterruptedException e2) {
							return;
						}
					}
				}
			}

			SkStatus.updateStateString(SkStatus.SSH_CONNECTED, "SSH connection established");
			SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_connected) + "</strong>");		
			startForwarder(portaLocal);

		} catch(Exception e) {
			mConnected = false;

			throw e;
		}
	}
	
	public synchronized void closeSSH() {
		stopForwarder();

		if (mConnection != null) {
			SkStatus.logDebug("Stopping SSH");
			mConnection.close();
		}
	}
	
	protected void conectar(String servidor, int porta) throws Exception {
		if (!mStarting) {
			throw new Exception();
		}
		
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		// aqui deve conectar
		try {

			mConnection = new Connection(servidor, porta);
			if (mConfig.getModoDebug() && !prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				// Desativado, pois estava enchendo o Logger
				//mConnection.enableDebugging(true, this);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(mContext, "Debug mode enabled",
							Toast.LENGTH_SHORT).show();
					}
				});
			}

			// proxy
			addProxy(prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false), prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT),
				(!prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true) ? mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY) : null),
					mConnection);

			// monitora a conexão
			mConnection.addConnectionMonitor(this);
			
			if (Build.VERSION.SDK_INT >= 23) {
				ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				ProxyInfo proxy = cm.getDefaultProxy();
				if (proxy != null) {
					SkStatus.logInfo("<strong>Network Proxy:</strong> " + String.format("%s:%d", proxy.getHost(), proxy.getPort()));
				}
			}
			
			SkStatus.updateStateString(SkStatus.SSH_CONNECTING, mContext.getString(R.string.state_connecting));
			SkStatus.logInfo(R.string.ssh_connecting);
			mConnection.connect(this, 10*1000, 20*1000);

			mConnected = true;

		} catch(Exception e) {

			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));

			String cause = e.getCause().toString();
			if (useProxy && cause.contains("Key exchange was not finished")) {
				SkStatus.logError("Proxy: lost connection");
			}
			else {
				SkStatus.logError("SSH: " + cause);
			}
			
			throw new Exception(e);
		}
	}


	/**
	 * Autenticação
	 */

	private static final String AUTH_PUBLICKEY = "publickey",
			AUTH_PASSWORD = "password";

	protected void autenticar(String usuario, String senha, String keyPath) throws IOException {
		if (!mConnected) {
			throw new IOException();
		}
		
		SkStatus.updateStateString(SkStatus.SSH_AUTHENTICATING, mContext.getString(R.string.state_auth));

		try {
			if (mConnection.isAuthMethodAvailable(usuario,
				AUTH_PASSWORD)) {

				SkStatus.logInfo(R.string.pass_auth);
					
				if (mConnection.authenticateWithPassword(usuario,
						senha)) {
					SkStatus.logInfo(mContext.getString(R.string.state_auth_success));
				}
			}
		} catch (IllegalStateException e) {
			Log.e(TAG,
				  "Connection went away while we were trying to authenticate",
				  e);
		} catch (Exception e) {
			Log.e(TAG, "Problem during handleAuthentication()", e);
		}

		try {
			if (mConnection.isAuthMethodAvailable(usuario,
					AUTH_PUBLICKEY) && keyPath != null && !keyPath.isEmpty()) {
				File f = new File(keyPath);
				if (f.exists()) {
					if (senha.equals("")) senha = null;
					SkStatus.logInfo(R.string.auth_public_key);
					if (mConnection.authenticateWithPublicKey(usuario, f,
							senha)) {
						SkStatus.logInfo(mContext.getString(R.string.state_auth_success));
					}
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "Host does not support 'Public key' authentication.");
		}

		if (!mConnection.isAuthenticationComplete()) {
			SkStatus.logInfo(R.string.state_auth_failed);
            SkStatus.logInfo(R.string.incorrect_pass_user);
			throw new IOException(mContext.getString(R.string.unable_auth));
		}
	}

	// XXX: Is it right?
	@Override
	public String[] replyToChallenge(String name, String instruction,
			int numPrompts, String[] prompt, boolean[] echo) throws Exception {
		String[] responses = new String[numPrompts];
		for (int i = 0; i < numPrompts; i++) {
			// request response from user for each prompt
			if (prompt[i].toLowerCase().contains("password"))
				responses[i] = mConfig.getPrivString(Settings.SENHA_KEY);
		}
		return responses;
	}


	/**
	 * ServerHostKeyVerifier
	 * Fingerprint
	 */

	@Override
	public boolean verifyServerHostKey(String hostname, int port,
		String serverHostKeyAlgorithm, byte[] serverHostKey)
	throws Exception {
		String fingerPrint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
		SkStatus.logInfo(R.string.fingerprint + fingerPrint);
		//Log.d(TAG, "Finger Print Type: " + "");
		return true;
	}


	/**
	 * Proxy
	 */

	private boolean useProxy = false;

	protected void addProxy(boolean isProteger, int mTunnelType, String mCustomPayload, Connection conn) throws Exception {

		if (mTunnelType != 0) {
			useProxy = true;
			switch (mTunnelType) {
				case Settings.bTUNNEL_TYPE_SSH_DIRECT:
					if (mCustomPayload != null) {
						try {
							ProxyData proxyData = new HttpProxyCustom(mConfig.getPrivString(Settings.SERVIDOR_KEY), Integer.parseInt(mConfig.getPrivString(Settings.SERVIDOR_PORTA_KEY)),
								null, null, mCustomPayload, true, mContext);

							conn.setProxyData(proxyData);

							if (!mCustomPayload.isEmpty() && !isProteger)
								SkStatus.logInfo(R.string.payload + mCustomPayload);

						} catch(Exception e) {
							throw new Exception(mContext.getString(R.string.error_proxy_invalid));
						}
					}
					else {
						useProxy = false;
					}
				break;

				case Settings.bTUNNEL_TYPE_SSH_PROXY:
					String customPayload = mCustomPayload;

					if (customPayload != null && customPayload.isEmpty()) {
						customPayload = null;
					}

					String servidor = mConfig.getPrivString(Settings.PROXY_IP_KEY);
					int porta = Integer.parseInt(mConfig.getPrivString(Settings.PROXY_PORTA_KEY));

					try {
						ProxyData proxyData = new HttpProxyCustom(servidor, porta,
							null, null, customPayload, false, mContext);

						if (!isProteger)
							SkStatus.logInfo(String.format("Remote Proxy: %s:%d", servidor, porta));
						conn.setProxyData(proxyData);

						if (customPayload != null && !customPayload.isEmpty() && !isProteger)
							SkStatus.logInfo(R.string.payload + customPayload);

					} catch(Exception e) {
						SkStatus.logError(R.string.error_proxy_invalid);

						throw new Exception(mContext.getString(R.string.error_proxy_invalid));
					}
				break;
				case Settings.bTUNNEL_TYPE_SSL_PROXY:
                    String customSNI = mConfig.getPrivString(Settings.TUNNEL_TYPE_SSL_PROXY);
                    if (customSNI != null && customSNI.isEmpty()) {
                        customPayload = null;
                    }

                    String sshServer = mConfig.getPrivString(Settings.SERVIDOR_KEY);
                    int sshPort = Integer.parseInt(mConfig.getPrivString(Settings.SERVIDOR_PORTA_KEY));

                    try {
						SkStatus.logInfo(R.string.ssl_handshake);
						if (!isProteger)
							SkStatus.logInfo(String.format(mContext.getString(R.string.host_sni, customSNI)));
						
                        ProxyData sslTypeData = new SSLTunnelProxy(sshServer, sshPort, customSNI);
                        conn.setProxyData(sslTypeData);

                    }catch(Exception e) {
                        SkStatus.logInfo(e.getMessage());
                    }
                    break;
					/*case Prefs.TUNNEL_TYPE_SSH_HTTP:
					 SkStatus.logInfo("Usando Tunnel HTTP");

					 String servidorHttp = "165.227.48.122";
					 int portaHttp = 80;

					 ProxyData pData = new HttpTunnelCliente(servidorHttp, portaHttp);

					 SkStatus.logInfo(String.format("Proxy: %s:%d", servidorHttp, portaHttp));
					 conn.setProxyData(pData);
					 break;*/
				case Settings.bTUNNEL_TYPE_SLOWDNS:
					if (mCustomPayload != null) {
						try {
							ProxyData proxyData = new HttpProxyCustom("127.0.0.1", Integer.parseInt("8989"),
																	  null, null, mCustomPayload, true, mContext);

							conn.setProxyData(proxyData);

							if (!mCustomPayload.isEmpty() && !isProteger)
								SkStatus.logInfo(R.string.payload + mCustomPayload);

						} catch(Exception e) {
							throw new Exception(mContext.getString(R.string.error_proxy_invalid));
						}
					}
					else {
						useProxy = false;
					}
					break;
				default: useProxy = false;
			}
		}
	}


	/**
	 * Socks5 Forwarder
	 */

	private DynamicPortForwarder dpf;

	private synchronized void startForwarderSocks(int portaLocal) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}

		SkStatus.logInfo(R.string.local_socks);
		SkStatus.logDebug(String.format("socks local listen: %d", portaLocal));
		
		try {		
			dpf = mConnection.createDynamicPortForwarder(portaLocal);
		} catch (Exception e) {
			SkStatus.logError(R.string.socks_local + e.getCause().toString());

			throw new Exception();
		}
	}

	private synchronized void stopForwarderSocks() {
		if (dpf != null) {
			try {
				dpf.close(); 
			} catch(IOException e){}
			dpf = null;
		}
	}


	/**
	 * Pinger
	 */
	/**
	 * Connection Monitor
	 */

	@Override
	public void connectionLost(Throwable reason)
	{
		if (mStarting || mStopping || mReconnecting) {
			return;
		}	
		SkStatus.logError("<strong>" + mContext.getString(R.string.log_conection_lost) + "</strong>");
		if (reason != null) {
			if (reason.getMessage().contains(
					"There was a problem during connect")) {
				return;
			} else if (reason.getMessage().contains(
						   "Closed due to user request")) {
				return;
			} else if (reason.getMessage().contains(
						   "The connect timeout expired")) {
				stopAll();
				return;
			}
		} else {
			stopAll();
			return;
		}
		
		reconnectSSH();
	}
	
	public boolean mReconnecting = false;
	
	public void reconnectSSH() {
		if (mStarting || mStopping || mReconnecting) {
			return;
		}
		
		mReconnecting = true;
		
		closeSSH();
		
		SkStatus.updateStateString(SkStatus.SSH_RECONECTANDO, "Reconnecting..");

		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
			mReconnecting = false;
			return;
		}

		for (int i = 0; i < RECONNECT_TRIES; i++) {
			if (mStopping) {
				mReconnecting = false;
				return;
			}

			int sleepTime = 5;
			if (!TunnelUtils.isNetworkOnline(mContext)) {
				SkStatus.updateStateString(SkStatus.SSH_WAITING, "Waiting for network...");

				SkStatus.logInfo(R.string.state_nonetwork);
			}
			else {
				sleepTime = 3;
				mStarting = true;
				SkStatus.updateStateString(SkStatus.SSH_RECONECTANDO, "Reconnecting..");

				SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

				try {
					startClienteSSH();
					
					mStarting = false;
					mReconnecting = false;
					//mConnected = true;

					return;
				} catch(Exception e) {
					SkStatus.logInfo("<strong>" + mContext.getString(R.string.state_disconnected) + "</strong>");
				}
				
				mStarting = false;
			}

			try {
				Thread.sleep(sleepTime*1000);
				i--;
			} catch(InterruptedException e2){
				mReconnecting = false;
				return;
			}
		}
		
		mReconnecting = false;

		stopAll();
	}

	/**
	 * Debug Logger
	 */

	@Override
	public void log(int level, String className, String message)
	{
		SkStatus.logDebug(String.format("%s: %s", className, message));
	}
	

	/**
	 * Vpn Tunnel
	 */
	 
	String serverAddr;

	protected void startTunnelVpnService() throws IOException {
		if (!mConnected) {
			throw new IOException();
		}
		
		SkStatus.logInfo(R.string.service_tunnel_start);
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		// Broadcast
		IntentFilter broadcastFilter =
			new IntentFilter(TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST);
		broadcastFilter.addAction(TunnelVpnService.TUNNEL_VPN_START_BROADCAST);
		// Inicia Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.registerReceiver(m_vpnTunnelBroadcastReceiver, broadcastFilter);

		String m_socksServerAddress = String.format("127.0.0.1:%s", mConfig.getPrivString(Settings.PORTA_LOCAL_KEY));
		boolean m_dnsForward = mConfig.getVpnDnsForward();
		String m_udpResolver = mConfig.getVpnUdpForward() ? mConfig.getVpnUdpResolver() : null;

		String servidorIP = mConfig.getPrivString(Settings.SERVIDOR_KEY);

		if (prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT) == Settings.bTUNNEL_TYPE_SSH_PROXY) {
			try {
				servidorIP = mConfig.getPrivString(Settings.PROXY_IP_KEY);
			} catch(Exception e) {
				SkStatus.logError(R.string.error_proxy_invalid);
				
				throw new IOException(mContext.getString(R.string.error_proxy_invalid));
			}
		}

		try {
			InetAddress servidorAddr = TransportManager.createInetAddress(servidorIP);
			serverAddr = servidorIP = servidorAddr.getHostAddress();
		} catch(Exception e) {
			throw new IOException(mContext.getString(R.string.error_server_ip_invalid));
		}
		
		String[] m_excludeIps = {servidorIP};

		String[] m_dnsResolvers = null;
		if (m_dnsForward) {
			m_dnsResolvers = new String[]{mConfig.getVpnDnsResolver()};
		}
		else {
			List<String> lista = VpnUtils.getNetworkDnsServer(mContext);
			m_dnsResolvers = new String[]{lista.get(0)};
		}

		if (isServiceVpnRunning()) {
			Log.d(TAG, "already running service");

			TunnelVpnManager tunnelManager = TunnelState.getTunnelState()
				.getTunnelManager();
			
			if (tunnelManager != null) {
				tunnelManager.restartTunnel(m_socksServerAddress);
			}

			return;
		}

		Intent startTunnelVpn = new Intent(mContext, TunnelVpnService.class);
		startTunnelVpn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		TunnelVpnSettings settings = new TunnelVpnSettings(m_socksServerAddress, m_dnsForward, m_dnsResolvers,
			(m_dnsForward && m_udpResolver == null || !m_dnsForward && m_udpResolver != null), m_udpResolver, m_excludeIps,
				mConfig.getIsFilterApps(), mConfig.getIsFilterBypassMode(), mConfig.getFilterApps(), mConfig.getIsTetheringSubnet(), mConfig.getBypass());
		startTunnelVpn.putExtra(TunnelVpnManager.VPN_SETTINGS, settings);

		if (mContext.startService(startTunnelVpn) == null) {
			SkStatus.logInfo(R.string.service_tunnel_failed);

			throw new IOException(mContext.getString(R.string.vpn_service_failed));
		}

		TunnelState.getTunnelState().setStartingTunnelManager();
	}

	public static boolean isServiceVpnRunning() {
		TunnelState tunnelState = TunnelState.getTunnelState();
		return tunnelState.getStartingTunnelManager() || tunnelState.getTunnelManager() != null;
	}

	protected synchronized void stopTunnelVpnService() {
		if (!isServiceVpnRunning()) {
			return;
		}
		
		// Use signalStopService to asynchronously stop the service.
		// 1. VpnService doesn't respond to stopService calls
		// 2. The UI will not block while waiting for stopService to return
		// This scheme assumes that the UI will monitor that the service is
		// running while the Activity is not bound to it. This is the state
		// while the tunnel is shutting down.
		SkStatus.logInfo(R.string.service_tunnel_stopping);
		
		TunnelVpnManager currentTunnelManager = TunnelState.getTunnelState()
			.getTunnelManager();
		
		if (currentTunnelManager != null) {
			currentTunnelManager.signalStopService();
		}
		
		/*if (mThreadLocation != null && mThreadLocation.isAlive()) {
			mThreadLocation.interrupt();
		}
		mThreadLocation = null;*/

		// Parando Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.unregisterReceiver(m_vpnTunnelBroadcastReceiver);
	}
	
	//private Thread mThreadLocation;

	// Local BroadcastReceiver
	private BroadcastReceiver m_vpnTunnelBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public synchronized void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (TunnelVpnService.TUNNEL_VPN_START_BROADCAST.equals(action)) {
				boolean startSuccess = intent.getBooleanExtra(TunnelVpnService.TUNNEL_VPN_START_SUCCESS_EXTRA, true);

				if (!startSuccess) {
					stopAll();
				}
				else {
					/*mThreadLocation = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(3000);
							} catch(InterruptedException e) {
								return;
							}
							try {
								if (serverAddr != null) {
									SkStatus.logInfo(String.format(
										"Server Localização: %s", TunnelUtils.getLocationIp(serverAddr)));
								}
							} catch (IOException e) {
								SkStatus.logDebug("Server Location Error: " + e.getMessage());
							}
						}
					});
					mThreadLocation.start();*/
				}
				
			} else if (TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST.equals(action)) {
				stopAll();
			}
		}
	};
	
}
