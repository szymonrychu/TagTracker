package com.rychu.tagtracker.camera;

import org.opencv.core.Mat;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

public class Camera2Preview extends Camera2TextureView{
	public interface Camera2TextureViewCallback{
		Size setCameraPreviewSize(int viewWidth, int viewHeight, Size[] sizes);
		Size setCameraProcessingSize(int viewWidth, int viewHeight, Size[] sizes);
		void processImage(final Mat mat, final int w, final int h, final int rotation);
		void onError(int errNum);
		CaptureRequest.Builder setupCamera(CameraDevice cameraDevice);
		void onCameraOpen(CameraCharacteristics characteristics);
	}
	private Camera2TextureViewCallback callback;
	public void setCamera2TextureViewCallback(Camera2TextureViewCallback callback){
		this.callback = callback;
	}
	private Context context;
	private void init(Context context){
		this.context = context;
	}
	public Camera2Preview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}
	public Camera2Preview(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	public Camera2Preview(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public Camera2Preview(Context context) {
		super(context);
		init(context);
	}

	@Override
	public CaptureRequest.Builder setupCamera(CameraDevice cameraDevice) {
		return callback.setupCamera(cameraDevice);
	}

	@Override
	public Matrix configureTransform(int width, int height, Size outputSize, int rotation) {
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, getWidth(), getHeight());
        RectF bufferRect = new RectF(0, 0, outputSize.getHeight(), outputSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) getHeight() / outputSize.getHeight(),
                    (float) getWidth() / outputSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
		return matrix;
	}

	@Override
	public void processImage(Mat mat, int w, int h, int rotation) {
		callback.processImage(mat, w, h, rotation);
	}

	@Override
	public void onCameraError(int error) {
		callback.onError(error);
		
	}

	@Override
	public void onCameraOpen(CameraCharacteristics characteristics) {
		callback.onCameraOpen(characteristics);
	}

	@Override
	public Size setCameraPreviewSize(int viewWidth, int viewHeight, Size[] sizes) {
		return callback.setCameraPreviewSize(viewWidth, viewHeight, sizes);
	}
	@Override
	public Size setCameraProcessingSize(int viewWidth, int viewHeight, Size[] sizes) {
		return callback.setCameraProcessingSize(viewWidth, viewHeight, sizes);
	}

}
