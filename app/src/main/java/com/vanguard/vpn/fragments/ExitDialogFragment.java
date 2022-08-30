package com.vanguard.vpn.fragments;

import android.app.Dialog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.DialogInterface;
import com.vanguard.vpn.R;
import android.support.v4.app.DialogFragment;
import android.content.Intent;
import android.app.Activity;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Handler;
import com.vanguard.vpn.service.SocksDNSService;

public class ExitDialogFragment extends DialogFragment {
	private Activity mActivity;
	private Handler mHandler;
	
	public ExitDialogFragment(Activity activity) {
		mActivity = activity;
		mHandler = new Handler();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog dialog = new AlertDialog.Builder(getActivity()).
			create();
		dialog.setTitle(getActivity().getString(R.string.attention));
		dialog.setMessage(getActivity().getString(R.string.alert_exit));
		
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, getActivity().getString(R.
			string.exit),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Intent stopTunnel = new Intent(SocksDNSService.TUNNEL_SSH_STOP_SERVICE);
					LocalBroadcastManager.getInstance(getContext())
						.sendBroadcast(stopTunnel);
					
					if (Build.VERSION.SDK_INT >= 16) {
						getActivity().finishAffinity();
					}
					
					System.exit(0);
				}
			}
		);
		
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getString(R.
			string.minimize),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					minimizeApp();
				}
			}
		);
		
		return dialog;
	}
	
	private void minimizeApp() {
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}
}
