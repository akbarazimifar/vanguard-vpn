package com.vanguard.vpn.tunnel;

import android.content.Intent;
import android.os.Build;
import android.content.Context;
import com.vanguard.vpn.service.SocksDNSService;
import android.support.v4.content.LocalBroadcastManager;

public class TunnelManagerHelper
{
	public static void startSocksHttp(Context context) {
        Intent startVPN = new Intent(context, SocksDNSService.class);
		
		if (startVPN != null) {
			TunnelUtils.restartRotate();
			
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			//noinspection NewApi
                context.startForegroundService(startVPN);
            else
                context.startService(startVPN);
        }
    }
	
	public static void stopSocksHttp(Context context) {
		Intent stopTunnel = new Intent(SocksDNSService.TUNNEL_SSH_STOP_SERVICE);
		LocalBroadcastManager.getInstance(context)
			.sendBroadcast(stopTunnel);
	}
}
