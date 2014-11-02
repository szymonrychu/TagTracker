package com.richert.tagtracker.recognizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import com.richert.tagtracker.elements.FullScreenActivity;
import com.richert.tagtracker.elements.OfflineDataHelper;
import com.richert.tagtracker.elements.Pointer;
import com.richert.tagtracker.elements.ResolutionDialog;
import com.richert.tagtracker.elements.TagDialog;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraProcessingCallback;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraSetupCallback;
import com.richert.tagtracker.elements.TextToSpeechToText;
import com.richert.tagtracker.elements.TextToSpeechToText.SpeechToTextListener;
import com.richert.tagtracker.geomerty.Tag;
import com.richert.tagtracker.markergen.MarkerGeneratorActivity;
import com.richert.tagtracker.natUtils.Misc;

import android.content.Context;
import android.content.DialogInterface;
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
	private Paint greenPaint;
	private Paint redPaint;
	private DriverHelper driverHelper;
	public int trackedID = -1;
	private TextToSpeechToText ttstt;
	private Boolean asked = false;
	private Looper mainLooper;
	private boolean[] tagMap = new boolean[32];
	private int confidenceCounter = 0;
	private final static String WHAT_TAG_FOLLOW = "what tag number should I follow?";
	private final static String DONT_SEE_TAGZ = "I don't see any tags, stopping!";
	private final static String FOLLOWING_ = "Following tag number ";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_recognize);
        preview = (CameraDrawerPreview) findViewById(R.id.recognize_preview);
        preview.setCameraSetupCallback(this);
        preview.setCameraProcessingCallback(this);
		helper = new OfflineDataHelper(this);
		Button screenshotButton = (Button) findViewById(R.id.screenshot_button);
		screenshotButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bitmap bmp = preview.getScreenShot();
				SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss",Locale.getDefault());
				String date = s.format(new Date());
				helper.saveScreenshot(bmp, "screenshot-"+date+".png");
			}
		});
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
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent intent = getIntent();
		String extra = intent.getStringExtra(MainActivity.INTENT_EXTRA);
		if(extra.contentEquals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
			ttstt = new TextToSpeechToText(this);
			ttstt.setSpeechToTextListener(this);
		}
		mainLooper = this.getMainLooper();
		for(boolean t : tagMap){
			t = false;
		}
		super.onCreate(savedInstanceState);
	}
	@Override
	protected void onResume() {
		driverHelper = new DriverHelper(this, (UsbManager) getSystemService(Context.USB_SERVICE));
		if(asked != true){
			asked = true;
			Handler h = new Handler(mainLooper);
			h.post(new Runnable() {
				
				@Override
				public void run() {
					if(ttstt != null){
						ttstt.speak(WHAT_TAG_FOLLOW);
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
		}
		super.onResume();
	}
	@Override
	protected void onPause() {
		if(driverHelper != null){
			driverHelper.unregisterReceiver();
		}
		if(ttstt != null){
			ttstt.onPause();
		}
		super.onPause();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.recognizer, menu);
		return true;
	}
	int maxW = 0;
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
					maxW = size.width;
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
	float XX=0;

	private static int previewX=0;
	private static int previewY=0;
	private static float X=0;
	private static float Y=0;
	private float drawTag(Tag tag, Canvas canvas, Paint paint){
		X = Y = 0;
		for(int c=0;c<4;c++){
			float x1 = tag.points[c].x*viewWidth;
			float y1 = tag.points[c].y*viewHeight;
			float x2 = tag.points[(c+1)%4].x*viewWidth;
			float y2 = tag.points[(c+1)%4].y*viewHeight;
			canvas.drawLine(x1, y1, x2, y2, paint);
			X+=x1;
			Y+=y1;
		}
		canvas.drawText(""+tag.id, X/4, Y/4, paint);
		return X/4;
	}
	private void drawTagPreview(Tag tag, Canvas canvas,  Paint paint){
		canvas.drawBitmap(Misc.mat2Bitmap(tag.preview), previewX, previewY , redPaint);
	}
	private void askIfChanged(final boolean[] newTagMap){
		Boolean diff = false;
		for(int c = 0;c<32 && !diff; c++){
			diff = tagMap[c] != newTagMap[c];
		}
		if(diff){
			confidenceCounter++;
			if(confidenceCounter>20){
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
							if(ttstt != null){
								ttstt.speak(WHAT_TAG_FOLLOW);
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
							if(ttstt != null){
								ttstt.speak(DONT_SEE_TAGZ);
							}else{
								TagDialog tagDialog = new TagDialog(0,32) {
									@Override
									public void onListItemClick(DialogInterface dialog, Integer tag) {
										trackedID = tag;
									}
								};
								tagDialog.show(getFragmentManager(), "tagz");
							}
						}
					});
				}
			}
		}else{
			confidenceCounter = 0;
		}
	}
	@Override
	public void drawOnCamera(Canvas canvas, double scaleX, double scaleY) {
		boolean[] newTagMap = new boolean[32];
		for(boolean t : newTagMap){
			t = false;
		}
		if(tags!=null){
			previewX = previewY = 0;
			for(Tag tag : tags){
				newTagMap[tag.id-1]=true;
				if(tag.id == trackedID){
					float steerX = drawTag(tag, canvas, greenPaint)/viewWidth;
					drawTagPreview(tag, canvas, greenPaint);
					driverHelper.steer((float)(3*(steerX-0.5f)),-1.0f);
					
				}else{
					drawTag(tag, canvas, redPaint);
					drawTagPreview(tag, canvas, redPaint);
					driverHelper.steer(0,0);
				}
				previewX+=tag.preview.cols();
			}
			
		}else{
			driverHelper.steer(0,0);
		}
		
		askIfChanged(newTagMap);
		
		
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
		
	}
	@Override
	public void getPointers(SparseArray<Pointer> pointers) {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void onPartialRecognitionResult(String result, float confidence) {
		Log.v(TAG,"onPartialRecognitionResult:"+result+":"+confidence);
	}
	@Override
	public void onRecognitionResult(String result, float confidence) {
		Log.v(TAG,"onRecognitionResult:"+result+":"+confidence);
		if(confidence < 0.5){
        	ttstt.speak(WHAT_TAG_FOLLOW);
		}else{
	        trackedID = ttstt.stringToInt(result);
	        Log.v(TAG,"response:"+result+":id:"+trackedID);
	        if(trackedID < 1){
	        	ttstt.speak(WHAT_TAG_FOLLOW);
	        }else{
	        	ttstt.speak(FOLLOWING_+trackedID);
	        }
		}
	}
	@Override
	public void onDoneTalking(String text) {
		if(text.contentEquals(FOLLOWING_+trackedID)){

		}else if(text.contentEquals(DONT_SEE_TAGZ)){
			
		}else if(text.contentEquals(WHAT_TAG_FOLLOW)){
			ttstt.recognizeText();
		}
		// TODO Auto-generated method stub
		
	}
}
