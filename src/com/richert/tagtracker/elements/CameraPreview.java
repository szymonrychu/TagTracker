package com.richert.tagtracker.elements;

import org.opencv.android.Utils;
import org.opencv.android.local.Misc;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import com.richert.tagtracker.activities.DriverActivity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public abstract class CameraPreview extends ViewGroup {
	private final static String TAG = CameraPreview.class.getSimpleName();
	private Context context = null;
	private Camera camera = null;
	private int maxThreads = 1;
	private double scaleX, scaleY;
	private int height, width, rotation;
	private CameraView cameraView;
	private CameraSetupCallback cameraSetupCallback = null;
	private Mat yuvFrame;
	private Canvas canvas;
	private Boolean rotate=false;
	private Boolean recreateMatFrame=false;
	private CameraProcessingCallback cameraProcessingCallback= null;
	public static int ROTATION_PORTRAIT=0;
	public static int ROTATION_LANDSCAPE=1;
	public static int ROTATION_PORT_UPS_DOWN=3;
	public static int ROTATION_LANDS_UPS_DOWN=4;
	/**
	 * CameraSetupCallback -this callback is responsible for setting correct camera parameters for preview.
	 * @author szymon
	 *
	 */
	public interface CameraSetupCallback{
		/**
		 * Method setup the camera for preview. To set your own parameters, please edit params and run CameraDrawerPreview.reloadCameraSetup().
		 * @param params -actual camera parameters
		 * @param width -width of the view presenting the camera preview
		 * @param height -height of the view presenting the camera preview
		 */
		public void setCameraParameters(Camera.Parameters params, int width, int height, int rotation);
		/**
		 * Method initially setup the camera for preview. To set your own parameters, please edit params and run CameraDrawerPreview.reloadCameraSetup().
		 * @param params -actual camera parameters
		 * @param width -width of the view presenting the camera preview
		 * @param height -height of the view presenting the camera preview
		 */
		public void setCameraInitialParameters(Camera.Parameters params, int width, int height, int rotation);
	}
	/**
	 * CameraProcessingCallback -callback responsible for camera preview processing.
	 * @author szymon
	 *
	 */
	public interface CameraProcessingCallback{
		/**
		 * Method runned in separate thread for camera preview processing.
		 * @param yuvFrame -frame directly taken from camera preview
		 * @param thiz -thread in which the method is running- prepared for queuing threads. Thread is running in max priority.
		 * @param rotation -parameter with information about rotation. In degrees.
		 */
		public void processImage(Mat yuvFrame, Thread thiz);
	}
	/**
	 * Method to setup the cameraSetupCallback
	 * @param c -CameraSetupCallback
	 */
	public void setCameraSetupCallback(CameraSetupCallback c){
		this.cameraSetupCallback = c;
	}
	/**
	 * Method to setup the cameraProcessingCallback
	 * @param c -CameraProcessingCallback
	 */
	public void setCameraProcessingCallback(CameraProcessingCallback c){
		this.cameraProcessingCallback = c;
	}
	/**
	 * Method to obtain amount of max camera processing threads running in pararell.
	 * @return max number of threads.
	 */
	public int getMaxThreads() {
		return maxThreads;
	}
	/**
	 * Method to setup amount of max camera processing threads running in pararell.
	 * @param maxThreads
	 */
	public void setMaxThreads(int maxThreads) {
		
		if(maxThreads > 0 || maxThreads==-1 ){
			this.maxThreads = maxThreads;
			if(maxThreads != 1){
				recreateMatFrame = true;
			}else{
				recreateMatFrame = false;
			}
		}
	}
	/**
	 * Method for reloading camera parameters.
	 * @param params -new cameraParameters
	 */
	public void reloadCameraSetup(Camera.Parameters params){
		cameraView.reloadCameraSetup(params);
	}
	/**
	 * Method created for obtaining camera parameters.
	 * @return actual camera parameters.
	 */
	public Camera.Parameters getCameraParameters(){
		return cameraView.getCameraParameters();
	}
	//##########################################################3
	private void init(Context context){
		this.context = context;
		this.cameraView = new CameraView(this);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT );
		addView(cameraView,params);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.EXACTLY);
		int h = MeasureSpec.makeMeasureSpec(heightMeasureSpec, MeasureSpec.EXACTLY);
		for(int i=0; i<getChildCount(); i++){
			View v = getChildAt(i);
			v.measure(w, h);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	public CameraPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public CameraPreview(Context context) {
		super(context);
		init(context);
	}
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		for(int i=0;i<count;i++){
			View child = getChildAt(i);
			child.layout(l, t, r, b);
		}
	}
	private class CameraView extends SurfaceView implements Callback, PreviewCallback, Runnable{
		private SurfaceHolder surfaceHolder;
		private Boolean previewRunning = false;
		private Boolean initial = true;
		public CameraView(CameraPreview parent){
			super(context);
			this.surfaceHolder = getHolder();
			this.surfaceHolder.addCallback(this);
		}
		protected Camera.Parameters getCameraParameters(){
			return camera.getParameters();
		}
		private int threads=0;
		Thread cameraPreview;
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			if(cameraProcessingCallback != null){
				int h = camera.getParameters().getPreviewSize().height;
				int w = camera.getParameters().getPreviewSize().width;
				if(recreateMatFrame){
					yuvFrame = new Mat(h+h/2, w, CvType.CV_8UC1);
				}
				yuvFrame.put(0, 0, data);
				if(maxThreads==-1 || threads<=maxThreads){
					threads++;
					Thread thread = new Thread(){
						public void run(){
							if(previewRunning){
								cameraProcessingCallback.processImage(yuvFrame, this);
							}
							threads--;
						}
					};
					thread.setPriority(Thread.MAX_PRIORITY);
					thread.start();
				}
			}
		}
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if(camera!=null){
				camera.release();
				camera=null;
			}
			camera = Camera.open();
	        try {
				camera.setPreviewDisplay(this.surfaceHolder);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int widthL,
				int heightL) {
			width = widthL;
			height = heightL;
			Log.v(TAG,"updated: w:"+width+":h:"+height);

			Camera.Parameters params = camera.getParameters();
	        reloadCameraSetup(params);
			
		}
		private void setupCameraDimens(Camera.Size size){
			WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Display display = (manager).getDefaultDisplay();
			rotation = display.getRotation();
        	camera.setDisplayOrientation((4-rotation+1)%4*90);
		}
		protected void reloadCameraSetup(Camera.Parameters params){
			try{
				camera.setPreviewCallback(null);
				camera.stopPreview();
				if(cameraPreview!=null){
					previewRunning = false;
					cameraPreview.join();
					cameraPreview = null;
				}
				setupCameraDimens(params.getPreviewSize());
				if(cameraSetupCallback!=null){
					if(initial){
						initial = false;
						if(rotation%2==0){
							cameraSetupCallback.setCameraInitialParameters(params, height, width,  rotation);
						}else{
							cameraSetupCallback.setCameraInitialParameters(params, height, width, rotation);
						}
					}else{
						if(rotation%2==0){
							cameraSetupCallback.setCameraParameters(params, height, width, rotation);
						}else{
							cameraSetupCallback.setCameraParameters(params, width, height, rotation);
						}
					}
				}
				camera.setParameters(params);
				int w = params.getPreviewSize().width;
				int h = params.getPreviewSize().height;
				yuvFrame = new Mat(h+h/2, w, CvType.CV_8UC1);
				previewRunning = true;
				cameraPreview = new Thread(this);
				cameraPreview.start();
			}catch(InterruptedException e){
				Log.e(TAG,"Couldn't reload camera parameters: "+e.getMessage());
			}
        	
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			previewRunning = false;
			try {
				if(cameraPreview != null){
					cameraPreview.join();
					cameraPreview = null;
				}
				if(camera!=null){
					camera.stopPreview();
					camera.setPreviewCallback(null);
					camera.release();
					camera=null;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		@Override
		public void run() {
			camera.setPreviewCallback(this);
			if(previewRunning){
				camera.startPreview();
			}
		}
	}
	
}
