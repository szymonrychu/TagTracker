package com.richert.tagtracker.recognizer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import com.richert.tagtracker.MainActivity;
import com.richert.tagtracker.R;
import com.richert.tagtracker.R.id;
import com.richert.tagtracker.R.layout;
import com.richert.tagtracker.R.menu;
import com.richert.tagtracker.driver.DriverActivity;
import com.richert.tagtracker.driver.DriverHelper;
import com.richert.tagtracker.elements.CameraDrawerPreview;
import com.richert.tagtracker.elements.CpuInfo;
import com.richert.tagtracker.elements.FullScreenActivity;
import com.richert.tagtracker.elements.OfflineDataHelper;
import com.richert.tagtracker.elements.PerformanceTester;
import com.richert.tagtracker.elements.PerformanceTester.TestResultCallback;
import com.richert.tagtracker.elements.Pointer;
import com.richert.tagtracker.elements.ResolutionDialog;
import com.richert.tagtracker.elements.TagDialog;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraProcessingCallback;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraSetupCallback;
import com.richert.tagtracker.elements.TextToSpeechToText;
import com.richert.tagtracker.elements.TextToSpeechToText.SpeechToTextListener;
import com.richert.tagtracker.elements.ThreadDialog;
import com.richert.tagtracker.geomerty.Tag;
import com.richert.tagtracker.markergen.MarkerGeneratorActivity;
import com.richert.tagtracker.natUtils.Misc;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.test.PerformanceTestCase;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.RotateAnimation;
import android.widget.Button;

