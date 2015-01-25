package com.richert.tagtracker.activities;

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
import org.opencv.android.local.Misc;
import org.opencv.android.local.RecognizerService;
import org.opencv.android.local.RecognizerService.ProcessingCallback;
import org.opencv.android.local.Tag;
import org.opencv.core.Mat;

import com.richert.tagtracker.R;
import com.richert.tagtracker.R.id;
import com.richert.tagtracker.R.layout;
import com.richert.tagtracker.R.menu;
import com.richert.tagtracker.elements.CameraDrawerPreview;
import com.richert.tagtracker.elements.DriverHelper;
import com.richert.tagtracker.elements.FullScreenActivity;
import com.richert.tagtracker.elements.PerformanceTester;
import com.richert.tagtracker.elements.PerformanceTester.TestResultCallback;
import com.richert.tagtracker.elements.ResolutionDialog;
import com.richert.tagtracker.elements.TagDialog;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraProcessingCallback;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraSetupCallback;
import com.richert.tagtracker.elements.ThreadDialog;
import com.richert.tagtracker.monitor.CpuInfo;
import com.richert.tagtracker.monitor.MonitoringService;
import com.richert.tagtracker.monitor.MonitoringService.MonitoringBinder;
import com.richert.tagtracker.multitouch.Pointer;
import com.richert.tagtracker.processing.OfflineDataHelper;
import com.richert.tagtracker.processing.LoadBalancer.InvalidStateException;
import com.richert.tagtracker.ttstt.LanguageHelper;
import com.richert.tagtracker.ttstt.TextToSpeechToText;
import com.richert.tagtracker.ttstt.TextToSpeechToText.SpeechToTextListener;
import com.richert.tagtracker.usb.UsbConnectionService;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
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
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
import android.widget.Toast;

public class RecognizeActivity extends FullScreenActivity implements CameraSetupCallback, CameraProcessingCallback, ProcessingCallback{
	private final static String TAG=RecognizeActivity.class.getSimpleName();
	private static final int ID = RecognizeActivity.class.hashCode();
	private CameraDrawerPreview preview = null;
	private Camera.Parameters params = null;
	private OfflineDataHelper helper;
	private int rotation=0;
	private int camWidth, camHeight;
	private int viewWidth, viewHeight;
	private Tag[] tags;
	private Paint greenPaint, redPaint, bluePaint;
	public int trackedID = -1;
	private boolean[] tagMap = new boolean[32];
	private int confidenceCounter = 0;
	private int previewX=0;
	private int previewY=0;
	private boolean showDebug = false;
	private Button screenshotButton;
	//binders 
	private UsbConnectionService.UsbDataCommunicationBinder usbBinder;
	private RecognizerService.MatProcessingBinder matBinder;
	private Intent startUsbService;
	private Intent startRecognizerService;
	private Intent startMonitorService;
	
	private MonitoringBinder monitorBinder;

	private ServiceConnection usbConnection;
	private ServiceConnection recognizerConnection;
	private ServiceConnection monitorConnection;
	
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
		for(boolean t : tagMap){
			t = false;
		}

