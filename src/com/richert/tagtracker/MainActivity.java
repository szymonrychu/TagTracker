package com.richert.tagtracker;

import com.richert.tagtracker.calibrator.CalibrateActivity;
import com.richert.tagtracker.driver.DriverActivity;
import com.richert.tagtracker.elements.AboutActivity;
import com.richert.tagtracker.elements.LicencesActivity;
import com.richert.tagtracker.markergen.MarkerGeneratorActivity;
import com.richert.tagtracker.recognizer.RecognizeActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	private Button calibrateButton = null;
	private Button recognizeButton = null;
	private Button generateButton = null;
	private Button driverButton = null;
	private Button exampleButton = null;
	private Button openGlButton = null;
	private Context context;
	public MainActivity() {
		this.context = this;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//TODO
		setContentView(R.layout.activity_main);
		calibrateButton = (Button) findViewById(R.id.butt_main_calibrate);
		calibrateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent calibrate = new Intent(context,CalibrateActivity.class);
				startActivity(calibrate);
			}
		});
		recognizeButton = (Button) findViewById(R.id.butt_main_recognize);
		recognizeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent recognize = new Intent(context,RecognizeActivity.class);
				startActivity(recognize);
			}
		});
		generateButton = (Button) findViewById(R.id.butt_main_marker_gen);
		generateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent generate = new Intent(context,MarkerGeneratorActivity.class);
				startActivity(generate);
			}
		});
		driverButton = (Button) findViewById(R.id.butt_main_driver);
		driverButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent generate = new Intent(context,DriverActivity.class);
				startActivity(generate);
			}
		});
		
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
