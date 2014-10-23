package com.richert.tagtracker.recognizer;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import com.richert.tagtracker.geomerty.Tag;
import com.richert.tagtracker.natUtils.Holder;
import com.richert.tagtracker.natUtils.Misc;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;


public class Recognizer { 
	static {
		if (!OpenCVLoader.initDebug()) {
	        // Handle initialization error
	    } else {
        	System.loadLibrary("recognize");
	    }
	}
	private final static String TAG = Recognizer.class.getSimpleName();
	private long ptr = 0;
	private int rotation;
	private native long newRecognizerNtv(long mCamMatrixAddr,long mDistMatrixAddr, int width, int height);
	private native void delRecognizerNtv(long ptr);
	private native Object remapFrameNtv(long ptr, long yuvAddr);
	private native Object[] findTagsNtv(long ptr, long yuvAddr);
	private native void notifySizeChangedNtv(long ptr, int width, int height, int rotation);
	
	
	public Recognizer(Mat cameraMatrix, Mat distortionMatrix, int width, int height) {
		if(cameraMatrix != null && distortionMatrix != null){
			ptr = newRecognizerNtv(cameraMatrix.getNativeObjAddr(),distortionMatrix.getNativeObjAddr(),width,height);
		}
	}
	@Override
	protected void finalize() throws Throwable {
		delRecognizerNtv(ptr);
		super.finalize();
	}
	public Bitmap remapFrame(Mat yuvFrame){
		Mat remapedMat = (Mat) remapFrameNtv(ptr, yuvFrame.getNativeObjAddr());
		if(remapedMat != null){
			return Misc.mat2Bitmap(remapedMat);
		}else{
			return null;
		}
	}
	public void notifySizeChanged(Camera.Size size, int rotation){
		this.rotation = rotation;
		Log.d(TAG,"rotation="+rotation);
		notifySizeChangedNtv(ptr, size.width, size.height, rotation);
	}
	public Tag[] findTags(Mat yuvFrame, int rotation){
		return (Tag[]) findTagsNtv(ptr, yuvFrame.getNativeObjAddr());
	}
}
