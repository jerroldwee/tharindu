package com.example.mqtt_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedIntentReceiver extends BroadcastReceiver{


	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			   Intent pushIntent = new Intent(context, service1.class);
			   context.startService(pushIntent);
			  }
	}
}
