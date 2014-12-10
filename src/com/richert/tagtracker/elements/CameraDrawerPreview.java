package com.richert.tagtracker.elements;

import org.opencv.android.Utils;
import org.opencv.android.local.Misc;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import com.richert.tagtracker.views.DriverActivity;

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
import android.hardware.Camera.Parameters;
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

public class CameraDrawerPreview extends ViewGroup {
	private final static String TAG = CameraDrawerPreview.class.getSimpleName();
	private Context context = null;
	private Camera camera = null;
	private double scaleX, scaleY;
	private int height, width, rotation;
	private CameraView cameraView;
	private DrawerView drawerView;
	private CameraSetupCallback cameraSetupCallback = null;
	private Mat yuvFrame;
	private Canvas canvas;
	private Boolean rotate=false;
	private Boolean recreateMatFrame=false;
	private CameraProcessingCallback cameraProcessingCallback= null;
	private OnMultitouch listener;
	private static long drawingPrevTime = 0;
	protected static long drawingTime = 0;
	private static long processingPrevTime = 0;
	protected static long processingTime = 0;
	private static long frameDelayPrevTime = 0;
	protected static long frameDelayTime = 0;
	protected static long oneThreadPDelayTime = 0;
	protected static long oneThreadDDelayTime = 0;
	private static long timeTmp3 = 0;
	protected static int droppedFrames = 0;
	private static int maxThreads = 1;
	protected static int threads=0;
	
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
		/**
		 * Method designed to refresh upper layer of preview with canvas.
		 * @param canvas -canvas in which results of processing can be rendered.
		 * @param scaleX -scale of x axis 
		 * @param scaleY -scale of y axis
		 */
		public void drawOnCamera(Canvas canvas, double scaleX, double scaleY);
		/**
		 * Method designed to retrieve points, where it was touched.
		 * @param pointers
		 */
		public void getPointers(SparseArray<Pointer> pointers);
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
	public int getThreadsNum(){
		return threads;
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
	/**
	 * Method getting actual preview's screenshot.
	 * @return bitmap containing preview with overlayed data.
	 */
	public Bitmap getScreenShot(){
		Bitmap bmp = Misc.mat2Bitmap(Misc.yuv2Rgb(yuvFrame, rotation));
		if(cameraProcessingCallback != null){
			Canvas canvas = new Canvas(bmp);
			cameraProcessingCallback.drawOnCamera(canvas, scaleX, scaleY);
		}
		return bmp;
	}
	/**
	 * Method is requesting refresh of surface child view, responsible for presenting drawings. 
	 */
	public void requestRefresh(){
		drawerView.refresh();
	}
	public String getState(){
		StringBuilder sb = new StringBuilder();
		sb.append("frame=");
		sb.append(frameDelayTime);
		sb.append(" processing=");
		sb.append(processingTime);
		sb.append(" redrawing=");
		sb.append(drawingTime);
		sb.append(" frameDrop=");
		sb.append(droppedFrames);
		sb.append(" delay=");
		sb.append(oneThreadDDelayTime + oneThreadPDelayTime);
		sb.append(" workers=");
		sb.append(threads);
		return sb.toString();
	}
	//##########################################################
	private void init(Context context){
		this.context = context;
		this.cameraView = new CameraView(this);
		this.drawerView = new DrawerView(this);
		this.listener = new OnMultitouch(drawerView);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT );
		addView(drawerView,params);
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
	public CameraDrawerPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	public CameraDrawerPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public CameraDrawerPreview(Context context) {
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
		public CameraView(CameraDrawerPreview parent){
			super(context);
			this.surfaceHolder = getHolder();
			this.surfaceHolder.addCallback(this);
		}
		protected Camera.Parameters getCameraParameters(){
			return camera.getParameters();
		}
		Thread cameraPreview;
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			timeTmp3 = System.currentTimeMillis();
			frameDelayTime = timeTmp3 - frameDelayPrevTime;
			frameDelayPrevTime = timeTmp3;
			if(cameraProcessingCallback != null){
				int h = camera.getParameters().getPreviewSize().height;
				int w = camera.getParameters().getPreviewSize().width;
				if(recreateMatFrame){
					yuvFrame = new Mat(h+h/2, w, CvType.CV_8UC1);
				}
				yuvFrame.put(0, 0, data);
				if(maxThreads==-1 && threads < 100 || threads<maxThreads){
					droppedFrames = 0;
					threads++;
					Thread thread = new Thread(){
						public void run(){ 
							long timeTmp1 = System.currentTimeMillis();
							processingTime = timeTmp1 - processingPrevTime;
							processingPrevTime = System.currentTimeMillis();
							if(previewRunning){
								cameraProcessingCallback.processImage(yuvFrame, this);
								cameraProcessingCallback.getPointers(listener.getPoints());
							}
							threads--;
							oneThreadPDelayTime = System.currentTimeMillis() - timeTmp1;
						}
					};
					thread.setPriority(Thread.MAX_PRIORITY);
					thread.start();
				}else{
					droppedFrames++;
				}
			}
		}
		@SuppressWarnings("deprecation")
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if(camera!=null){
				camera.release();
				camera=null;
			}
			for(int c=0;c<10 && camera == null; c++){
				try {
					camera = Camera.open();
					camera.setPreviewDisplay(this.surfaceHolder);
					Thread.sleep(100);
				}catch (Exception e) {
					if(camera != null){
						camera.release();
						camera=null;
					}
				} 
			}
			if(camera == null){
				//TODO no camera error
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
	private class DrawerView extends SurfaceView implements Callback, Runnable{
		private SurfaceHolder surfaceHolder;
		private Boolean draw = false;
		public DrawerView(CameraDrawerPreview parent){
			super(context);
			this.surfaceHolder = getHolder();
			this.surfaceHolder.addCallback(this);
			this.surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
		}
		@Override
		public void surfaceCreated(SurfaceHolder holder){}
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			draw = true;
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			draw = false;
		}
		public synchronized void refresh(){
			if(draw){
				canvas=surfaceHolder.lockCanvas();
				if(canvas!=null){
					synchronized(canvas){
						canvas.drawColor(0, Mode.CLEAR);
						long timeTmp2 = System.currentTimeMillis();
						drawingTime = timeTmp2 - drawingPrevTime;
						drawingPrevTime = timeTmp2;
						if(cameraProcessingCallback != null){
							cameraProcessingCallback.drawOnCamera(canvas,scaleX,scaleY);
						}
						surfaceHolder.unlockCanvasAndPost(canvas);
						oneThreadDDelayTime = System.currentTimeMillis() - timeTmp2;
					}	
				}
			}
		}
		@Override
		public void run() {
			refresh();
		}
		
	}
}
