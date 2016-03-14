package com.example.mqtt_test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions; 
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;

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
import com.microsoft.band.sensors.BandContactEvent;
import com.microsoft.band.sensors.BandContactEventListener;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.SampleRate;

import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;

import android.widget.Toast;
import android.annotation.TargetApi;
import android.app.Notification;
// This service is to pull the data from MS health and save them.

public class service1 extends Service {

	private BandClient client = null;

	String heartStatus;

String motionType;
	int heartRate;
	float accX=0;
	float accY=0;
	float accZ=0;
	float skinTempreture;
	float pace;
	float speed; 
	int unixTime;
	int SensorFreq=10000;// set the period for sending mqtt
	long calorieCount,totalDistance,stepCount;
	byte[] productionDate;
	private Handler handler = new Handler();
	datasaver mds;
	datasaver2 sthread;
	boolean bufferData=false,ready=false;
	String Dtime,WearingState,conStat;
	@Override
	public void onCreate() {
		mds=new datasaver();
		sthread=new datasaver2();
		super.onCreate();
	}
	String SerialNo;
	Thread readthread ;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		saveDatatext("service started");
		showUserSettings();
		
		Toast.makeText(getApplicationContext(), "service started:"+syncFrq, Toast.LENGTH_LONG).show();
	//	SerialNo=android.os.Build.SERIAL;
		/* try {
	        	handler.post(timedTask);
			} catch (Exception e) {
			
				Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
			}*/
		 
		 //////
		 final int myID = 1234;

