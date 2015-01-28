package com.rychu.tagtracker.camera;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

public abstract class Camera2TextureView extends TextureView implements TextureView.SurfaceTextureListener{
	private static final String TAG = Camera2TextureView.class.getSimpleName();
	private Context context;
	private CameraDevice cameraDevice;
	private Semaphore cameraOpenCloseLock = new Semaphore(1);
	private HandlerThread backgroundThread;
	private Size previewSize;
	private Matrix transformMatrix;
	private CameraCaptureSession previewSession;
	private Handler backgroundHandler;
	Surface previewSurface;
	private long previousTime = 0;
	List<Surface> targets = new ArrayList<Surface>();
	private ImageReader reader;
	private  CameraCharacteristics characteristics;
	private int rotation;
	private WindowManager wManager;
	private boolean cameraOpened;
	private Size sizes[];
	private int maxImages = 2;
	public abstract CaptureRequest.Builder setupCamera(CameraDevice cameraDevice);
	public abstract Matrix configureTransform(int width, int height, Size outputSize, int rotation);
	public abstract void processImage(Mat mat, int w, int h, int r);
	public abstract void onCameraError(int error);
	public abstract void onCameraOpen(CameraCharacteristics characteristics);
	public abstract Size setCameraPreviewSize(int viewWidth, int viewHeight, Size[] sizes);
	public abstract Size setCameraProcessingSize(int viewWidth, int viewHeight, Size[] sizes);
	private StateCallback cameraStateCallback = new StateCallback() {
		@Override
		public void onOpened(CameraDevice camera) {
			cameraOpened = true;
			cameraDevice = camera;
			rotation = wManager.getDefaultDisplay().getRotation();
			setTransform(configureTransform(getWidth(),getHeight(), previewSize, rotation));
			
			openPreview();
			cameraOpenCloseLock.release();
		}
		@Override
		public void onError(CameraDevice camera, int error) {
			cameraOpened = false;
			onCameraError(error);
			cameraOpenCloseLock.release();
			cameraDevice = camera;
			if(cameraDevice != null){
				cameraDevice.close();
				cameraDevice = null;
			}
		}
		@Override
		public void onDisconnected(CameraDevice camera) {
			cameraOpened = false;
			cameraOpenCloseLock.release();
			cameraDevice = camera;
			cameraDevice.close();
			cameraDevice = null;

        	
		}
	};
	ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try{
            	synchronized(reader.getSurface()){
            		image = reader.acquireLatestImage();
                	ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                	long tNow = System.currentTimeMillis();
                	//Log.v(TAG, "preview fps: "+1000.0f/(tNow - previousTime));
                	previousTime = tNow;
                	int h = image.getHeight();
                	int w = image.getWidth();
                	Mat mat = new Mat(h+h/2, w, CvType.CV_8UC1);
                	mat.put(0, 0, bytes);
                	processImage(mat, w, h, rotation);
                    if (image != null) {
                        image.close();
                    }
            	}
            	
            }catch(Exception e){}
            
        }
    };
    private CaptureCallback captureCallback = new CaptureCallback() {
		@Override
		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
				TotalCaptureResult result) {
			/*if(cameraOpened){
				updatePreview();
			}*/
			super.onCaptureCompleted(session, request, result);
		}
	};
	private void openCamera(int width, int height){
		CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
		assert transformMatrix != null;
		try{
			if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)){
				throw new RuntimeException("Couldn't open camera!");
			}
			String cameraID = manager.getCameraIdList()[0];
			
			characteristics = manager.getCameraCharacteristics(cameraID);
			onCameraOpen(characteristics);
			StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			sizes = map.getOutputSizes(SurfaceTexture.class);
			previewSize = setCameraPreviewSize(width, height, sizes);
			
			//setPreviewSize
			int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setMeasuredDimension(previewSize.getWidth(), previewSize.getHeight());
            } else {
                setMeasuredDimension(previewSize.getHeight(), previewSize.getWidth());
            }
			rotation = wManager.getDefaultDisplay().getRotation();
			setTransform(configureTransform(getWidth(),getHeight(), previewSize, rotation));
            //setMatrix
            manager.openCamera(cameraID, cameraStateCallback, null);
		} catch (CameraAccessException e) {
			Log.e("openCamera(int width, int height)", "Cannot access the camera. CameraAccessException");
        //} catch (NullPointerException e) {
		//	Log.e("openCamera(int width, int height)", "Cannot access the camera. NullPointerException");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
	}
	private void closeCamera() {
		cameraOpened = false;
		try {
			previewSession.abortCaptures();
		} catch (CameraAccessException e1) {} catch (NullPointerException e){}
        try {
            cameraOpenCloseLock.acquire();
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
             cameraOpenCloseLock.release();
        }
    }
	private void openPreview(){
		if(null == cameraDevice || !isAvailable() || null == previewSize  ){
			return; 
		}
		try{
			SurfaceTexture texture = getSurfaceTexture();
			assert texture != null;
			texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
			previewSurface = new Surface(texture);
			Size processingSize = setCameraProcessingSize(previewSize.getWidth(), previewSize.getHeight(), sizes);
	        reader = ImageReader.newInstance(processingSize.getWidth(), processingSize.getHeight(), ImageFormat.YUV_420_888, maxImages);
	        HandlerThread thread = new HandlerThread("CameraPicture");
	        thread.start();
	        final Handler backgroundHandler = new Handler(thread.getLooper());
	        reader.setOnImageAvailableListener(readerListener, backgroundHandler);
	        

	        targets.add(previewSurface);
	        targets.add(reader.getSurface());
			
			cameraDevice.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {

				@Override
				public void onConfigureFailed(CameraCaptureSession session) {
					onCameraError(5); //TODO important! errornum!
				}

				@Override
				public void onConfigured(CameraCaptureSession session) {
					previewSession = session;
					updatePreview();
				}
				
			}, backgroundHandler);
		} catch (CameraAccessException e) {
            e.printStackTrace();
        } 
	}
    public void reloadProcessingSetup(Size newProcessingSize) throws CameraAccessException{
    	targets.remove(reader.getSurface());
    	reader = ImageReader.newInstance(newProcessingSize.getWidth(), newProcessingSize.getHeight(), ImageFormat.YUV_420_888, maxImages);
    	targets.add(reader.getSurface());
    	reader.setOnImageAvailableListener(readerListener, backgroundHandler);
    	cameraDevice.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {

			@Override
			public void onConfigureFailed(CameraCaptureSession session) {
				onCameraError(5); //TODO important! errornum!
			}

			@Override
			public void onConfigured(CameraCaptureSession session) {
				previewSession = session;
				updatePreview();
			}
			
		}, backgroundHandler);
    }
	public void updatePreview(){
		assert cameraDevice != null;
		try{
			CaptureRequest.Builder previewBuilder = setupCamera(cameraDevice);
			for(Surface s : targets){
				previewBuilder.addTarget(s);
			}
			if(cameraOpened){
				previewSession.setRepeatingRequest(previewBuilder.build(), captureCallback, backgroundHandler);
			}
			
		}catch(CameraAccessException e){}
	}
	private void init(Context context){
		this.context = context;
		wManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
	}
	public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		this.init(context);
	}
	public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.init(context);
	}
	public Camera2TextureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.init(context);
	}
	public Camera2TextureView(Context context) {
		super(context);
		this.init(context);
	}
	public void startPreview(){
		if(cameraDevice == null){
			backgroundThread = new HandlerThread("Camera background");
			backgroundThread.start();
			backgroundHandler = new Handler(backgroundThread.getLooper());
			if(isAvailable()){
				openCamera(getWidth(), getHeight());
			}else{
				setSurfaceTextureListener(this);
			}
		}
	}
	public void stopPreview() {
        closeCamera();
        if(backgroundThread != null){
        	backgroundThread.quitSafely();
            try {
            	backgroundThread.join();
            	backgroundThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
		
    }
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		startPreview();
	}
	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		stopPreview();
		return true;
	}
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		WindowManager wManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		rotation = wManager.getDefaultDisplay().getRotation();
		if(previewSize != null){
			
			setTransform(configureTransform(getWidth(),getHeight(), previewSize, rotation));
		}
	}
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		
	}
	
}
