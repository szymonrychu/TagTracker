package com.rychu.tagtracker.opencv;

import java.util.ArrayList;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import android.graphics.Bitmap;
import android.util.Log;


public class Misc {
	private static boolean libsLoaded = false;
	public static void loadLibs(){
		if(!Misc.libsLoaded){
			if (!OpenCVLoader.initDebug()) {
				Log.e("OpenCv", "couldn't load libs!");
		        // Handle initialization error
		    } else {
	        	System.loadLibrary("misc");
	        	System.loadLibrary("recognizer");
	        	System.loadLibrary("calibrate");
	        	System.loadLibrary("detector");
	        	Misc.libsLoaded = true;
		    }
		}
		
	}
	private static native Object yuvToRgbNtv(long yuvAddr, int rotation);
	public static Mat yuv2Rgb(Mat yuvFrame, int rotation){
		return (Mat)yuvToRgbNtv(yuvFrame.getNativeObjAddr(),rotation);
	}
	public static Bitmap mat2Bitmap(Mat src){
		Bitmap bmp = null;
		try {
		    bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
		    Utils.matToBitmap(src, bmp);
		}
		catch (CvException e){
			Log.d("Exception",e.getMessage());
		}
		return bmp;
	}
}
