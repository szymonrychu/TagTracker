package com.richert.tagtracker.elements;

import java.io.File;
import java.io.FileOutputStream;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

public class OfflineDataHelper {
	static {
		if (!OpenCVLoader.initDebug()) {
	        // Handle initialization error
	    } else {
	    	
	    }
	}
	private final static String TAG=OfflineDataHelper.class.getSimpleName();
	private final static String SHARED_PREFERENCES_NAME="camera_preferences";
	private final static String PREFERENCE_RESOLUTION_WIDTH="resolution_width";
	private final static String PREFERENCE_RESOLUTION_HEIGHT="resolution_height";
	private final static String CAMERA_MATRIX="camera_matrix";
	private final static String DISTORTION_MATRIX="distortion_matrix";
	private final static String WRITABLE_DIRECTORY="/tracker_screenshots/";
	private final static String DELIM=":";
	private final static String ROW_DELIM=";";
	private SharedPreferences preferences;
	/**
	 * Constructor
	 * @param context
	 */
	public OfflineDataHelper(Context context) {
		preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,0);
	}
	/**
	 * Check if external storage is writable.
	 * @return true if storage is writable, false another way.
	 */
	protected boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	/**
	 * Check if external storage is readable.
	 * @return true if storage is readable, false another way.
	 */
	protected boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	private File getScreenShotDir(String screenShotName) {
	    // Get the directory for the user's public pictures directory. 
	    File file = new File(Environment.getExternalStoragePublicDirectory(
	            WRITABLE_DIRECTORY), screenShotName);
	    if (!file.getParentFile().mkdirs()) {
    		Log.v(TAG, "Directory not created");
	    }
	    return file;
	}
	/**
	 * Method for saving preview resolution to shared preferences.
	 * @param size -size of camera preview.
	 */
	public void setResolution(Camera.Size size){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(PREFERENCE_RESOLUTION_WIDTH, size.width);
		editor.putInt(PREFERENCE_RESOLUTION_HEIGHT, size.height);
		editor.commit();
	}
	/**
	 * Method designed to obtain width of the camera preview.
	 * @param defaultWidth -default value, in case shared preference don't exist
	 * @return width obtained from shared preferences, or passed from default value
	 */
	public int getResolutionWidth(int defaultWidth){
		return preferences.getInt(PREFERENCE_RESOLUTION_WIDTH, defaultWidth);
	}
	/**
	 * Method designed to obtain height of the camera preview.
	 * @param defaultHeight -default value, in case shared preference don't exist
	 * @return height obtained from shared preferences, or passed from default value
	 */
	public int getResolutionHeight(int defaultHeight){
		return preferences.getInt(PREFERENCE_RESOLUTION_HEIGHT, defaultHeight);
	}
	/**
	 * saves 5x1 distortion matrix
	 * @param mat
	 */
	public void saveDistortionMatrix(Mat mat){
		StringBuilder builder = new StringBuilder();
		for(int r=0;r<mat.rows();r++){
			double[] data = new double[1];
			data = mat.get(r, 0);
			builder.append(r);
			builder.append(DELIM);
			builder.append(data[0]);
			builder.append(ROW_DELIM);
		}
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(DISTORTION_MATRIX, builder.toString());
		editor.commit();
	}
	/**
	 * loads 5x1 distortion matrix
	 * @return
	 */
	public Mat loadDistortionMatrix(){
		String data = preferences.getString(DISTORTION_MATRIX, null);
		if(data != null){
			Mat result = new Mat(5,1,CvType.CV_64F);
			String[] rows = data.split(ROW_DELIM);
			for(String row : rows){
				String vals[] = row.split(DELIM);
				int r = Integer.parseInt(vals[0]);
				double val[] = { Double.parseDouble(vals[1]) };
				result.put(r, 0, val);
			}
			return result;
		}else{
			return null;
		}
	}
	/**
	 * saves 3x3 camera matrix
	 * @param mat
	 */
	public void saveCameraMatrix(Mat mat){
		StringBuilder builder = new StringBuilder();
		for(int r=0;r<mat.rows();r++){
			for(int c=0;c<mat.cols();c++){
				double[] data = new double[1];
				data = mat.get(r, c);
				builder.append(r);
				builder.append(DELIM);
				builder.append(c);
				builder.append(DELIM);
				builder.append(data[0]);
				builder.append(ROW_DELIM);
			}
		}
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(CAMERA_MATRIX, builder.toString());
		editor.commit();
	}
	/**
	 * loads 3x3 camera matrix
	 * @return
	 */
	public Mat loadCameraMatrix(){
		String data = preferences.getString(CAMERA_MATRIX, null);
		if(data != null){
			Mat result = new Mat(3,3,CvType.CV_64F);
			for(String row : data.split(ROW_DELIM)){
				String vals[] = row.split(DELIM);
				int r = Integer.parseInt(vals[0]);
				int c = Integer.parseInt(vals[1]);
				double val[] = { Double.parseDouble(vals[2]) };
				result.put(r, c, val);
			}
			return result;
		}else{
			return null;
		}
	}
	/**
	 * Method for saving screenshot of actual preview.
	 * @param bmp -bitmap with preview
	 * @param screenShotName -name of screenshot
	 */
	public void saveScreenshot(Bitmap bmp, String screenShotName){
		if(bmp==null){
		    Log.e(TAG,"Couldn't save image -err0");
			return;
		}
		if(!isExternalStorageWritable()){
		    Log.e(TAG,"Couldn't save image -err1");
			return;
		}
		FileOutputStream out = null;
		try {
			File path = getScreenShotDir(screenShotName);
			out = new FileOutputStream(path);
			bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
			bmp.recycle();
		} catch (Exception e) {
	    	Log.e(TAG,"Couldn't save image: "+e.getMessage());
		} finally {
		       try{
		           out.close();
		       } catch(Throwable ignore) {}
		}
	}
}
