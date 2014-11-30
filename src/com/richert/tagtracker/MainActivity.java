package com.richert.tagtracker;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.richert.tagtracker.driver.DriverActivity;
import com.richert.tagtracker.elements.LanguageHelper;
import com.richert.tagtracker.elements.OfflineDataHelper;
import com.richert.tagtracker.facefollower.FaceFollowerActivity;
import com.richert.tagtracker.markergen.MarkerGeneratorActivity;
import com.richert.tagtracker.recognizer.RecognizeActivity;

public class MainActivity extends Activity implements Runnable{
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final int ID = MainActivity.class.hashCode();
	public static final String INTENT_EXTRA = "intent type";
	private Button recognizeButton = null;
	private Button generateButton = null;
	private Button driverButton = null;
	private Button facefollowerButton = null;
	private Context context;
	private String action, preferedActivity;
	private OfflineDataHelper dbHelper;
	private String driverSimpleName, recognizeSimpleName;
	private Boolean latch;
	private TextToSpeech tts;
	public MainActivity() {
		this.context = this;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbHelper = new OfflineDataHelper(this);
		setContentView(R.layout.activity_main);
		driverSimpleName = DriverActivity.class.getSimpleName();
		recognizeSimpleName = RecognizeActivity.class.getSimpleName();
		action = getIntent().getAction();
		recognizeButton = (Button) findViewById(R.id.butt_main_recognizer);
		driverButton = (Button) findViewById(R.id.butt_main_driver);
		generateButton = (Button) findViewById(R.id.butt_main_marker_gen);
		facefollowerButton = (Button) findViewById(R.id.butt_main_facefollower);
		latch = false;
		tts = new TextToSpeech(this, new OnInitListener(){

			@Override
			public void onInit(int status) {
				int res = tts.isLanguageAvailable(Locale.getDefault());
				if(res == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE){
					LanguageHelper.setDefaultLocale(Locale.US);
				}
			}
			
		});
		tts.shutdown();
		
	}
	
	@Override
	protected void onResume() {
		recognizeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dbHelper.savePreferencedActivity(RecognizeActivity.class.getSimpleName());
				Intent recognize = new Intent(context,RecognizeActivity.class);
				recognize.putExtra(INTENT_EXTRA, action);
				startActivity(recognize);
			}
		});
		driverButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dbHelper.savePreferencedActivity(DriverActivity.class.getSimpleName());
				Intent drive = new Intent(context,DriverActivity.class);
				drive.putExtra(INTENT_EXTRA, action);
				startActivity(drive);
			}
		});
		generateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent generate = new Intent(context,MarkerGeneratorActivity.class);
				generate.putExtra(INTENT_EXTRA, action);
				startActivity(generate);
			}
		});
		facefollowerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent facefollower = new Intent(context,FaceFollowerActivity.class);
				facefollower.putExtra(INTENT_EXTRA, action);
				startActivity(facefollower);
			}
		});
		super.onResume();
	}
	@Override
	protected void onPostResume() {
		if(!latch){
			action = getIntent().getAction();
			if(action.contentEquals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
				Handler h = new Handler();
				h.post(this);
			}
		}
		super.onPostResume();
	}
	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id){
		case R.id.main_action_about:
			Intent about = new Intent(context,AboutActivity.class);
			startActivity(about);
			return true;
		case R.id.main_action_licences:
			Intent licences = new Intent(context,LicencesActivity.class);
			startActivity(licences);
			return true;
		case R.id.main_action_logcat:
			Intent logcat = new Intent(context,LogcatActivity.class);
			startActivity(logcat);
			return true;
		case R.id.main_action_othercode:
			Intent othercode = new Intent(context,OtherCodesActivity.class);
			startActivity(othercode);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	@Override
	public void run() {
		latch = true;
		preferedActivity = dbHelper.loadPreferedActivity();
		//Intent.ACTION_MAIN
		//UsbManager.ACTION_USB_DEVICE_ATTACHED
		try{
			if(preferedActivity.contentEquals(recognizeSimpleName)){
				Intent recognize = new Intent(context,RecognizeActivity.class);
				recognize.putExtra(INTENT_EXTRA, action);
				startActivityForResult(recognize, 0);
				onPause();
			}else if(preferedActivity.contentEquals(driverSimpleName)){
				Intent drive = new Intent(context,DriverActivity.class);
				drive.putExtra(INTENT_EXTRA, action);
				startActivityForResult(drive, 0);
				onPause();
			}
		}catch ( Exception e){}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		latch = true;
		action = Intent.ACTION_MAIN;
		super.onActivityResult(requestCode, resultCode, data);
	}
}