		Intent intent1 = new Intent(this, MainActivity.class);
		intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent1, 0);

		
		Notification notification=new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentText("Heart rate="+ heartRate+"\nMotion Status="+motionType)
        .setContentIntent(pendIntent).build();

		startForeground(myID, notification);// bring the service to foreground
		new SensorSubscriptionTask().execute();
	
		mds.start();
		sthread.start();
		return START_STICKY;
	}
	
	final static String MY_ACTION = "MY_ACTION";
	
	@Override
	public void onLowMemory() {
		
		saveDatatext("low Memory");
		super.onLowMemory();
	}

	@Override
	public void onDestroy() {
		Toast.makeText(getApplicationContext(), "service stoped", Toast.LENGTH_LONG).show();
		
		saveDatatext("service distroyed");
		super.onDestroy();
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}
	
	//****************************************************Main Tread****************************************************
	Handler mHandler=new Handler();
    //...

    public void runa() throws Exception{
        mHandler.post(new Runnable() {
			
			@Override
			public void run() {
	                Toast.makeText(service1.this, heartStatus, Toast.LENGTH_LONG).show();
	         
			}
		});

    }    
    int count=0;
   
  //************************************************************************************************************************************  	  
     	  
     	 public String time()
         {
         	long xx = (long)(System.currentTimeMillis() / 1000);
     		
         return Long.toString(xx);
         }
     	  
     	private int syncFrq;
     	String URL,port,topic;
    	private void showUserSettings() {
    		SharedPreferences sharedPrefs = PreferenceManager
    				.getDefaultSharedPreferences(this);
    		syncFrq=Integer.parseInt(sharedPrefs.getString("prefSyncFrequency", "2"));
    		port=sharedPrefs.getString("mqttPORT", "1883");
    		URL=sharedPrefs.getString("mqttURL", "broker.hivemq.com");
    		topic="band/"+android.os.Build.SERIAL+"/";
    	}

    	
    	
    	 class datasaver extends Thread{
			@Override
			public void run() {
				java.util.Date date1= new java.util.Date();
				 Dtime="\n"+(new Timestamp(date1.getTime())).toString();
				while (true) {
					
				try {
					if(getConnectedBandClient()){
						conStat="CONNECTED";
						saveDatatext("thread start");
						heartStatus="ACQUIRING";
						new HeartRateSubscriptionTask().execute();
						
						while(heartStatus.equals("ACQUIRING") && getConnectedBandClient() ){}
						//saveDatatext("data aqq");
						saveData();
						if (bufferData) {
							saveDatatext("buffering started");
							BufferSData("heart.csv", time(), ""+heartRate);
						}
						
						
						try {
							sendMQTT(Dtime+","+time()+",1,"+heartRate);
						} catch (Exception e) {
							saveDatatext("error sending mqtt");
						}
						try {
							client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
							saveDatatext2("hrm stopped");
						} catch (Exception e) {
							saveDatatext("coudn't turn off the hrm");
						}
						
         				 
						saveDatatext("thread stop");
						
						datasaver.sleep(60000*syncFrq);
					}
					else{
						saveDatatext("Band not connected");
						bufferData=true;
					}
					} catch (InterruptedException e) {
						saveDatatext("Interrupted thread");
					}
					catch (BandException e) {
						saveDatatext("Error Connecting MS Services"+e.getErrorType().toString());
						
					}	
					catch (Exception e) {
						// TODO: handle exception
						saveDatatext("Error on main thread:"+e.toString());
					}
					}
					
					
				
			}
    	 }
    	 private void resetData(){
        	 heartStatus="";motionType="";
        	heartRate=0;
        	accX=0;
        	accY=0;
        	accZ=0;
        	skinTempreture=0;
        	pace=0;
        	speed=0; 
        	unixTime=0;
        	calorieCount=0;
        	totalDistance=0;
        	stepCount=0;
        }
    	 
    	 class datasaver2 extends Thread{
 			@Override
 			public void run() {
 				
 				while (true) {
 					
 					new SensorSubscriptionTask().execute();
 					try {
 						if(getConnectedBandClient()){
 						
	 						if (bufferData) {
								StartBuffering();
								saveDatatext2("data Buffering");
							}
						sendMQTT2();
						if(ready){
							sendBufferedSensorData();
							ready=false;
						}
 						saveData2();
 						}
 						else {
 							resetData();
							sendMQTT2();
 						saveData2();
						}
 						Intent intent = new Intent();
					       intent.setAction(MY_ACTION);
					      
					       intent.putExtra("accX", accX);
					       intent.putExtra("accy", accY);
					       intent.putExtra("accz", accZ);
					       intent.putExtra("heart", heartRate);
					       intent.putExtra("cal", calorieCount);
					       intent.putExtra("pace", pace);
					       intent.putExtra("speed", speed);
					       intent.putExtra("totalDistance",totalDistance);
					       intent.putExtra("stepCount", stepCount);
					       intent.putExtra("rotX", angX);
					       intent.putExtra("rotY", angY);
					       intent.putExtra("rotZ", angZ);
					       intent.putExtra("conStat", conStat);
					       intent.putExtra("WearingState", WearingState);
					       intent.putExtra("motionType", motionType);
					       
					       sendBroadcast(intent);
 						datasaver.sleep(SensorFreq);
 					} catch (InterruptedException e) {
 						saveDatatext("Interrupted thread");
 					}
 					catch (Exception e) {
 						saveDatatext("Error on main thread2: "+e.toString());
 					}		
 					
 				}
 					
 			}	
     	 }
    	 
    	 private void StartBuffering() {
    		 String time = time();
				BufferSData("accx.csv", time, ""+accX);
				BufferSData("accy.csv", time, ""+accY);
				BufferSData("accz.csv", time, ""+accZ);
				BufferSData("angx.csv", time, ""+angX);
				BufferSData("angy.csv", time, ""+angY);
				BufferSData("angz.csv", time, ""+angZ);
				BufferSData("cal.csv", time, ""+calorieCount);
				BufferSData("pace.csv", time, ""+pace);
				BufferSData("speed.csv", time, ""+speed);
				BufferSData("dis.csv", time, ""+totalDistance);
				BufferSData("step.csv", time, ""+stepCount);
				BufferSData("stemp.csv", time, ""+skinTempreture);
				BufferSData("motion.csv", time, ""+motionType);
				BufferSData("wstate.csv", time, ""+WearingState);
				BufferSData("conStat.csv", time, ""+conStat);
				
			}
	//****************************************** Sensor Listners*******************************************************************************************
	
	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
            	heartRate=event.getHeartRate();
            	heartStatus=event.getQuality().toString();
            	
            }
        }
    };
    
    float angX,angY,angZ;
    private BandGyroscopeEventListener mBandGyroscopeEventListener=new BandGyroscopeEventListener() {
		
		@Override
		public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
			
			accX=event.getAccelerationX();
			accY=event.getAccelerationY();
			accZ=event.getAccelerationZ();
			angX=event.getAngularVelocityX();
			angY=event.getAngularVelocityY();
			angZ=event.getAngularVelocityZ();
		}
	};
    
  
    
    
    private BandCaloriesEventListener mCalorieEventListener = new BandCaloriesEventListener() {

		@Override
		public void onBandCaloriesChanged(final BandCaloriesEvent event) {
			if (event!=null) {
				try {
					calorieCount=event.getCalories();
				
				} catch (Exception e) {
					
				}
				
				
			}
			
		}
        
    };
    
    private BandContactEventListener mBandContactEventListener = new BandContactEventListener() {
		
		@Override
		public void onBandContactChanged(BandContactEvent event) {
			  if (event != null) {
				  WearingState=event.getContactState().toString();
			  }
			
		}
	};
    
	
    private BandAltimeterEventListener mAltimeterEventListener = new BandAltimeterEventListener() {
        @Override
        public void onBandAltimeterChanged(final BandAltimeterEvent event) {
            if (event != null) {
           /*appendToUI(new StringBuilder().append(String.format("Total Gain = %d cm\n", event.getTotalGain()))
                		.append(String.format("Total Loss = %d cm\n", event.getTotalLoss()))
                		.append(String.format("Stepping Gain = %d cm\n", event.getSteppingGain()))
                		.append(String.format("Stepping Loss = %d cm\n", event.getSteppingLoss()))
                		.append(String.format("Steps Ascended = %d\n", event.getStepsAscended()))
                		.append(String.format("Steps Descended = %d\n", event.getStepsDescended()))
                		.append(String.format("Rate = %f cm/s\n", event.getRate()))
                		.append(String.format("Flights of Stairs Ascended = %d\n", event.getFlightsAscended()))
                		.append(String.format("Flights of Stairs Descended = %d\n", event.getFlightsDescended())).toString());
           */}
        }
    };
		
    	private BandAmbientLightEventListener mAmbientLightEventListener = new BandAmbientLightEventListener() {
            @Override
            public void onBandAmbientLightChanged(final BandAmbientLightEvent event) {
               if (event != null) {
                	
                }
            }
        };
        
        private BandBarometerEventListener mBarometerEventListener = new BandBarometerEventListener() {
            @Override
            public void onBandBarometerChanged(final BandBarometerEvent event) {
                if (event != null) {
                	/*appendToUIalti(String.format("Air Pressure = %.3f hPa\n"
                			+ "Temperature = %.2f degrees Celsius", event.getAirPressure(), event.getTemperature()));*/
               }
            }
        };
        
        private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        	
            @Override
            public void onBandGsrChanged(final BandGsrEvent event) {
                if (event != null) {
                	//appendToUI(String.format("Resistance = %d kOhms\n", event.getResistance()));
                }
            }
        };
        
       
        private BandDistanceEventListener mDistanceEventListener = new BandDistanceEventListener() {

			@Override
			public void onBandDistanceChanged(final BandDistanceEvent event) {
				if (event != null) {
					try {
						
						motionType = event.getMotionType().toString();						
						pace=event.getPace();
						speed=event.getSpeed();
						totalDistance=event.getTotalDistance();
					
					} catch (Exception e) {
						saveDatatext("4:"+e.toString());
					}
				}
				
			}
        };
        
        private BandPedometerEventListener mPedometerEventListener = new BandPedometerEventListener() {

			@Override
			public void onBandPedometerChanged(final BandPedometerEvent event) {
				if (event != null) {
					try {
						stepCount=event.getTotalSteps();
					
					} catch (Exception e) {
						//Toast.makeText(getApplicationContext(), "Error distance"+e.toString(), Toast.LENGTH_SHORT).show();
					}
                	
                }
				
			}

			
            
        };
        
        private BandSkinTemperatureEventListener mSkinTempretureEventListner = new BandSkinTemperatureEventListener(){

			@Override
			public void onBandSkinTemperatureChanged(final BandSkinTemperatureEvent event) {
				if (event != null) {
					skinTempreture=event.getTemperature();
					
                }
				
			}
        	
        };
        
        //******************* Subscribing to sensors
        private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
    		@Override
    		protected Void doInBackground(Void... params) {
    			try {
    				if (getConnectedBandClient()) {
    						
    					
    					if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
    						try {
    							
    							client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
    							saveDatatext2("hrm started");
    					
    							
    						} catch (Exception e) {
    							//Toast.makeText(getApplicationContext(), "Error 0011"+e.toString(), Toast.LENGTH_SHORT).show();
    						}
    						
    					} else {
    						//alert("You have not given this application consent to access heart rate data yet."
    								//+ " Please press the Heart Rate Consent button.\n");
    					}
    				} else {
    					conStat="NOT_CONNECTED";
    					//alert("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
    					saveDatatext2("Band isn't connected. Please make sure bluetooth is on and the band is in range.");
    				}
    			} catch (BandException e) {
    				String exceptionMessage="";
    				switch (e.getErrorType()) {
    				case UNSUPPORTED_SDK_VERSION_ERROR:
    					conStat="UNSUPPORTED_SDK_VERSION_ERROR";
    					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
    					break;
    				case SERVICE_ERROR:
    					conStat="SERVICE_ERROR";
    					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
    					break;
    				default:
    					conStat="UNKNOWN_ERROR";
    					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
    					break;
    				}
    			//	alert(exceptionMessage);
    				saveDatatext2(exceptionMessage);

    			} catch (Exception e) {
    				saveDatatext2(e.toString());
    				//alert(e.getMessage());
    			}
    			return null;
    		}
    	}
        
        
        private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
    		@Override
    		protected Void doInBackground(Void... params) {
    			try {
    				if (getConnectedBandClient()) {
    						try {
    							
    						    client.getSensorManager().registerCaloriesEventListener(mCalorieEventListener);
    							client.getSensorManager().registerPedometerEventListener(mPedometerEventListener);
    							client.getSensorManager().registerDistanceEventListener(mDistanceEventListener);
    							client.getSensorManager().registerSkinTemperatureEventListener(mSkinTempretureEventListner);
    							client.getSensorManager().registerContactEventListener(mBandContactEventListener);
    							client.getSensorManager().registerGyroscopeEventListener(mBandGyroscopeEventListener, SampleRate.MS128);
    						} catch (Exception e) {
    							Toast.makeText(getApplicationContext(), "Error 0011"+e.toString(), Toast.LENGTH_SHORT).show();
    						}
    						int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
    						 if (hardwareVersion >= 20) {
    			                	try {
    			                		client.getSensorManager().registerAltimeterEventListener(mAltimeterEventListener);
    									client.getSensorManager().registerAmbientLightEventListener(mAmbientLightEventListener);
    									client.getSensorManager().registerBarometerEventListener(mBarometerEventListener);
    									client.getSensorManager().registerGsrEventListener(mGsrEventListener);
    								} catch (Exception e) {
    									saveDatatext("Error 0012");
    								
    								}
    							
    								} else {
    								    //saveDatatext("The Altimeter, GSR and Ambient Light sensor and Barometer is not supported with your Band version. Microsoft Band 2 is required.\n");
    							}
    					
    				
    				} else {
    					conStat="NOT_CONNECTED";
    					saveDatatext("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
    				}
    			} catch (BandException e) {
    				String exceptionMessage="";
    				switch (e.getErrorType()) {
    				case UNSUPPORTED_SDK_VERSION_ERROR:
    					conStat="UNSUPPORTED_SDK_VERSION_ERROR";
    					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
    					break;
    				case SERVICE_ERROR:
    					conStat="SERVICE_ERROR";
    					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
    					break;
    				default:
    					conStat="UNKNOWN_ERROR";
    					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
    					break;
    				}
    			//	alert(exceptionMessage);
    				saveDatatext(exceptionMessage);
    			} catch (Exception e) {
    				//alert(e.getMessage());
    			}
    			return null;
    		}
    	}
    	
        
        //**************************Connecting to band**************************************
        
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

        //**********************************SAVE DATA***************************************************
	public void saveData (){
		java.util.Date date1= new java.util.Date();
		String data="\n"+(new Timestamp(date1.getTime())).toString()+","+WearingState+","+heartRate+","+heartStatus+","+calorieCount+","+totalDistance+","+motionType+","+speed+","+pace+","+stepCount+","+skinTempreture+","+accX+","+accY+","+accZ;
		String fileName = "dg2.csv";
				String dirName = "MyDirectory";
				File myDir = new File("sdcard", dirName);

				/*if directory doesn't exist, create it*/
				if(!myDir.exists())
				    myDir.mkdirs(); 


				File myFile = new File(myDir, fileName);

				/*Write to file*/
				try {
				    FileWriter fileWriter = new FileWriter(myFile,true);
				  BufferedWriter fbw = new BufferedWriter(fileWriter);
				    fbw.write(data);
				    //fbw.flush();
				    fbw.close();
				}
				catch(IOException e){
					//Toast.makeText(getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
				    //e.printStackTrace();
					saveDatatext("error writing to file");
				}
	}

	

	public void BufferSData (String fileName,String time,String Data1){
		
		
		String data="\n"+time+","+Data1;
				
				String dirName = "MyDirectory";
				File myDir = new File("sdcard", dirName);

				/*if directory doesn't exist, create it*/
				if(!myDir.exists())
				    myDir.mkdirs(); 
				File myFile = new File(myDir, fileName);
				try {
				    FileWriter fileWriter = new FileWriter(myFile,true);
				  BufferedWriter fbw = new BufferedWriter(fileWriter);
				    fbw.write(data);
				    fbw.close();
				}
				catch(IOException e){
					saveDatatext("error writing to file");
				}
	}

	
	public void saveData2 (){
		java.util.Date date1= new java.util.Date();
		String data="\n"+(new Timestamp(date1.getTime())).toString()+","+conStat+","+","+calorieCount+","+totalDistance+","+motionType+","+speed+","+pace+","+stepCount+","+skinTempreture+","+accX+","+accY+","+accZ+","+angX+","+angY+","+angZ;
		String fileName = "dg3.csv";
				String dirName = "MyDirectory";
				
				File myDir = new File("sdcard", dirName);

				/*if directory doesn't exist, create it*/
				if(!myDir.exists())
				    myDir.mkdirs(); 


				File myFile = new File(myDir, fileName);

				/*Write to file*/
				try {
				    FileWriter fileWriter = new FileWriter(myFile,true);
				  BufferedWriter fbw = new BufferedWriter(fileWriter);
				    fbw.write(data);
				    //fbw.flush();
				    fbw.close();
				}
				catch(IOException e){
					//Toast.makeText(getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
				    //e.printStackTrace();
					saveDatatext("error writing to file");
				}
	}
	
	public void saveDatatext (String string){
		java.util.Date date1= new java.util.Date();
		String data="\n"+(new Timestamp(date1.getTime())).toString()+","+string;
				String fileName = "log.csv";
				String dirName = "MyDirectory";
				File myDir = new File("sdcard", dirName);

				/*if directory doesn't exist, create it*/
				if(!myDir.exists())
				    myDir.mkdirs(); 


				File myFile = new File(myDir, fileName);

				/*Write to file*/
				try {
				    FileWriter fileWriter = new FileWriter(myFile,true);
				  BufferedWriter fbw = new BufferedWriter(fileWriter);
				    fbw.write(data);
				    //fbw.flush();
				    fbw.close();
				}
				catch(IOException e){
					//Toast.makeText(getApplicationContext(), e.toString(),Toast.LENGTH_SHORT).show();
				    //e.printStackTrace();
				}
	}
	public void saveDatatext2 (String string ){
		java.util.Date date1= new java.util.Date();
		String data="\n"+(new Timestamp(date1.getTime())).toString()+","+string;
				String fileName = "hrm.csv";
				String dirName = "MyDirectory";
				File myDir = new File("sdcard", dirName);

				/*if directory doesn't exist, create it*/
				if(!myDir.exists())
				    myDir.mkdirs(); 


				File myFile = new File(myDir, fileName);

				/*Write to file*/
				try {
				    FileWriter fileWriter = new FileWriter(myFile,true);
				  BufferedWriter fbw = new BufferedWriter(fileWriter);
				    fbw.write(data);
				    //fbw.flush();
				    fbw.close();
				}
				catch(IOException e){
				
				}
	}
	
	
	//*************************************************** Sending MQTT**************************************************************************
	MqttAndroidClient client1;
	MemoryPersistence memPer;
	
	private void sendMQTT(final String string){
	    memPer = new MemoryPersistence();
		 client1 = new MqttAndroidClient(getApplicationContext(), "tcp://"+URL+":"+port,"SMU001",memPer);
		 
		MqttConnectOptions options = new MqttConnectOptions();
		options.setKeepAliveInterval(9);
		options.setWill("WillTopic", "ERROR HAPPENED".getBytes(), 2,false);
		try {
			client1.connect(options,null, new IMqttActionListener() {
				
				@Override
				public void onSuccess(IMqttToken arg0) {
	          try {  
	              saveDatatext("client connected to"+"tcp://"+URL+":"+port);
	              
	              MqttMessage message = new MqttMessage((time()+","+string).getBytes() );
	              message.setQos(2);
	              message.setRetained(false);

	      		 try {
	      			client1.publish(topic+"heartRate", message);
	      			client1.disconnect();
	      		} catch (MqttPersistenceException e) {
	      			saveDatatext("1:"+e.toString());
	      		} catch (MqttException e) {
	      			saveDatatext("2:"+e.toString());
	      		}
	      		 catch (Exception e) {
	      			 bufferData=true;
	      			 saveDatatext("3:"+e.toString());
	      		}
	          	  }
	          catch (Exception e) {
	        	  	bufferData=true;
	        	  saveDatatext(e.toString());
	          		}
				}
				
				@Override
				public void onFailure(IMqttToken arg0, Throwable arg1) {
					bufferData=true;
					saveDatatext("COnnect failure mqtt");
					
				}
			});
		} catch (Exception e) {
			bufferData=true; 
			saveDatatext(e.toString());
			//alert(e.toString());
		}
		
		
		 client1.setCallback(new MqttCallback() {
				
				@Override
				public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
					
				}
				
				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0) {
					saveDatatext("Dilivered");
					if (bufferData) {
					//	sendBufferedData();
						saveDatatext("buffering stoped");
					}
					bufferData=false;
					
					java.util.Date date1= new java.util.Date();
					 Dtime="\n"+(new Timestamp(date1.getTime())).toString();
					
					
				}
				
				@Override
				public void connectionLost(Throwable arg0) {
					saveDatatext("mqtt connection lost");
				}
			});
         
	}
	
	
	
	String lastmqtt ;
	int i=0;
	private void sendMQTT2(){
		i=i+1;
		saveDatatext2("sending mqtt");
	 final MqttAndroidClient clientx;
	 
	    memPer = new MemoryPersistence();
		 clientx = new MqttAndroidClient(getApplicationContext(), "tcp://"+URL+":"+port,"ELU5",memPer);
		
		 MqttConnectOptions options = new MqttConnectOptions();
			options.setKeepAliveInterval(480);
			options.setWill("WillTopic", "ERROR HAPPENED".getBytes(), 1,true);
			
			if(clientx.isConnected()){
				try {
					clientx.disconnect();
				} catch (MqttException e) {
					
				}
			}
		try {
			
			
			clientx.connect(options,null, new IMqttActionListener() {
				
				@Override
				public void onSuccess(IMqttToken arg0) {
	          try {  
	              saveDatatext2("client connected to: "+topic);
	      		 try {
	      			 String time = time();
	      		  MqttMessage message = new MqttMessage((Dtime+":"+time+","+calorieCount).getBytes() );
	              message.setQos(0);
	              message.setRetained(false);
	      			clientx.publish(topic+"cal", message);
	      		  message = new MqttMessage((Dtime+":"+time+","+accX).getBytes() );
	              message.setQos(0);
	              message.setRetained(false);
	      			clientx.publish(topic+"AccelerationX", message);
	      			
	      		  message = new MqttMessage((time+","+conStat).getBytes() );
	              message.setQos(0);
	              message.setRetained(false);
	      			clientx.publish(topic+"Connection", message);
	      			
	      		  message = new MqttMessage((Dtime+":"+time+","+accY).getBytes() );
	              message.setQos(0);
	              message.setRetained(false);
	      			clientx.publish(topic+"AccelerationY", message);
	      			message = new MqttMessage((Dtime+":"+time+","+accZ).getBytes() );
		              message.setQos(0);
		              message.setRetained(false);
		      			clientx.publish(topic+"AccelerationZ", message);
		      			 message = new MqttMessage((Dtime+":"+time+","+angX).getBytes() );
			              message.setQos(0);
			              message.setRetained(false);
			      			clientx.publish(topic+"angVeloX", message);
			      		  message = new MqttMessage((Dtime+":"+time+","+angY).getBytes() );
			              message.setQos(0);
			              message.setRetained(false);
			      			clientx.publish(topic+"angVeloY", message);
			      			message = new MqttMessage((Dtime+":"+time+","+angZ).getBytes() );
				              message.setQos(0);
				              message.setRetained(false);
				      			clientx.publish(topic+"angVeloZ", message);
		      		  message = new MqttMessage((Dtime+":"+time+","+motionType).getBytes() );
		              message.setQos(0);
		              message.setRetained(false);
		      			clientx.publish(topic+"motionType", message);
		      			message = new MqttMessage((Dtime+":"+time+","+speed).getBytes() );
			              message.setQos(2);
			              message.setRetained(false);
			      			clientx.publish(topic+"speed", message);
			      		  message = new MqttMessage((Dtime+":"+time+","+pace).getBytes() );
			              message.setQos(2);
			              message.setRetained(false);
			      			clientx.publish(topic+"pace", message);
			      			message = new MqttMessage((Dtime+":"+time+","+stepCount).getBytes() );
				              message.setQos(2);
				              message.setRetained(false);
				      			clientx.publish(topic+"stepCount", message);
				      		  message = new MqttMessage((Dtime+":"+time+","+totalDistance).getBytes() );
				              message.setQos(2);
				              message.setRetained(false);
				      			clientx.publish(topic+"totalDistance", message);
				      			message = new MqttMessage((Dtime+":"+time+","+skinTempreture).getBytes() );
					              message.setQos(2);
					              message.setRetained(false);
					      			clientx.publish(topic+"skinTempreture", message);
					      		 lastmqtt=message.toString();
					      			clientx.disconnect();
					      		
	      			
	      		} catch (MqttPersistenceException e) {
	      			saveDatatext2("1:"+e.toString());
	      		} catch (MqttException e) {
	      			saveDatatext2("2:"+e.toString());
	      		}
	      		 catch (Exception e) {
	      			 bufferData=true;
	      			 saveDatatext2("3:"+e.toString());
	      		}
	          	  }
	          catch (Exception e) {
	        	  	bufferData=true;
	        	  saveDatatext2(e.toString());
	          		}
				}
				
				@Override
				public void onFailure(IMqttToken arg0, Throwable arg1) {
					bufferData=true;
					saveDatatext2("COnnect failure mqtt");
					
				}
			});
		} catch (Exception e) {
			bufferData=true;
			saveDatatext(e.toString());
			//alert(e.toString());
		}
		
		
		 clientx.setCallback(new MqttCallback() {
			 int j=0;
				@Override
				public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
					
				}
				
				@Override
				public void deliveryComplete(IMqttDeliveryToken a) {
					j=j+1;
					
					saveDatatext2(i+"Dilivered:"+j);
					

					BufferSData("check2.txt", time(), lastmqtt);
					try {
						BufferSData("check2.txt", time(), a.getMessage().toString());
					} catch (Exception e1) {
						BufferSData("check2.txt", time(), e1.toString());
					}
					if (bufferData) {
						//sendBufferedData();
						
						saveDatatext2("buffering stoped");
					
							try {
								if(lastmqtt.equals(a.getMessage().toString())){
									BufferSData("check.txt", time(), lastmqtt);
									sendBufferedSensorData();
									bufferData=false;
								}
								
							} catch (MqttException e) {
								
							}
					}
					
					java.util.Date date1= new java.util.Date();
					 Dtime="\n"+(new Timestamp(date1.getTime())).toString();
					
					
				}
				
				@Override
				public void connectionLost(Throwable arg0) {
				//	bufferData=true;
					saveDatatext2("mqtt connection lost");
				}
			});
         
	}
	
	