		screenshotButton = (Button) findViewById(R.id.screenshot_button);
		screenshotButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
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
	private void setupMatBinder(){
		matBinder.setProcessingCallback(this);
		try {
			matBinder.setMaxPoolSize(8);
			matBinder.setMaxThreadsNum(5);
		} catch (InvalidStateException e) {
			Toast.makeText(getBaseContext(), "Set threads and pool num failed!", Toast.LENGTH_SHORT).show();
		}
		try {
			matBinder.startProcessing();
		} catch (InterruptedException e) {
		}
	}
	private void setupMonitorBinder(){
		monitorBinder.startCollecting(this, 400, 250);
	}
	private void startServices(){
		startMonitorService = new Intent(this, MonitoringService.class);
		startService(startMonitorService);
		monitorConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				monitorBinder = (MonitoringBinder) service;
				setupMonitorBinder();
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {
				monitorBinder = null;
			}
		};
		bindService(startMonitorService,  monitorConnection, Context.BIND_AUTO_CREATE);
		startRecognizerService = new Intent(this, RecognizerService.class);
		startService(startRecognizerService);
		recognizerConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				matBinder = (RecognizerService.MatProcessingBinder) service;
				setupMatBinder();
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {
				matBinder = null;
			}
		};
		bindService(startRecognizerService,  recognizerConnection, Context.BIND_AUTO_CREATE);
		
		startUsbService = new Intent(this, UsbConnectionService.class);
		startService(startUsbService);
		usbConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				usbBinder = (UsbConnectionService.UsbDataCommunicationBinder) service;
				
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {
				usbBinder = null;
			}
		};
		
		bindService(startUsbService, usbConnection, Context.BIND_AUTO_CREATE);
	}
	private void stopServices(){
		unbindService(monitorConnection);
		unbindService(usbConnection);
		unbindService(recognizerConnection);
		stopService(startRecognizerService);
		stopService(startUsbService);
		stopService(startMonitorService);
	}
	@Override
	protected void onResume() {
		startServices();
		super.onResume();
	}
	@Override
	protected void onPause() {
		stopServices();
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
					if(matBinder != null){
						matBinder.notifySizeChanged(size, rotation);
					}
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
			        try {
						matBinder.setMaxThreadsNum(num);
					} catch (InvalidStateException e) {
						Toast.makeText(getBaseContext(), "Set threads num failed!", Toast.LENGTH_SHORT).show();
					}
				}
			};
			threadDialog.show(getFragmentManager(), "threadz");
			return true;
		case R.id.recognize_action_debug:
			item.setChecked(! item.isChecked());
			showDebug = item.isChecked();
			if(showDebug){
				screenshotButton.setVisibility(View.VISIBLE);
				if(monitorBinder != null){
					matBinder.setMonitor(monitorBinder);
					monitorBinder.startCollecting(this, 400, 250);
				}
			}else{
				screenshotButton.setVisibility(View.INVISIBLE);
				monitorBinder.stopCollecting();
				matBinder.unsetMonitor();
			}
			return true;
		case R.id.recognize_action_graph:
			item.setChecked(! item.isChecked());
			//showCPU = item.isChecked();
			return true;
		case R.id.recognize_force_speaking:
			item.setChecked(! item.isChecked());
			//forceSpeaking = item.isChecked();
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
		if(matBinder != null){
			matBinder.processMat(yuvFrame);
		}else{
			Log.d(TAG, "not connected to service!");
		}
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
				if(t.id>0)
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
				//TODO
			}
		}else{
			confidenceCounter = 0;
		}
	}
	private void drawDebugInfo(Canvas canvas){
		if(showDebug){
			monitorBinder.drawData(15, 15, canvas, redPaint);
		}
	}
	private float driverY = -1;
	@Override
	public void drawOnCamera(Canvas canvas, double scaleX, double scaleY) {
		float driverX = 0;
		float driverY = 0;
		synchronized(canvas){
			if(tags!=null){
				for(Tag tag : tags){
					driverY += tag.center.y;
					if(tag.id == trackedID){
						drawTag(tag, canvas, greenPaint);
						drawTagPreview(tag, canvas, greenPaint);
						if(usbBinder != null){
							float steerX = (float)(3*((tag.center.x)-0.5f));
							usbBinder.steer(steerX,-1.0f,driverY);
						}
						
					}else{
						drawTag(tag, canvas, redPaint);
						drawTagPreview(tag, canvas, redPaint);
						if(usbBinder != null){
							usbBinder.steer(0,0,this.driverY);
						}
						float z = (float)5*((tag.center.y)-0.5f);
					}
				}
				float driverTmpY = (float)5*((driverY)-0.5f)/tags.length;
				if(this.driverY < 0){
					
					this.driverY = driverTmpY;
				}else{
					if(Math.abs(driverTmpY - this.driverY)>0.1){
						this.driverY = driverTmpY;
					}
				}
				
				
			}else{
				if(usbBinder != null){
					usbBinder.steer(0,0,this.driverY);
				}
			}
			drawDebugInfo(canvas);
		}
		
		
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
					//Log.v(TAG," b:"+a.rect.bottom +" t:"+a.rect.top+" l:"+a.rect.left+" r:"+a.rect.right);
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
		if(matBinder != null){
			matBinder.notifySizeChanged(params.getPreviewSize(), rotation);
		}
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
		if(matBinder != null){
			matBinder.notifySizeChanged(params.getPreviewSize(), rotation);
		}
	}
	@Override
	public void getPointers(SparseArray<Pointer> pointers) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void post(Tag[] tagz) {
		tags = tagz;
		preview.requestRefresh();
	}
	
}
