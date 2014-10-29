package com.richert.tagtracker;

import java.util.ArrayList;
import java.util.Locale;

import com.richert.tagtracker.calibrator.CalibrateActivity;
import com.richert.tagtracker.driver.DriverActivity;
import com.richert.tagtracker.elements.AboutActivity;
import com.richert.tagtracker.elements.LicencesActivity;
import com.richert.tagtracker.elements.LogcatActivity;
import com.richert.tagtracker.elements.TextToSpeechToText;
import com.richert.tagtracker.markergen.MarkerGeneratorActivity;
import com.richert.tagtracker.recognizer.RecognizeActivity;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity implements Runnable {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final int ID = MainActivity.class.hashCode();
	private Button calibrateButton = null;
	private Button recognizeButton = null;
	private Button generateButton = null;
	private Button driverButton = null;
	private ProgressBar progressBar = null;
	private Context context;
	private int timeMillis=5000;
	private TextToSpeechToText ttstt;
	private Boolean asked = false;
	public MainActivity() {
		this.context = this;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		asked = false;
		//TODO
		setContentView(R.layout.activity_main);
		calibrateButton = (Button) findViewById(R.id.butt_main_calibrate);
		calibrateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				asked = true;
				Intent calibrate = new Intent(context,CalibrateActivity.class);
				startActivity(calibrate);
			}
		});
		recognizeButton = (Button) findViewById(R.id.butt_main_recognize);
		recognizeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				asked = true;
				Intent recognize = new Intent(context,RecognizeActivity.class);
				startActivity(recognize);
			}
		});
		driverButton = (Button) findViewById(R.id.butt_main_driver);
		driverButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				asked = true;
				Intent drive = new Intent(context,DriverActivity.class);
				startActivity(drive);
			}
		});
		generateButton = (Button) findViewById(R.id.butt_main_marker_gen);
		generateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				asked = true;
				Intent generate = new Intent(context,MarkerGeneratorActivity.class);
				startActivity(generate);
			}
		});
		progressBar = (ProgressBar) findViewById(R.id.progressBar_main);
		progressBar.setMax(1000);
		progressBar.setProgress(0);
		
		if(asked != true){
			Handler handler = new Handler();
			handler.postDelayed(this,timeMillis);
			progressBar.setProgress(0);
			ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 1000); 
		    animation.setDuration(timeMillis);
		    animation.setInterpolator(new DecelerateInterpolator());
		    animation.start();
		}else{
	        progressBar.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	protected void onResume() {
		ttstt = new TextToSpeechToText(this);
		super.onResume();
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	@Override
	public void run() {
        progressBar.setVisibility(View.INVISIBLE);
		if(asked != true){
			asked = true;
			ttstt.ask("Make your selection - pick number! First: Calibrate your camera. Second: Recognize the tags. Third: Drive the motors, Fourth: Generate the tags.", ID);
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String response = ttstt.getResponse(requestCode, resultCode, data, ID);
        if(response.contains("one") || response.contains("first") || response.contains("calibrate")){
            ttstt.speak("Starting camera calibration module.");
			Intent calibrate = new Intent(context,CalibrateActivity.class);
			startActivity(calibrate);
        }else if(response.contains("two") || response.contains("second") || response.contains("recognize")){
			Intent recognize = new Intent(context,RecognizeActivity.class);
			startActivity(recognize);
        }else if(response.contains("three") || response.contains("third") || response.contains("drive")){
            ttstt.speak("Starting driver module");
			Intent drive = new Intent(context,DriverActivity.class);
			startActivity(drive);
        }else if(response.contains("four") || response.contains("fourth") || response.contains("generate")){
            ttstt.speak("Starting generator module");
			Intent generate = new Intent(context,MarkerGeneratorActivity.class);
			startActivity(generate);
        }else{
    		ttstt.ask("Make your selection - pick number! First: Calibrate your camera. Second: Recognize the tags. Third: Drive the motors, Fourth: Generate the tags.", ID);
        }
    }
}
