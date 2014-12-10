package com.richert.tagtracker.services;

import com.richert.tagtracker.elements.OfflineDataHelper;
import com.richert.tagtracker.views.DriverActivity;
import com.richert.tagtracker.views.RecognizeActivity;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class UsbConnectionService extends Service implements Runnable {
	private Context context;
	private Application application;
	private String action, preferedActivity;
	private Boolean latch;
	public static final String INTENT_EXTRA = "intent type";
	@Override
	public void onStart(Intent intent, int startId) {
		action = intent.getAction();
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}
	@Override
	public void onCreate() {
		context = getBaseContext();
		application = getApplication();
		
		
		// TODO Auto-generated method stub
		super.onCreate();
	}
	@Override
	public IBinder onBind(Intent arg0) {
		
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void run() {
		latch = true;
		OfflineDataHelper dbHelper = new OfflineDataHelper(context);
		preferedActivity = dbHelper.loadPreferedActivity();
		if(preferedActivity.isEmpty()){
			onDestroy();
		}
		//Intent.ACTION_MAIN
		//UsbManager.ACTION_USB_DEVICE_ATTACHED
		try{
			if(preferedActivity.contentEquals(RecognizeActivity.class.getSimpleName())){
				Intent recognize = new Intent(context,RecognizeActivity.class);
				recognize.putExtra(INTENT_EXTRA, action);
				application.startActivity(recognize);
				onDestroy();
			}else if(preferedActivity.contentEquals(DriverActivity.class.getSimpleName())){
				Intent drive = new Intent(context,DriverActivity.class);
				drive.putExtra(INTENT_EXTRA, action);
				startActivity(drive);
				onDestroy();
			}
		}catch ( Exception e){}
	}

}
