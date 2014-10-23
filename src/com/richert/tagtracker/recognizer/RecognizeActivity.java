package com.richert.tagtracker.recognizer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import com.richert.tagtracker.R;
import com.richert.tagtracker.R.id;
import com.richert.tagtracker.R.layout;
import com.richert.tagtracker.R.menu;
import com.richert.tagtracker.driver.DriverHelper;
import com.richert.tagtracker.elements.CameraDrawerPreview;
import com.richert.tagtracker.elements.Constants;
import com.richert.tagtracker.elements.FullScreenActivity;
import com.richert.tagtracker.elements.OfflineDataHelper;
import com.richert.tagtracker.elements.Pointer;
import com.richert.tagtracker.elements.ResolutionDialog;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraProcessingCallback;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraSetupCallback;
import com.richert.tagtracker.geomerty.Tag;
import com.richert.tagtracker.natUtils.Misc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.RotateAnimation;
import android.widget.Button;

public class RecognizeActivity extends FullScreenActivity implements CameraSetupCallback, CameraProcessingCallback{
	private final static String TAG=RecognizeActivity.class.getSimpleName();
	private CameraDrawerPreview preview = null;
	private Camera.Parameters params = null;
	private Recognizer recognizer;
	private OfflineDataHelper helper;
	private Bitmap tmp;
	private int rotation=0;
	private int camWidth, camHeight;
	private int viewWidth, viewHeight;
	private Mat cameraMatrix, distortionMatrix;
	private Boolean showPreview = false;
	private Tag[] tags;
	private Paint paint;
	private DriverHelper driverHelper;
	public int trackedID = 1;
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
		cameraMatrix = helper.loadCameraMatrix();
		distortionMatrix = helper.loadDistortionMatrix();
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.GREEN);
		paint.setStrokeWidth(7.0f);
		paint.setTextSize(25.0f);
		driverHelper = new DriverHelper(this, (UsbManager) getSystemService(Context.USB_SERVICE));
		super.onCreate(savedInstanceState);
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
		if (id == R.id.recognize_action_resolution) {
			params = preview.getCameraParameters();
			ResolutionDialog resolutionDialog = new ResolutionDialog(params.getSupportedPreviewSizes()) {
				@Override
				public void onListItemClick(DialogInterface dialog, Camera.Size size) {
					params.setPreviewSize(size.width, size.height);
					maxW = size.width;
					preview.reloadCameraSetup(params);
					recognizer.notifySizeChanged(size, rotation);
					helper.setResolution(size);
				}
			};
			resolutionDialog.show(getFragmentManager(), "resolutions");
			
			return true;
		}
		return super.onOptionsItemSelected(item);
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
		// TODO Auto-generated method stub
		if(showPreview){
			tmp = recognizer.remapFrame(yuvFrame);
		}
		tags = recognizer.findTags(yuvFrame, rotation);
		preview.requestRefresh();
	}
	Boolean transmiting = false;
	float XX=0;
	Boolean flag = false;

	@Override
	public void drawOnCamera(Canvas canvas, double scaleX, double scaleY) {
		if(showPreview){
			canvas.drawBitmap(tmp,0, 0, new Paint());
		}
		if(tags!=null){
			int x=0;
			int y=0;
			
			for(Tag tag : tags){
				float X=0;
				float Y=0;
				for(int c=0;c<4;c++){
					canvas.drawLine(tag.points[c].x*viewWidth, tag.points[c].y*viewHeight, tag.points[(c+1)%4].x*viewWidth, tag.points[(c+1)%4].y*viewHeight, paint);
					X+=tag.points[c].x;
					Y+=tag.points[c].y;
					
				}
				flag = true;
				canvas.drawBitmap(Misc.mat2Bitmap(tag.preview), x, y , paint);
				canvas.drawText(""+tag.id, (X*camWidth)/4, (Y*camHeight)/4, paint);
				x+=tag.preview.cols();
				if(tag.id == trackedID){
					final float lastX =((float)X/4);
					Log.d(TAG,"blah"+(float)(2*(lastX-0.5f)));
					Log.d(TAG,"w"+viewWidth);
					Log.d(TAG,"h"+viewHeight);
					if(XX != lastX){
						if(!transmiting){
							transmiting = true;
							new Thread(){
								public void run() {
									steer((float)(3*(lastX-0.5f)),-1.0f);
									Log.d(TAG,"blah"+(float)(3*(lastX-0.5f)));
									XX=lastX;
									transmiting = false;
								};
							}.run();
						}
					}
				}
			}
			
		}else{
			if(XX != 0){
				if(!transmiting){
					transmiting = true;
					new Thread(){
						public void run() {
							steer(0,0);
							XX=0;
							transmiting = false;
						};
					}.run();
				}
			}
		}
	}
	@Override
	public void setCameraParameters(Parameters params, int width, int height, int rotation) {
		viewHeight = height;
		viewWidth = width;
		Log.v(TAG,"params.getPreviewSize()=w:"+params.getPreviewSize().width+":h:"+params.getPreviewSize().height);
		recognizer.notifySizeChanged(params.getPreviewSize(), rotation);
	}
	@Override
	public void setCameraInitialParameters(Parameters params, int width, int height, int rotation) {
		viewHeight = height;
		viewWidth = width;
		camWidth = helper.getResolutionWidth(params.getPreviewSize().width);
		camHeight = helper.getResolutionHeight(params.getPreviewSize().height);
		params.setPreviewSize(camWidth, camHeight);
		recognizer = new Recognizer(cameraMatrix,distortionMatrix, width, height);
		recognizer.notifySizeChanged(params.getPreviewSize(), rotation);
		
	}
	@Override
	public void getPointers(SparseArray<Pointer> pointers) {
		// TODO Auto-generated method stub
		
	}
	String sent="";
	int steerMax = 240;
	int steerMin = 115;
	public void steer(float procX, float procY){
		int steer, lFront, lBack, rFront, rBack;
		int steerCenter = (steerMax - steerMin)/2 + steerMin;
		steer = steerCenter - (int)(procX*((steerMax - steerMin)/2));

		lBack = Math.min(procY > 0 ? procX > 0 ? (int)(procY*255) : Math.max((int)(procY*255)+(int)(procX*50),0) : 0, 255);//leftT
		lFront = Math.min(procY < 0 ? procX > 0 ? -(int)(procY*255) : Math.max(-(int)(procY*255)+(int)(procX*50),0) : 0, 255);//leftP
		rBack = Math.min(procY > 0 ? procX < 0 ? (int)(procY*255) : Math.max((int)(procY*255)-(int)(procX*50),0) : 0, 255);//rightT
		rFront = Math.min(procY < 0 ? procX < 0 ? -(int)(procY*255) : Math.max(-(int)(procY*255)-(int)(procX*50),0) : 0, 255);//rightP
		
		
		
		sent = ""+steer+","+lFront+","+lBack+","+rFront+","+rBack+",";
		driverHelper.send(sent.getBytes());
	}
}
