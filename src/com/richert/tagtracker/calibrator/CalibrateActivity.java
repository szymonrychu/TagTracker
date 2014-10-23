package com.richert.tagtracker.calibrator;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import com.richert.tagtracker.R;
import com.richert.tagtracker.elements.CameraDrawerPreview;
import com.richert.tagtracker.elements.FullScreenActivity;
import com.richert.tagtracker.elements.OfflineDataHelper;
import com.richert.tagtracker.elements.Pointer;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraProcessingCallback;
import com.richert.tagtracker.elements.CameraDrawerPreview.CameraSetupCallback;
import com.richert.tagtracker.geomerty.Point;
import com.richert.tagtracker.natUtils.Misc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class CalibrateActivity  extends FullScreenActivity implements CameraSetupCallback, CameraProcessingCallback{
	private static final String TAG = CalibrateActivity.class.getSimpleName();
	private CameraDrawerPreview preview;
	private Calibrator calibrator;
	private Point[] points;
	private Paint paint;
	private Button addToSetButton;
	private Button processDataButton;
	private OfflineDataHelper helper;
	private int viewWidth, viewHeight;
	Bitmap bmp;
	private int rotation;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibrate);
		preview = (CameraDrawerPreview) findViewById(R.id.calibrate_preview);
		preview.setCameraProcessingCallback(this);
		preview.setCameraSetupCallback(this);
		addToSetButton = (Button) findViewById(R.id.butt_calibrate_addframe);
		calibrator = new Calibrator();
		helper = new OfflineDataHelper(this);
		addToSetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				calibrator.addFrameToSet();
			}
		});
		processDataButton = (Button) findViewById(R.id.butt_calibrate_process);
		processDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Thread th = new Thread(new Runnable() {
					@Override
					public void run() {
						calibrator.processFrames();
						Log.d(TAG,"calibrated!");
						Mat cameraMatrix = calibrator.getCameraMatrix();
						Mat distortionCoefficient = calibrator.getDistortionCoefficient();
						if(distortionCoefficient != null && cameraMatrix != null){
							helper.saveCameraMatrix(cameraMatrix);
							helper.loadCameraMatrix();
							helper.saveDistortionMatrix(distortionCoefficient);
							helper.loadDistortionMatrix();
						}
					}
				});
				th.start();
			}
		});
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.GREEN);
		paint.setStrokeWidth(10.0f);
		paint.setTextSize(10.0f);
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
		points = calibrator.detectChessBoard(yuvFrame, rotation);
		preview.requestRefresh();
	}
	@Override
	public void drawOnCamera(Canvas canvas, double scaleX, double scaleY) {
		if(points != null)
			for(Point point : points){
				canvas.drawPoint(point.x*viewWidth, point.y*viewHeight, paint);
		}
		//canvas.drawBitmap(bmp, 100, 100, paint);
	}

	@Override
	public void setCameraParameters(Parameters params, int width, int height, int rotation) {
		viewHeight = height;
		viewWidth = width;
		this.rotation = rotation;
		Camera.Size bestFit = params.getPreviewSize();
		float ratio=1;
		for(Camera.Size size : params.getSupportedPreviewSizes()){
			float x = ((float)size.width/(float)width);
        	float y = ((float)size.height/(float)height);
        	if(Math.abs(x/y-1) < ratio){
        		bestFit=size;
        		ratio=Math.abs(x/y-1);
        	}
		}
		params.setPreviewSize(bestFit.width, bestFit.height);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
	}

	@Override
	public void setCameraInitialParameters(Parameters params, int width, int height, int rotation) {
		viewHeight = height;
		viewWidth = width;
		this.rotation = rotation;
		Log.d(TAG,"camera dimensions: w:"+width+":h:"+height);
		Camera.Size bestFit = params.getPreviewSize();
		float ratio=1;
		for(Camera.Size size : params.getSupportedPreviewSizes()){
			Log.d(TAG,"proposed camera dimensions: w:"+size.width+":h:"+size.height);
			float x = ((float)size.width/(float)width);
        	float y = ((float)size.height/(float)height);
        	if(Math.abs(x/y-1) < ratio){
        		bestFit=size;
        		ratio=Math.abs(x/y-1);
        	}
		}
		params.setPreviewSize(bestFit.width, bestFit.height);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
	}

	@Override
	public void getPointers(SparseArray<Pointer> pointers) {
		// TODO Auto-generated method stub
		
	}

}
