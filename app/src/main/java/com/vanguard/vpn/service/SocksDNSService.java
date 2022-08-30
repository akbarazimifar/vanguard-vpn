package com.vanguard.vpn.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.content.Context;
import com.vanguard.vpn.logger.SkStatus;

import android.os.Handler;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.vanguard.vpn.aidl.IUltraSSHServiceInternal;

import android.annotation.TargetApi;
import android.os.Build;
import android.app.Notification;
import com.vanguard.vpn.logger.ConnectionStatus;
import android.support.annotation.NonNull;
import android.app.NotificationManager;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.support.annotation.RequiresApi;
import com.vanguard.vpn.tunnel.TunnelUtils;
import com.vanguard.vpn.tunnel.TunnelManagerThread;
import android.content.BroadcastReceiver;
import com.vanguard.vpn.config.Settings;

import android.app.NotificationChannel;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.os.Vibrator;
import com.vanguard.vpn.core.MainReceiver;
import com.vanguard.vpn.R;

import android.content.SharedPreferences;
import com.vanguard.vpn.tunnel.DNSTunnelThread;

public class SocksDNSService extends Service
implements SkStatus.StateListener
{
    private static final String TAG = SocksDNSService.class.getSimpleName();
    public static final String START_SERVICE = "com.vanguard.vpn.startTunnel";
    private static final int PRIORITY_MIN = -2;
    private static final int PRIORITY_DEFAULT = 0;
    private static final int PRIORITY_MAX = 2;
    private boolean mNotificationShowing = false;
    private NotificationManager mNotificationManager;
    private Handler mHandler;
    private Settings mPrefs;
    private Thread mTunnelThread;
    private TunnelManagerThread mTunnelManager;
    private ConnectivityManager connMgr;
    
    private final IBinder mBinder = new IUltraSSHServiceInternal.Stub() {

        @Override
        public void stopVPN() {
            SocksDNSService.this.stopTunnel();
        }

    };

	private DNSTunnelThread mDnsThread;

    @Override
    public void onCreate()
    {
        Log.i(TAG, "onCreate");

        super.onCreate();

        mPrefs = new Settings(this);
        mHandler = new Handler();
        connMgr = (ConnectivityManager) this
            .getSystemService(Context.CONNECTIVITY_SERVICE);


        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(TAG, "onStartCommand");

        startTunnelBroadcast();

        SkStatus.addStateListener(this);

        if (intent != null && START_SERVICE.equals(intent.getAction()))
            return START_NOT_STICKY;

        String stateMsg = getString(SkStatus.getLocalizedState(SkStatus.getLastState()));
        showNotification(stateMsg,
                         stateMsg, NOTIFICATION_CHANNEL_NEWSTATUS_ID, 0, ConnectionStatus.LEVEL_START, null);

        new Thread(new Runnable() {
                @Override
                public void run() {
                    startTunnel();
                }
            }).start();

        //return Service.START_STICKY;
        return Service.START_NOT_STICKY;
    }


    /**
     * Tunnel
     */

    public synchronized void startTunnel() {

        SkStatus.updateStateString(SkStatus.SSH_STARTING, getString(R.string.starting_service_ssh));

        networkStateChange(this, true);

        SkStatus.logInfo(String.format("Local IP: %s", getIpPublic()));

        try {
   
			SharedPreferences prefs = mPrefs.getPrefsPrivate();
			int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
			
			if (tunnelType == Settings.bTUNNEL_TYPE_SLOWDNS) {
				mPrefs.setBypass(true);
				mDnsThread = new DNSTunnelThread(this);
				mDnsThread.start();
			}
            mTunnelManager = new TunnelManagerThread(mHandler, this);
            mTunnelManager.setOnStopClienteListener(new TunnelManagerThread.OnStopCliente() {
                    @Override
                    public void onStop() {
                        endTunnelService();
                    }
                });

            mTunnelThread = new Thread(mTunnelManager);
            mTunnelThread.start();

            SkStatus.logInfo(R.string.tunnel_start);

        } catch(Exception e) {
            SkStatus.logException(e);
            endTunnelService();
        }
    }

    public synchronized void stopTunnel() {
		SharedPreferences prefs = mPrefs.getPrefsPrivate();
		int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);

		if (tunnelType == Settings.bTUNNEL_TYPE_SLOWDNS) {
			mPrefs.setBypass(false);
			if (mDnsThread != null) {
				mDnsThread.interrupt();
			}
			mDnsThread = null;
		}
		if (mTunnelManager != null) {
            mTunnelManager.stopAll();

            networkStateChange(this, true);

            if (mTunnelThread != null) {

                mTunnelThread.interrupt();

                SkStatus.logInfo(R.string.tunnel_stop);
            }

            mTunnelManager = null;
        }
    }

    protected String getIpPublic() {

        final android.net.NetworkInfo network = connMgr
            .getActiveNetworkInfo();

        if (network != null && network.isConnectedOrConnecting()) {
            return TunnelUtils.getLocalIpAddress();
        }
        else {
            return "Indisponivel";
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "onDestroy");

        super.onDestroy();

        stopTunnel();

        stopTunnelBroadcast();

        SkStatus.removeStateListener(this);
    }

    public void endTunnelService() {
        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopForeground(true);

                    stopSelf();
                    SkStatus.removeStateListener(SocksDNSService.this);
                }
            });
    }


    /**
     * Notificação
     */
    public static final String NOTIFICATION_CHANNEL_BG_ID = "openvpn_bg";
    public static final String NOTIFICATION_CHANNEL_NEWSTATUS_ID = "openvpn_newstat";
    public static final String NOTIFICATION_CHANNEL_USERREQ_ID = "openvpn_userreq";
    private void connected()
    {
        Vibrator vb_service = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vb_service.vibrate(150); 

    } 
    private String lastChannel;
    private void showNotification(final String msg, String tickerText, @NonNull String channel, long when, ConnectionStatus status, Intent intent) {
        int icon = getIconByConnectionStatus(status);
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Notification.Builder mNotifyBuilder =	null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(mNotificationManager);
            mNotifyBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        } else if (Build.VERSION.SDK_INT >= 28) {
            createNotificationChannels(mNotificationManager);
            mNotifyBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_NEWSTATUS_ID);
        } else {
            mNotifyBuilder = new Notification.Builder(this);
		}
              mNotifyBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setOnlyAlertOnce(true)
                .setOngoing(true);

            // Try to set the priority available since API 16 (Jellybean)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                addVpnActionsToNotification(mNotifyBuilder);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                lpNotificationExtras(mNotifyBuilder, Notification.CATEGORY_SERVICE);
       

        int priority;
        if (channel.equals(NOTIFICATION_CHANNEL_BG_ID))
            priority = PRIORITY_MIN;
        else if (channel.equals(NOTIFICATION_CHANNEL_USERREQ_ID))
            priority = PRIORITY_MAX;
        else
            priority = PRIORITY_DEFAULT;

        mNotifyBuilder.setSmallIcon(icon);
        mNotifyBuilder.setContentText(msg);

        if (status == ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT) {
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
            mNotifyBuilder.setContentIntent(pIntent);
        }
        else {
            mNotifyBuilder.setContentIntent(getGraphPendingIntent(this));
        }

        if (when != 0)
            mNotifyBuilder.setWhen(when);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            jbNotificationExtras(priority, mNotifyBuilder);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //noinspection NewApi
            mNotifyBuilder.setChannelId(channel);
        }

        if (tickerText != null && !tickerText.equals(""))
            mNotifyBuilder.setTicker(tickerText);

        Notification notification = mNotifyBuilder.build();

        int notificationId = channel.hashCode();

        mNotificationManager.notify(notificationId, notification);

        startForeground(notificationId, notification);

        if (lastChannel != null && !channel.equals(lastChannel)) {
            // Cancel old notification
            mNotificationManager.cancel(lastChannel.hashCode());
        }

        lastChannel = channel;
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void lpNotificationExtras(Notification.Builder nbuilder, String category) {
        nbuilder.setCategory(category);
        nbuilder.setLocalOnly(true);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void jbNotificationExtras(int priority,
                                      Notification.Builder nbuilder) {
        try {
            if (priority != 0) {
                Method setpriority = nbuilder.getClass().getMethod("setPriority", int.class);
                setpriority.invoke(nbuilder, priority);

                Method setUsesChronometer = nbuilder.getClass().getMethod("setUsesChronometer", boolean.class);
                setUsesChronometer.invoke(nbuilder, true);
            }

            //ignore exception
        } catch (NoSuchMethodException | IllegalArgumentException |
        InvocationTargetException | IllegalAccessException e) {
            SkStatus.logException(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void addVpnActionsToNotification(Notification.Builder nbuilder) {

        Intent reconnectVPN = new Intent(this, MainReceiver.class);
        reconnectVPN.setAction(MainReceiver.ACTION_SERVICE_RESTART);
        PendingIntent reconnectPendingIntent = PendingIntent.getBroadcast(this, 0, reconnectVPN, PendingIntent.FLAG_CANCEL_CURRENT);

        nbuilder.addAction(R.drawable.ic_autorenew_black_24dp,
                           getString(R.string.reconnect), reconnectPendingIntent);

        }

    private int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                connected();
                return R.drawable.ic_connected;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.ic_connecting;
        }
    }

    // Usado também pelo tunnel VPN
    public static PendingIntent getGraphPendingIntent(Context context) {
        // Let the configure Button show the Log

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, context.getPackageName() + ".MainActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        PendingIntent startLW = PendingIntent.getActivity(context, 0, intent, 0);

        return startLW;
    }
    
    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannels(NotificationManager mNotifyBuilder) {
        NotificationManager mNotificationManager =
            (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        // Background message
        CharSequence name = getString(R.string.channel_name_background);
        NotificationChannel mChannel = new NotificationChannel(SocksDNSService.NOTIFICATION_CHANNEL_BG_ID,
                                                               name, NotificationManager.IMPORTANCE_MIN);

        mChannel.setDescription(getString(R.string.channel_description_background));
        mChannel.enableLights(false);

        mChannel.setLightColor(R.color.colorPrimary);
        mNotificationManager.createNotificationChannel(mChannel);

        // Connection status change messages
        name = getString(R.string.channel_name_status);
        mChannel = new NotificationChannel(SocksDNSService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                                           name, NotificationManager.IMPORTANCE_LOW);

        mChannel.setDescription(getString(R.string.channel_description_status));
        mChannel.enableLights(true);

        mChannel.setLightColor(R.color.colorPrimary);
        mNotificationManager.createNotificationChannel(mChannel);


        // Urgent requests, e.g. two factor auth
        name = getString(R.string.channel_name_userreq);
        mChannel = new NotificationChannel(SocksDNSService.NOTIFICATION_CHANNEL_USERREQ_ID,
                                           name, NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription(getString(R.string.channel_description_userreq));
        mChannel.enableVibration(true);
        mChannel.setLightColor(R.color.colorPrimary);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    /**
     * SkStatus.StateListener
     */

    @Override
    public void updateState(String state, String msg, int resid, ConnectionStatus level, Intent intent) {

        // If the process is not running, ignore any state,
        // Notification should be invisible in this state

        if (mTunnelThread == null && !mNotificationShowing)
            return;

        String channel = NOTIFICATION_CHANNEL_NEWSTATUS_ID;

        if (level == ConnectionStatus.LEVEL_CONNECTED) {
            channel = NOTIFICATION_CHANNEL_USERREQ_ID;
        }

        String stateMsg = getString(SkStatus.getLocalizedState(SkStatus.getLastState()));
        showNotification(stateMsg,
                         stateMsg, channel, 0, level, null);
    }



    /**
     * Tunnel Broadcast
     */

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network net) {
            SkStatus.logDebug("Available network");
        }

        @Override
        public void onLost(Network net) {
            SkStatus.logDebug("Network lost");
        }

        @Override
        public void onUnavailable() {
            SkStatus.logDebug("Network unavailable");
        }
    };

    public static final String TUNNEL_SSH_RESTART_SERVICE = SocksDNSService.class.getName() + "::restartservicebroadcast",
    TUNNEL_SSH_STOP_SERVICE = SocksDNSService.class.getName() + "::stopservicebroadcast";

    private void startTunnelBroadcast() {
        if (Build.VERSION.SDK_INT >= 24) {
            connMgr.registerDefaultNetworkCallback(networkCallback);
        }

        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(TUNNEL_SSH_STOP_SERVICE);
        broadcastFilter.addAction(TUNNEL_SSH_RESTART_SERVICE);

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mTunnelSSHBroadcastReceiver, broadcastFilter);
    }

    private void stopTunnelBroadcast() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(mTunnelSSHBroadcastReceiver);

        if (Build.VERSION.SDK_INT >= 24)
            connMgr.unregisterNetworkCallback(networkCallback);
    }

    private BroadcastReceiver mTunnelSSHBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action == null) {
                return;
            }

            if (action.equals(TUNNEL_SSH_RESTART_SERVICE)) {
                new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (mTunnelManager != null) {
                                mTunnelManager.reconnectSSH();
                            }
                        }
                    }).start();
            }

            else if (action.equals(TUNNEL_SSH_STOP_SERVICE)) {
                endTunnelService();
            }
        }
    };

    private static String lastStateMsg;

    protected void networkStateChange(Context context, boolean showStatusRepetido) {
        String netstatestring;

        try {
            // deprecated in 29
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            if (networkInfo == null) {
                netstatestring = "not connected";
            } else {
                String subtype = networkInfo.getSubtypeName();
                if (subtype == null)
                    subtype = "";
                String extrainfo = networkInfo.getExtraInfo();
                if (extrainfo == null)
                    extrainfo = "";
                netstatestring = String.format("%2$s %4$s to %1$s %3$s", networkInfo.getTypeName(),
                                               networkInfo.getDetailedState(), extrainfo, subtype);
            }

        } catch (Exception e) {
            netstatestring = e.getMessage();
        }

        if (showStatusRepetido || !netstatestring.equals(lastStateMsg))
            SkStatus.logInfo(netstatestring);

        lastStateMsg = netstatestring;
    }
}
