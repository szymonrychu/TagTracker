package com.rychu.tagtracker.activities;


import com.rychu.tagtracker.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	private Button recognizerButton;
	private Button controllerButton;
	private Intent recognizerIntent;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_main);
		recognizerButton = (Button) findViewById(R.id.main_recognizer_button);
		controllerButton = (Button) findViewById(R.id.main_controller_button);

		recognizerIntent = new Intent(getBaseContext(), DriverActivity.class);
		recognizerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				recognizerIntent.putExtra(DriverActivity.DESIRED_FRAGMENT_KEY, DriverActivity.FRAGMENT_RECOGNIZER);
				startActivity(recognizerIntent);
			}
		});
		controllerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				recognizerIntent.putExtra(DriverActivity.DESIRED_FRAGMENT_KEY, DriverActivity.FRAGMENT_CONTROLLER);
				startActivity(recognizerIntent);
			}
		});
		super.onCreate(savedInstanceState);
	}
}
