package com.richert.tagtracker.services;

import com.richert.tagtracker.activities.DriverActivity;
import com.richert.tagtracker.activities.RecognizeActivity;
import com.richert.tagtracker.elements.DriverHelper;
import com.richert.tagtracker.elements.OfflineDataHelper;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class UsbConnectionService extends Service implements Runnable {
	private static final String TAG = UsbConnectionService.class.getSimpleName();
	private Context context;
	private Application application;
	private String action, preferedActivity;
	private Boolean latch;
	public static final String INTENT_EXTRA = "intent type";
	UsbDataCommunicationBinder binder;
	

	private DriverHelper driverHelper;
	
	
	public class UsbDataCommunicationBinder extends Binder {
		public void steer(float procX, float procY, float procZ){
			driverHelper.steer(procX,procY,procZ);
		}
	}
	@Override
	public void onStart(Intent intent, int startId) {
		//action = intent.getAction();
		new Handler().post(this);
		Log.v(TAG, "onStrart()");
		super.onStart(intent, startId);
	}
	
	
	
	
	
	
	
	@Override
	public void onCreate() {
		context = getBaseContext();
		application = getApplication();
		binder = new UsbDataCommunicationBinder();
		driverHelper = new DriverHelper(this, (UsbManager) getSystemService(Context.USB_SERVICE));
		driverHelper.startMonitor(this);
		
		// TODO Auto-generated method stub
		super.onCreate();
	}
	@Override
	public void onDestroy() {
		if(driverHelper != null){
			driverHelper.unregisterReceiver();
		}
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return binder;
	}
	@Override
	public void run() {
		latch = true;
		OfflineDataHelper dbHelper = new OfflineDataHelper(context);
		preferedActivity = dbHelper.loadPreferedActivity();
		if(preferedActivity.isEmpty()){
			stopSelf();
		}
		//Intent.ACTION_MAIN
		//UsbManager.ACTION_USB_DEVICE_ATTACHED
		try{
			if(preferedActivity.contentEquals(RecognizeActivity.class.getSimpleName())){
				Intent recognize = new Intent(context,RecognizeActivity.class);
				recognize.putExtra(INTENT_EXTRA, action);
				application.startActivity(recognize);
			}else if(preferedActivity.contentEquals(DriverActivity.class.getSimpleName())){
				Intent drive = new Intent(context,DriverActivity.class);
				drive.putExtra(INTENT_EXTRA, action);
				startActivity(drive);
			}
		}catch ( Exception e){}
	}

}
