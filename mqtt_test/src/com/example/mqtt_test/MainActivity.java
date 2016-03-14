package com.example.mqtt_test;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.internal.a.c;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.MotionType;
import com.microsoft.band.sensors.SampleRate;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ClientCertRequest;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity {
	
	// VAriable 

	private BandClient client = null;
	private Button btnStart, btnConsent,b1,b2;
	private TextView txtStatus,txtacc,txtalti,txtcal,txtskin,txtsteps,txtdis;
	String heartStatus,motionType;
	int heartRate;
	float accX,accY,accZ,skinTempreture,pace,speed; 
	int unixTime;
	long calorieCount,totalDistance,stepCount;
	byte[] productionDate;
	private Handler handler = new Handler();
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		  txtStatus = (TextView) findViewById(R.id.txtStatus);
	        txtacc =	(TextView) findViewById(R.id.txtacc);
	        txtcal =	(TextView) findViewById(R.id.txtcal);
	        
	        txtskin=(TextView) findViewById(R.id.txtskin);
	        txtdis=(TextView) findViewById(R.id.txtdis);
	        txtsteps=(TextView) findViewById(R.id.txtsteps);
	        
	        txtalti =(TextView) findViewById(R.id.txtalti);
	        btnStart = (Button) findViewById(R.id.btnStart);
	        b1=(Button) findViewById(R.id.b1);
	        b2=(Button) findViewById(R.id.b2);
	        
	       
	        showUserSettings();
	        
	        
	         FileInputStream is;
	        BufferedReader reader;
	        final File file = new File("/sdcard/text.txt");
try {
	 if (file.exists()) {
	            is = new FileInputStream(file);
	            reader = new BufferedReader(new InputStreamReader(is));
	            String line = reader.readLine();
	            while(line != null){
	                //Log.d("StackOverflow", line);
	                line = reader.readLine();
	            }
	        }
	       
} catch (Exception e) {
	
}
	     //new HeartRateSubscriptionTask().execute();
	/*        try {
	        	handler.post(timedTask);
			} catch (Exception e) {
				// TODO: handle exception
				Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
			}*/

	        
	        b1.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					try {
					startService(new Intent(getBaseContext(), service1.class));
						  
					} catch (Exception e) {
						alert(e.toString());
						// TODO: handle exception
					}
					
				}
				
			});
	        btnStart.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					try {
						stopService(new Intent(getBaseContext(),service1.class));
						
					} catch (Exception e) {
						// TODO: handle exception
					}
					
				}
			});
	        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
	        b2.setOnClickListener(new OnClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onClick(View v) {
					
					new HeartRateConsentTask().execute(reference);
				}
			});
	        
	        
	        
	        
	       
	}
MyReceiver myReceiver;
private class MyReceiver extends BroadcastReceiver{
 
 @Override
 public void onReceive(Context arg0, Intent arg1) {
  // TODO Auto-generated method stub
  try {
	  float[] fdatapassed = {arg1.getFloatExtra("accX", 0),
			  			arg1.getFloatExtra("accy", 0),
			  			arg1.getFloatExtra("accz", 0),
			  			arg1.getFloatExtra("rotX", 0),
			  			arg1.getFloatExtra("rotY", 0),
			  			arg1.getFloatExtra("rotZ", 0),
			  			arg1.getFloatExtra("pace", 0),
			  			arg1.getFloatExtra("speed", 0)  
	  }; 
	  long [] ldatapassed={
			    arg1.getLongExtra("totalDistance", 0),
	  			arg1.getLongExtra("stepCount", 0),
	  			arg1.getLongExtra("cal", 0)
			  
	  };
	  String [] sdatapassed={
			  arg1.getStringExtra("conStat"),
			  arg1.getStringExtra("WearingState"),
			  arg1.getStringExtra("motionType")
	  };
	  
	  StringBuilder builder = new StringBuilder();
	  java.util.Date date1= new java.util.Date();
  
	  builder.append(new Timestamp(date1.getTime()).toString()).append("\nConnection State:"+sdatapassed[0]).append("\nWearing State:"+sdatapassed[1])
	  		 .append("\nAccelerometer X: "+fdatapassed[0]).append("\nAccelerometer Y: "+fdatapassed[1])
	  		.append("\nAccelerometer Z: "+fdatapassed[2]).append("\nRotational Y: "+fdatapassed[3])
	  		.append("\nRotational X: "+fdatapassed[4]).append("\nRotational Y: "+fdatapassed[5])
	  		.append("\nMotion Type: "+sdatapassed[2]).append("\nPace: "+fdatapassed[6]).append("Speed: "+fdatapassed[7])
	  		.append("\nTotal Distance: "+ldatapassed[0]).append("\nStep Count: "+ldatapassed[1])
	  		.append("\nTotal Calories "+ldatapassed[2])
	  
	  ;
  txtacc.setText(builder.toString());
	
} catch (Exception e) {
	alert(e.getMessage());
}
  
  
 }
 
}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_settings:
			Intent i = new Intent(this, UserSettingActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			break;

		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case RESULT_SETTINGS:
			showUserSettings();
			break;

		}

	}
	
	@Override
	protected void onStart() {
	 // TODO Auto-generated method stub
	 
	      //Register BroadcastReceiver
	      //to receive event from our service
		
		try {
			myReceiver = new MyReceiver();
	      IntentFilter intentFilter = new IntentFilter();
	      intentFilter.addAction(service1.MY_ACTION);
	      registerReceiver(myReceiver, intentFilter);
	     
	      //Start our own service
	      
	 super.onStart();
		} catch (Exception e) {
			alert(e.getMessage());
		}
	      
	 
	}
	 
	@Override
	protected void onStop() {
	 // TODO Auto-generated method stub
		try {
			unregisterReceiver(myReceiver);
	 super.onStop();
		} catch (Exception e) {
			alert(e.getMessage());
			// TODO: handle exception
		}
	 
	}

	private static final int RESULT_SETTINGS = 1;

	private int syncFrq;
	private void showUserSettings() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		syncFrq=Integer.parseInt(sharedPrefs.getString("prefSyncFrequency", "2"));
		 
	}

	@Override
	protected void onResume() {
		
		super.onResume();
		txtStatus.setText("");
	}
	
    @Override
	protected void onPause() {
    
		super.onPause();
		
		
	}
	
   

	@Override
    protected void onDestroy() {
    	
    	if (client != null) {
            try {
            	unregisterReceiver(myReceiver);
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (Exception e) {
                // Do nothing as this is happening during destroy
            	alert(e.toString());
            }
        }
        super.onDestroy();
    }
    
	
	
	private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
		@Override
		protected Void doInBackground(WeakReference<Activity>... params) {
			try {
				if (getConnectedBandClient()) {
					
					if (params[0].get() != null) {
					
						client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
							@Override
							public void userAccepted(boolean consentGiven) {
							}
					    });
						
					}
				} else {
				alert("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				

			} catch (Exception e) {
				
			}
			return null;
		}
	}
    
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				//alert("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		//appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
	}

	private void alert (String string){
		Toast.makeText(getApplicationContext(), string, Toast.LENGTH_LONG).show();
	}
	
}