public class RecognizeActivity extends FullScreenActivity implements CameraSetupCallback, CameraProcessingCallback, SpeechToTextListener{
	private final static String TAG=RecognizeActivity.class.getSimpleName();
	private static final int ID = RecognizeActivity.class.hashCode();
	private CameraDrawerPreview preview = null;
	private Camera.Parameters params = null;
	private Recognizer recognizer;
	private OfflineDataHelper helper;
	private int rotation=0;
	private int camWidth, camHeight;
	private int viewWidth, viewHeight;
	private Tag[] tags;
	private Paint greenPaint, redPaint, bluePaint;
	private DriverHelper driverHelper;
	public int trackedID = -1;
	private TextToSpeechToText ttstt;
	private Boolean asked = false;
	private Looper mainLooper;
	private boolean[] tagMap = new boolean[32];
	private int confidenceCounter = 0;
	private final static String WHAT_TAG_FOLLOW = " What tag number should I follow?";
	private final static String DONT_SEE_TAGZ = "I don't see any tags, stopping!";
	private final static String FOLLOWING_ = "Following tag number ";
	private final static String I_DIDNT_CATCH_THAT_ = "I didn't catch that! you said: ";
	private final static String I_AM_NOT_SURE_ = "I'm not sure what you mean! you said: ";
	private final static String PLEASE_REPEAT_ = ", please repeat!";
	private final static String IS_IT_OK_ = ", is it ok?";
	private final static String ERROR_ = ", error occured, please repeat!";
	private final static String FATAL_ERROR = ", error occured, stopping recognizer module!";
	private int previewX=0;
	private int previewY=0;
	private boolean showDebug = false;
	private boolean showCPU = false;
	private CpuInfo cpuInfo;
	private Button screenshotButton;
	private PerformanceTester tester;
	private int errorCounter = 0;
	private boolean speak = true;
	private boolean forceSpeaking = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_recognize);
        preview = (CameraDrawerPreview) findViewById(R.id.recognize_preview);
        preview.setCameraSetupCallback(this);
        preview.setCameraProcessingCallback(this);
		helper = new OfflineDataHelper(this);
		greenPaint = new Paint();
		greenPaint.setAntiAlias(true);
		greenPaint.setColor(Color.GREEN);
		greenPaint.setStrokeWidth(7.0f);
		greenPaint.setTextSize(25.0f);
		redPaint = new Paint();
		redPaint.setAntiAlias(true);
		redPaint.setColor(Color.RED);
		redPaint.setStrokeWidth(7.0f);
		redPaint.setTextSize(25.0f);
		bluePaint = new Paint();
		bluePaint.setAntiAlias(true);
		bluePaint.setColor(Color.BLUE);
		bluePaint.setStrokeWidth(7.0f);
		bluePaint.setTextSize(25.0f);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		ttstt = new TextToSpeechToText(this);
		ttstt.setSpeechToTextListener(this);
		mainLooper = this.getMainLooper();
		for(boolean t : tagMap){
			t = false;
		}
		cpuInfo = new CpuInfo(300);
		driverHelper = new DriverHelper(this, (UsbManager) getSystemService(Context.USB_SERVICE));

		screenshotButton = (Button) findViewById(R.id.screenshot_button);
		screenshotButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tester.startTests();
				Thread saver = new Thread(){
					public void run() {
						Bitmap bmp = preview.getScreenShot();
						SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss",Locale.getDefault());
						String date = s.format(new Date());
						helper.saveScreenshot(bmp, "screenshot-"+date+".png");
					};
				};
				saver.start();
			}
		});
		screenshotButton.setVisibility(View.INVISIBLE);
		super.onCreate(savedInstanceState);
	}
	@Override
	protected void onResume() {
		this.speak = true;
		driverHelper.startMonitor(this);
		cpuInfo.startReading();
		super.onResume();
	}
	@Override
	protected void onPause() {
		if(driverHelper != null){
			driverHelper.unregisterReceiver();
		}
		ttstt.onPause();
		this.speak = false;
		cpuInfo.stopReading();
		if(tester != null){
			tester.stopTests();
		}
		super.onPause();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.recognizer, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch(id){
		case R.id.recognize_action_resolution:
			params = preview.getCameraParameters();
			ResolutionDialog resolutionDialog = new ResolutionDialog(params.getSupportedPreviewSizes()) {
				@Override
				public void onListItemClick(DialogInterface dialog, Camera.Size size) {
					params.setPreviewSize(size.width, size.height);
					preview.reloadCameraSetup(params);
					//TODO workaround - need further investigation
					WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
					Display display = (manager).getDefaultDisplay();
					rotation = display.getRotation();
					recognizer.notifySizeChanged(size, rotation);
					helper.setResolution(size);
				}
			};
			resolutionDialog.show(getFragmentManager(), "resolutions");
			return true;
		case R.id.recognize_action_set_tag:
			TagDialog tagDialog = new TagDialog(1,33) {
				@Override
				public void onListItemClick(DialogInterface dialog, Integer tag) {
					trackedID = tag;
				}
			};
			tagDialog.show(getFragmentManager(), "tagz");
			return true;
		case R.id.recognize_action_set_threads:
			ThreadDialog threadDialog = new ThreadDialog(8) {
				@Override
				public void onListItemClick(DialogInterface dialog, Integer num) {
			        preview.setMaxThreads(num);
				}
			};
			threadDialog.show(getFragmentManager(), "threadz");
			return true;
		case R.id.recognize_action_debug:
			item.setChecked(! item.isChecked());
			showDebug = item.isChecked();
			if(showDebug){
				screenshotButton.setVisibility(View.VISIBLE);
			}else{
				screenshotButton.setVisibility(View.INVISIBLE);
			}
			return true;
		case R.id.recognize_action_graph:
			item.setChecked(! item.isChecked());
			showCPU = item.isChecked();
			return true;
		case R.id.recognize_force_speaking:
			item.setChecked(! item.isChecked());
			forceSpeaking = item.isChecked();
			return true;
		default:
			return false;
		}
	}
	@Override
	protected void onSystemBarsVisible() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onSystemBarsHided() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void processImage(Mat yuvFrame, Thread thiz) {
		tags = recognizer.findTags(yuvFrame, rotation);
		preview.requestRefresh();
	}

	private void drawTag(Tag tag, Canvas canvas, Paint paint){
		for(int c=0;c<4;c++){
			float x1 = tag.points[c].x*viewWidth;
			float y1 = tag.points[c].y*viewHeight;
			float x2 = tag.points[(c+1)%4].x*viewWidth;
			float y2 = tag.points[(c+1)%4].y*viewHeight;
			canvas.drawLine(x1, y1, x2, y2, paint);
		}
		canvas.drawText(""+tag.id, tag.center.x*viewWidth, tag.center.y*viewHeight, paint);
	}
	private void drawTagPreview(Tag tag, Canvas canvas,  Paint paint){
		if(showDebug){
			canvas.drawBitmap(Misc.mat2Bitmap(tag.preview), previewX, previewY , redPaint);
		}
	}
	private void askIfChanged(){
		
		boolean[] newTagMap = new boolean[32];
		for(boolean t : newTagMap){
			t = false;
		}
		if(tags != null){
			for(Tag t : tags){
				newTagMap[t.id -1] = true;
			}
		}
		Boolean diff = false;
		for(int c = 0;c<32 && !diff; c++){
			diff = tagMap[c] != newTagMap[c];
		}
		if(diff){
			confidenceCounter++;
			if(confidenceCounter>30){
				speak = driverHelper.transreceiving();
				confidenceCounter=0;
				trackedID = -1;
				tagMap = newTagMap;
				Boolean notSeeAnyTag = false;
				for(int c = 0;c<32 && !notSeeAnyTag; c++){
					notSeeAnyTag = newTagMap[c];
				}
				Handler h = new Handler(mainLooper);
				if(notSeeAnyTag){
					h.post(new Runnable() {
						@Override
						public void run() {
							if(speak || forceSpeaking){
								ttstt.speak(WHAT_TAG_FOLLOW);
								Log.v(TAG,"speaking"+WHAT_TAG_FOLLOW);
							}else{
								TagDialog tagDialog = new TagDialog(1,33) {
									@Override
									public void onListItemClick(DialogInterface dialog, Integer tag) {
										trackedID = tag;
									}
								};
								tagDialog.show(getFragmentManager(), "tagz");
							}
						}
					});
				}else{
					h.post(new Runnable() {
						@Override
						public void run() {
							if(speak || forceSpeaking){
								ttstt.speak(DONT_SEE_TAGZ);
							}
						}
					});
				}
			}
		}else{
			confidenceCounter = 0;
		}
	}
	private void drawDebugInfo(Canvas canvas){
		int Y = 50;
		if(showDebug){
			canvas.drawText("Preview state: " + preview.getState(), 30, Y+=30, bluePaint);
			canvas.drawText("Driver state: " + driverHelper.getState(), 30, Y+=30, bluePaint);
			canvas.drawText("Device: "+driverHelper.getDeviceInfo(), 30, Y+=30, bluePaint);
			canvas.drawText("Driver buffer: " + driverHelper.getBuffer(), 30, Y+=30, bluePaint);
			for(int coreNum = 0; coreNum < cpuInfo.getNumCores(); coreNum++){
				canvas.drawText(cpuInfo.getCpuUsage(coreNum), 30, Y+=30, bluePaint);
			}
		}
		if(showCPU){
			cpuInfo.drawCpuUsage(canvas, 30, Y+=30, 800, Y+=300);
		}
		tester.getStatistics();
	}
	private float meanY = -1;
	@Override
	public void drawOnCamera(Canvas canvas, double scaleX, double scaleY) {
		
		if(tags!=null){
			previewX = previewY = 0;
			float mY = 0;
			for(Tag tag : tags){
				mY += tag.center.x;
				if(tag.id == trackedID){
					drawTag(tag, canvas, greenPaint);
					drawTagPreview(tag, canvas, greenPaint);
					float z = (float)5*((tag.center.y)-0.5f);
					mY += z;
					driverHelper.steer((float)(3*((tag.center.x)-0.5f)),-1.0f,meanY);
					
				}else{
					drawTag(tag, canvas, redPaint);
					drawTagPreview(tag, canvas, redPaint);
					driverHelper.steer(0,0,meanY);
					float z = (float)5*((tag.center.y)-0.5f);
					mY += z;
				}
				previewX+=tag.preview.cols();
			}
			if(meanY < 0){
				meanY = mY/tags.length;
			}else{
				if(Math.abs(mY/tags.length - meanY)>0.1){
					meanY = mY/tags.length;
				}
			}
			
			
		}else{
			driverHelper.steer(0,0,meanY);
		}
		drawDebugInfo(canvas);
		askIfChanged();
	}
	@SuppressWarnings("deprecation")
	private void setCameraParameters(Parameters params){
		if(params.isAutoExposureLockSupported()){
			params.setAutoExposureLock(false);
		}
		if(params.isAutoWhiteBalanceLockSupported()){
			params.setAutoWhiteBalanceLock(false);
		}
		if(params.isVideoStabilizationSupported()){
			params.setVideoStabilization(true);
		}
		if(params.getAntibanding().contains(Parameters.ANTIBANDING_AUTO)){
			params.setAntibanding(Parameters.ANTIBANDING_AUTO);
		}
		if(params.getMaxNumFocusAreas() > 0){
			List<Area> areas = params.getMeteringAreas();
			if(areas != null){
				for(Area a : areas){
					Log.v(TAG," b:"+a.rect.bottom +" t:"+a.rect.top+" l:"+a.rect.left+" r:"+a.rect.right);
				}
			}
			areas = new ArrayList<Camera.Area>();
			areas.add(new Area(new Rect(-500, -500, 500, 500), 1000));
			params.setMeteringAreas(areas);
		}
	}
	@Override
	public void setCameraParameters(Parameters params, int width, int height, int rotation) {
		setCameraParameters(params);
		viewHeight = height;
		viewWidth = width;
		Log.v(TAG,"params.getPreviewSize()=w:"+params.getPreviewSize().width+":h:"+params.getPreviewSize().height);
		recognizer.notifySizeChanged(params.getPreviewSize(), rotation);
	}
	private ProgressDialog progress;
	private void showProgressDialog(int maxStates){
		final RecognizeActivity thiz = this;
		final int max = maxStates;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				progress = new ProgressDialog(thiz);
				progress.setTitle("Testowanie urz¹dzenia");
				progress.setMessage("postêp");
				progress.setCancelable(true);
				progress.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						tester.stopTests();
					}
				});
				progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progress.setMax(max);
				progress.show();
			}
		});
	}
	private void updateProgressDialogStatus(int state){
		final int st = state;
		if(progress != null){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					progress.setProgress(st);
				}
			});
		}
		
	}
	private void dismissProgressDialog(){
		if(progress != null){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					progress.dismiss();
					progress = null;
				}
			});
			
		}
	}
	@Override
	public void setCameraInitialParameters(Parameters params, int width, int height, int rotation) {
		setCameraParameters(params);
		viewHeight = height;
		viewWidth = width;
		camWidth = helper.getResolutionWidth(params.getPreviewSize().width);
		camHeight = helper.getResolutionHeight(params.getPreviewSize().height);
		params.setPreviewSize(camWidth, camHeight);
		recognizer = new Recognizer(width, height);
		recognizer.notifySizeChanged(params.getPreviewSize(), rotation);
		tester = new PerformanceTester(this, preview, recognizer, cpuInfo, helper);
		tester.addTestResultCallback(new TestResultCallback() {
			
			@Override
			public void onTestStarted(int maxStates) {
				showProgressDialog(maxStates);
			}
			
			@Override
			public void onTestResult() {
				dismissProgressDialog();
			}
			
			@Override
			public void onTestInterrupted() {
				dismissProgressDialog();
			}

			@Override
			public void onTestProgress(int state) {
				updateProgressDialogStatus(state);
			}
		});
		
	}
	@Override
	public void getPointers(SparseArray<Pointer> pointers) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onPartialRecognitionResult(String result, float confidence) {
		Log.v(TAG,"onPartialRecognitionResult:"+result+":"+confidence);
	}
	int notSureTag = -1;
	@Override
	public void onRecognitionResult(String result, float confidence) {
		Log.v(TAG,"onRecognitionResult:"+result+":"+confidence);
		if(speak || forceSpeaking){
			if(notSureTag > 0){
				int yesNo = ttstt.stringToBool(result);
				if(yesNo > 0){
					trackedID = notSureTag;
					ttstt.speak(FOLLOWING_+trackedID);
					notSureTag = -1;
				}else{
					ttstt.speak(I_AM_NOT_SURE_ +result+ WHAT_TAG_FOLLOW);
					notSureTag = -1;
				}
			}else{
				int trackId = ttstt.stringToInt(result);
				if(confidence < 0.4 && trackId > 0){
		        	ttstt.speak(I_AM_NOT_SURE_ +result+ IS_IT_OK_);
		        	notSureTag = trackId;
				}else if(confidence < 0.4 && trackId <= 0){
					ttstt.speak(I_DIDNT_CATCH_THAT_ +result+ PLEASE_REPEAT_);
					notSureTag = -1;
				}else if(confidence >= 0.4 && trackId <= 0){
					ttstt.speak(I_DIDNT_CATCH_THAT_ +result+ PLEASE_REPEAT_);
					notSureTag = -1;
				}else if(confidence >= 0.4 && trackId > 0){
					trackedID = trackId;
					ttstt.speak(FOLLOWING_+trackedID);
					notSureTag = -1;
				}
			}
		}
	}
	@Override
	public void onDoneTalking(String text) {
		if(speak || forceSpeaking){
			if(text.contains(FOLLOWING_)){

			}else if(text.contains(DONT_SEE_TAGZ)){
				
			}else if(text.contains(WHAT_TAG_FOLLOW)){
				ttstt.recognizeText();
			}else if(text.contains(I_AM_NOT_SURE_)){
				ttstt.recognizeText();
			}else if(text.contains(I_DIDNT_CATCH_THAT_)){
				ttstt.recognizeText();
			}else if(text.contains(ERROR_)){
				ttstt.recognizeText();
			}
		}
	}
	@Override
	public void onEvent(int type) {
		if(type < 10){
			if(errorCounter > 3 ){
				speak = false;
				ttstt.speak(TextToSpeechToText.ERRORS[type] + FATAL_ERROR);
			}else{
				ttstt.speak(TextToSpeechToText.ERRORS[type] + ERROR_);
			}
			errorCounter++;
		}
	}
}
