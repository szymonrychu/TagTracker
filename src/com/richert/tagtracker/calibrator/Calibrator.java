package com.richert.tagtracker.calibrator;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import com.richert.tagtracker.geomerty.Point;
import com.richert.tagtracker.natUtils.Holder;

import android.graphics.Bitmap;
import android.util.Log;


public class Calibrator { 
	static {
		if (!OpenCVLoader.initDebug()) {
	        // Handle initialization error
	    } else {
        	System.loadLibrary("calibrate");
	    }
	}
	private native long newCalibratorNtv(int x, int y);
	private native void delCalibratorNtv(long ptr);
	private native void addFrameToSetNtv(long ptr);
	private native void processFramesNtv(long ptr);
	private native Object[] detectChessBoardNtv(long ptr, long yuvAddr, int rotation);
	private native Object getCameraMatrixNtv(long ptr);
	private native Object getDistortionCoefficientNtv(long ptr);
	
	long ptr = 0;
	public Calibrator() {
		ptr = newCalibratorNtv(6,8);
	}
	@Override
	protected void finalize() throws Throwable {
		delCalibratorNtv(ptr);
		super.finalize();
	}
	public Point[] detectChessBoard(Mat yuvFrame, int rotation){
		return (Point[])detectChessBoardNtv(ptr, yuvFrame.getNativeObjAddr(),rotation);
	}
	public void addFrameToSet(){
		addFrameToSetNtv(ptr);
	}
	public void processFrames(){
		processFramesNtv(ptr);
	}
	/**
	 * returns 3x3 camera Matrix 64bit double
	 * @return
	 */
	public Mat getCameraMatrix(){
		return (Mat) getCameraMatrixNtv(ptr);
	}
	/**
	 * returns 5x1 distortion matrix 64bit double
	 * @return
	 */
	public Mat getDistortionCoefficient(){
		return (Mat) getDistortionCoefficientNtv(ptr);
	}

}