private void sendBufferedSensorData(){
		
		saveDatatext("buuffered sensor data send");
	
		try {
			
		 memPer = new MemoryPersistence();
		 final MqttAndroidClient client3 = new MqttAndroidClient(getApplicationContext(), "tcp://"+URL+":"+port,"SMU012",memPer);
		 
		MqttConnectOptions options = new MqttConnectOptions();
		options.setKeepAliveInterval(500);
		options.setWill("WillTopic", "ERROR HAPPENED".getBytes(), 2,false);
		try {
			client3.connect(options,null, new IMqttActionListener() {
				
				@Override
				public void onSuccess(IMqttToken arg0) {
				     FileInputStream iss;
					 BufferedReader readers;
					final String [] fileNames = {"conStat.csv","accx.csv","accy.csv","accz.csv","angx.csv","angy.csv","angz.csv","cal.csv","pace.csv","speed.csv",
							"dis.csv","step.csv","stemp.csv","motion.csv","wstate.csv","heart.csv"};
					final String[] topics={"Connection","AccelerationX","AccelerationY","AccelerationZ","angVeloX","angVeloY","angVeloZ","cal",
									  "pace","speed","totalDistance","stepCount","skinTempreture","motionType","wState","heartRate"};
					final String dirNames = "MyDirectory";
					final File myDirs = new File("sdcard", dirNames);
	          try {  
	              saveDatatext("client connected to"+"tcp://"+URL+":"+port);
	              MqttMessage message;
	              for (int c=0;c<fileNames.length;c++) {
	            	  BufferSData("c1.txt", time(), "+c");
	              File myFiles = new File(myDirs, fileNames[c]);
	              if (myFiles.exists()) {
	            	iss= new FileInputStream(myFiles);
	      			readers = new BufferedReader(new InputStreamReader(iss));
	            	String line = readers.readLine();
	            	line = readers.readLine();
	            	  while(line != null) {
	      					message = new MqttMessage(line.getBytes() );
	      	              message.setQos(2);
	      	              message.setRetained(false);
	      	            try {
	    	      			client3.publish("buffer", message);
	    	      			
	    	      			
	    	      		} catch (MqttPersistenceException e) {
	    	      			saveDatatext("1:"+e.toString());
	    	      		} catch (MqttException e) {
	    	      			saveDatatext("2:"+e.toString());
	    	      		}
	    	      		 catch (Exception e) {
	    	      			 bufferData=true;
	    	      			 saveDatatext("3:"+e.toString());
	    	      		}
	      					saveDatatext(line);
	      					line = readers.readLine();
	      					}
	      				boolean deleted2 = myFiles.delete();
	              }
	              
	      		
	          	  }
	              client3.disconnect();
	          }
	          catch (Exception e) {
	        	  	bufferData=true;
	        	  saveDatatext(e.toString());
	          		}
				}
				
				@Override
				public void onFailure(IMqttToken arg0, Throwable arg1) {
					bufferData=true;
					saveDatatext("COnnect failure mqtt");
					
				}
			});
		} catch (Exception e) {
			bufferData=true;
			saveDatatext(e.toString());
			//alert(e.toString());
		}
		
		
		 client1.setCallback(new MqttCallback() {
				
				@Override
				public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
					
				}
				
				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0) {
					saveDatatext(" Buffer Dilivered");
					if (bufferData) {
					//	sendBufferedData();
						saveDatatext("buffering stoped");
					}
					bufferData=false;
					
					
				}
				
				@Override
				public void connectionLost(Throwable arg0) {
					saveDatatext("mqtt connection lost");
				}
			});
         
		
		
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}


}
