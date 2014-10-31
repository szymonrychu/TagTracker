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
	private native long newRecognizerNtv(int width, int height);
	private native void delRecognizerNtv(long ptr);
	private native Object[] findTagsNtv(long ptr, long yuvAddr);
	private native void notifySizeChangedNtv(long ptr, int width, int height, int rotation);
	
	
	public Recognizer(int width, int height) {
		ptr = newRecognizerNtv(width,height);
	}
	@Override
	protected void finalize() throws Throwable {
		delRecognizerNtv(ptr);
		super.finalize();
	}
	public void notifySizeChanged(Camera.Size size, int rotation){
		Log.d(TAG,"rotation="+rotation);
		notifySizeChangedNtv(ptr, size.width, size.height, rotation);
	}
	public Tag[] findTags(Mat yuvFrame, int rotation){
		return (Tag[]) findTagsNtv(ptr, yuvFrame.getNativeObjAddr());
	}
}
