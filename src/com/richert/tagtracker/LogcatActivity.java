package com.richert.tagtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.richert.tagtracker.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class LogcatActivity extends Activity {
	private TextView textView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_logcat);
		textView = (TextView)findViewById(R.id.logcat_text_view);
		textView.setMovementMethod(new ScrollingMovementMethod());
		textView.setText("");
		
		super.onCreate(savedInstanceState);
	}
	@Override
	protected void onResume() {
		try {
			Process process = Runtime.getRuntime().exec("logcat -d");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder log=new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				log.append(line);
			}
			textView.append(log.toString());
		} catch (IOException e) {
			
	    }
		super.onResume();
	}
	@Override
	protected void onPause() {
		try {
			Runtime.getRuntime().exec("logcat -c");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.onPause();
	}
}
