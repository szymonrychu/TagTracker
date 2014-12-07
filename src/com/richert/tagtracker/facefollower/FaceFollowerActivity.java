package com.richert.tagtracker.facefollower;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.opencv.core.Mat;

import com.richert.tagtracker.R;
import com.richert.tagtracker.driver.DriverHelper;
import com.richert.tagtracker.elements.CameraDrawerPreview;
import com.richert.tagtracker.elements.CpuInfo;
import com.richert.tagtracker.elements.FullScreenActivity;
import com.richert.tagtracker.elements.LanguageHelper;
import com.richert.tagtracker.elements.OfflineDataHelper;
import com.richert.tagtracker.elements.PerformanceTester;
import com.richert.tagtracker.elements.Pointer;
import com.richert.tagtracker.elements.ResolutionDialog;
import com.richert.tagtracker.elements.TagDialog;
import com.richert.tagtracker.elements.TextToSpeechToText;
import com.richert.tagtracker.elements.ThreadDialog;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraProcessingCallback;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraSetupCallback;
import com.richert.tagtracker.elements.PerformanceTester.TestResultCallback;
import com.richert.tagtracker.geomerty.Tag;
import com.richert.tagtracker.recognizer.RecognizeActivity;
import com.richert.tagtracker.recognizer.Recognizer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

public class FaceFollowerActivity extends FullScreenActivity implements CameraSetupCallback, CameraProcessingCallback{
	private final static String TAG=FaceFollowerActivity.class.getSimpleName();
	private CameraDrawerPreview preview = null;
	private OfflineDataHelper helper;
	private Paint greenPaint;
	private DriverHelper driverHelper;
	private Camera.Parameters params = null;
	private int rotation;
	private Tag[] tags;
	private int viewWidth, viewHeight;
	private int camWidth, camHeight;
	private String filename;
	private FaceFollower recognizer;
	public FaceFollowerActivity() {
		// TODO Auto-generated constructor stub
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {setContentView(R.layout.activity_recognize);
    	setContentView(R.layout.activity_facefollower);
	    preview = (CameraDrawerPreview) findViewById(R.id.facefollower_preview);
	    preview.setCameraSetupCallback(this);
	    preview.setCameraProcessingCallback(this);
		helper = new OfflineDataHelper(this);
		greenPaint = new Paint();
		greenPaint.setAntiAlias(true);
		greenPaint.setColor(Color.GREEN);
		greenPaint.setStrokeWidth(7.0f);
		greenPaint.setTextSize(25.0f);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		driverHelper = new DriverHelper(this, (UsbManager) getSystemService(Context.USB_SERVICE));
		filename = "";
		super.onCreate(savedInstanceState);
	}
	@Override
	protected void onResume() {
		driverHelper.startMonitor(this);
		recognizer = new FaceFollower(this);
		super.onResume();
	}
	@Override
	protected void onPause() {
		if(driverHelper != null){
			driverHelper.unregisterReceiver();
		}
		super.onPause();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.facefollower, menu);
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
					//recognizer.notifySizeChanged(size, rotation);
					helper.setResolution(size);
				}
			};
			resolutionDialog.show(getFragmentManager(), "resolutions");
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
		default:
			return false;
		}
	}
	private void drawTag(Tag tag, Canvas canvas, Paint paint){
		for(int c=0;c<4;c++){
			float x1 = tag.points[c].x;
			float y1 = tag.points[c].y;
			float x2 = tag.points[(c+1)%4].x;
			float y2 = tag.points[(c+1)%4].y;
			canvas.drawLine(x1, y1, x2, y2, paint);
		}
		canvas.drawText("face", tag.center.x, tag.center.y, paint);
	}
	@Override
	public void processImage(Mat yuvFrame, Thread thiz) {
		tags = recognizer.findTags(yuvFrame, rotation);
		preview.requestRefresh();
	}
	float meanY = 177;
	@Override
	public void drawOnCamera(Canvas canvas, double scaleX, double scaleY) {
		float Y = -1;
		if(tags!=null){
			for(Tag tag : tags){
				drawTag(tag, canvas, greenPaint);
				Y = (float)5*((tag.center.y/viewHeight)-0.5f);
				driverHelper.steer((float)(3*((tag.center.x/viewWidth)-0.5f)),-1.0f,meanY);
			}
			if(meanY < 0){
				meanY = Y;
			}else{
				if(Math.abs(meanY - Y)>0.1){
					meanY = Y;
				}
			}
		}else{
			driverHelper.steer(0,0,meanY);
		}
	}
	@Override
	public void getPointers(SparseArray<Pointer> pointers) {
		// TODO Auto-generated method stub
		
	}
	boolean sw = false;
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
		Log.v(TAG,"setCameraParameters=w:"+params.getPreviewSize().width+":h:"+params.getPreviewSize().height);
		//recognizer.notifySizeChanged(params.getPreviewSize(), rotation);
	}
	@Override
	public void setCameraInitialParameters(Parameters params, int width, int height, int rotation) {
		setCameraParameters(params);
		viewHeight = height;
		viewWidth = width;
		camWidth = helper.getResolutionWidth(params.getPreviewSize().width);
		camHeight = helper.getResolutionHeight(params.getPreviewSize().height);
		params.setPreviewSize(camWidth, camHeight);
		Log.v(TAG,"setCameraInitialParameters=w:"+params.getPreviewSize().width+":h:"+params.getPreviewSize().height);
	}
	@Override
	protected void onSystemBarsVisible() {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void onSystemBarsHided() {
		// TODO Auto-generated method stub
		
	}
}
