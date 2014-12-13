package com.richert.tagtracker.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.richert.tagtracker.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Scroller;
import android.widget.TextView;

public class LogcatActivity extends Activity implements OnClickListener{
	public static final String TAG = LogcatActivity.class.getSimpleName();
	private TextView textView;
	private Button clearButton;
	public static int crashLine = -1;
	public static CharSequence parseLogcat(int maxLines){
		try {
			crashLine = -1;
			Process process = Runtime.getRuntime().exec("logcat -d");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			SpannableStringBuilder log = new SpannableStringBuilder();
			int counter = 0;
			String line;
			ArrayList<Integer> lines = new ArrayList<Integer>();
			int linesCounter = 0;
			while ((line = bufferedReader.readLine()) != null) {
				counter = log.length();
				if(line.length()>2){
					if(line.contains("beginning of crash")){
						crashLine = linesCounter;
						Log.d(TAG, "crash line="+crashLine);
					}
					linesCounter++;
					String subLine = line.substring(2);
					int subLineLen = subLine.length();
					if(line.startsWith("V/")){
						log.append(subLine);
						log.setSpan(new ForegroundColorSpan(Color.WHITE), counter, counter + subLineLen, 0);
					}else if(line.startsWith("E/")){
						log.append(subLine);
						log.setSpan(new ForegroundColorSpan(Color.RED), counter, counter + subLineLen, 0);
					}else if(line.startsWith("D/")){
						log.append(subLine);
						log.setSpan(new ForegroundColorSpan(Color.BLUE), counter, counter + subLineLen, 0);
					}else if(line.startsWith("I/")){
						log.append(subLine);
						log.setSpan(new ForegroundColorSpan(Color.GREEN), counter, counter + subLineLen, 0);
					}else if(line.startsWith("W/")){
						log.append(subLine);
						log.setSpan(new ForegroundColorSpan(Color.YELLOW), counter, counter + subLineLen, 0);
					}else if(line.startsWith("A/")){
						log.append(subLine);
						log.setSpan(new ForegroundColorSpan(Color.LTGRAY), counter, counter + subLineLen, 0);
					}else{
						log.append(subLine);
						log.setSpan(new ForegroundColorSpan(Color.DKGRAY), counter, counter + subLineLen, 0);
					}
					lines.add(subLineLen);
					log.append("\n");
				}
			}
			int lSize = lines.size();
			int len = log.length();
			int max = Math.min(lSize, maxLines);
			int start = 0;
			for(int c=1;c<=max;c++){
				start+=lines.get(lSize - c)+1;
			}
			return log.subSequence(len - start, len);
		} catch (IOException e) {
			return new SpannableString("Couldn't read logcat");
	    }
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_logcat);
		clearButton = (Button)findViewById(R.id.logcat_clear_button);
		clearButton.setOnClickListener(this);
		textView = (TextView)findViewById(R.id.logcat_text_view);
		textView.setMovementMethod(new ScrollingMovementMethod());
		textView.setText("");
		textView.setTextSize(8);
		
		super.onCreate(savedInstanceState);
	}
	@Override
	protected void onResume() {
		textView.setText(parseLogcat(2000));
		
		super.onResume();
	}
	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	public void onClick(View v) {
		try {
			Runtime.getRuntime().exec("logcat -c");
			textView.setText("");
		} catch (IOException e) {
			textView.setText("Couldn't clear logcat");
	    }	
	}
}
