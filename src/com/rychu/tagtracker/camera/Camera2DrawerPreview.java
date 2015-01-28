package com.rychu.tagtracker.camera;

import org.opencv.core.Mat;





import com.rychu.tagtracker.processing.LoadBalancer;
import com.rychu.tagtracker.processing.LoadBalancer.InvalidStateException;
import com.rychu.tagtracker.processing.LoadBalancer.Task;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.WindowManager;

public class Camera2DrawerPreview extends ViewGroup implements CanvasTextureView.RedrawingCallback, Camera2Preview.Camera2TextureViewCallback{
	public interface Camera2ProcessingCallback{
		/**
		 * Function called every time, when new image matrix is available. New images comes in YUV_420_888.
		 * @param mat matrix in YUV_420_888 format
		 * @param imgW width of the image in the mat
		 * @param imgH height of the image in the mat
		 * @param prevW width of the preview window
		 * @param prevH height of the preview window
		 * @param r actual rotation of the view
		 */
		void processImage(Mat mat, int imgW, int imgH, int prevW, int prevH, int r);
		/**
		 * Function called every time the result of the processing cames. 
		 * @param canvas canvas on which the processing data may be drawed onto
		 * @param w width of the canvas 
		 * @param h height of the canvas
		 */
		void redraw(Canvas canvas, int w, int h);
		/**
		 * Function called always when the processing is started
		 */
		void init(Context context);
		/**
		 * Function called every time when processing algorithms aren't needed anymore.
		 */
		void destroy();

		Size setCameraProcessingSize(int viewWidth, int viewHeight, final Size[] sizes);
	}
	public interface Camera2SetupCallback{
		CaptureRequest.Builder setupCamera(CameraDevice cameraDevice);
		void onCameraOpen(CameraCharacteristics characteristics);
		Size setCameraPreviewSize(int viewWidth, int viewHeight, final Size[] sizes);
		void onError(int error);
	}
	
	private Context context;
	// views 
	protected Camera2Preview camera2View;
	protected CanvasTextureView canvasView;
	protected LoadBalancer balancer;
	private Camera2SetupCallback setupCallback;
	private Camera2ProcessingCallback processingCallback;
	public int rotation;
	private int maxPoolSize; 
	private int maxThreadsNum;
	private Size previewSize;
	private int  savedPoolSize = 0;
	public int getMaxPoolSize(){
		return maxPoolSize;
	}
	public int getMaxThreadsNum(){
		return maxThreadsNum;
	}
	private void init(Context context){
		this.context = context;
		this.camera2View = new Camera2Preview(context);
		this.camera2View.setCamera2TextureViewCallback(this);
		this.canvasView = new CanvasTextureView(context);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT );
		addView(camera2View,params);
		addView(canvasView,params);
		balancer = new LoadBalancer();
	}
	public void reloadProcessingSetup(Size newProcessingSize) throws CameraAccessException{
		camera2View.reloadProcessingSetup(newProcessingSize);
	}
	public Camera2DrawerPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}
	public Camera2DrawerPreview(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	public Camera2DrawerPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public Camera2DrawerPreview(Context context) {
		super(context);
		init(context);
	}
	
	public void setCamera2ProcessingCallback(Camera2ProcessingCallback processingCallback){
		if(this.processingCallback != null){
			if(processingCallback != null){
				processingCallback.init(context);
			}
			this.processingCallback.destroy();
		}
		this.processingCallback = processingCallback;
		
		
	}
	public void startPreview(Camera2SetupCallback callback) throws InterruptedException, InvalidStateException{
		if(processingCallback != null){
			processingCallback.init(context);
		}
		this.setupCallback = callback;
		if(savedPoolSize != 0){
			balancer.setMaxPoolSize(savedPoolSize);
		}
		balancer.startWorking();
		this.canvasView.init(this);
		this.camera2View.startPreview();
	}
	public void stopPreview() throws InvalidStateException{
		camera2View.stopPreview();
		try {
			//balancer.setMaxPoolSize(0);
			balancer.stopWorking();
		} catch (InterruptedException e) {}
		if(processingCallback != null){
			processingCallback.destroy();
		}
	}
	public void setMaxPoolSize(int size) throws InvalidStateException{
		balancer.setMaxPoolSize(size);
		savedPoolSize = size;
	}
	public void setMaxThreadsNum(int size) throws InvalidStateException{
		balancer.setMaxThreadsNum(size);
	}
	public void setSurfaceTextureListener(SurfaceTextureListener listener){
		camera2View.setSurfaceTextureListener(listener);
	}
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		for(int i=0;i<count;i++){
			View child = getChildAt(i);
			child.layout(l, t, r, b);
		}
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

	@Override
	public Size setCameraPreviewSize(int viewWidth, int viewHeight, Size[] sizes) {
		previewSize = setupCallback.setCameraPreviewSize(viewWidth, viewHeight, sizes);
		return previewSize;
	}

	@Override
	public void processImage(final Mat mat, final int w, final int h, final int rotation) {
		this.rotation = rotation;
		LoadBalancer.Task task = new Task() {
			@Override
			public void work() {
				if(processingCallback != null){
					processingCallback.processImage(mat, w, h, canvasView.getWidth(), canvasView.getHeight(), rotation);
				}
				canvasView.requestRedraw();
			}
		};
		try{
			balancer.setNextTaskIfEmpty(task);
		}catch(IndexOutOfBoundsException e){
			Log.e("processImage", "IndexOutOfBoundsException "+e.getMessage());
		}catch (NullPointerException e) {
			Log.e("processImage", "NullPointerException");
		}
	}

	@Override
	public void onError(int errNum) {
		setupCallback.onError(errNum);
	}

	@Override
	public void redraw(Canvas canvas) {
		int w, h;
		
		if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
			w = getWidth();
			h = getHeight();
		}else{
			w = getHeight();
			h = getWidth();
		}
		if(processingCallback != null){
			processingCallback.redraw(canvas, w, h);
		}
		
	}
	@Override
	public CaptureRequest.Builder setupCamera(CameraDevice cameraDevice) {
		return setupCallback.setupCamera(cameraDevice);
	}
	@Override
	public void onCameraOpen(CameraCharacteristics characteristics) {
		setupCallback.onCameraOpen(characteristics);
	}
	@Override
	public Size setCameraProcessingSize(int viewWidth, int viewHeight, Size[] sizes) {
		if(processingCallback != null){
			return processingCallback.setCameraProcessingSize(viewWidth, viewHeight, sizes);
		}else{
			return previewSize;
		}
	}

}
