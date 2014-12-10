package org.opencv.android.local;

import java.util.ArrayList;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import android.graphics.Bitmap;
import android.util.Log;


public class Misc {
	public static void loadLibs(){
		if (!OpenCVLoader.initDebug()) {
	        // Handle initialization error
	    } else {
        	System.loadLibrary("misc");
        	System.loadLibrary("recognize");
        	System.loadLibrary("calibrate");
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
