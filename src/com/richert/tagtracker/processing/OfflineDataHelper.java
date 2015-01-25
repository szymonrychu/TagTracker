package com.richert.tagtracker.processing;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import com.richert.tagtracker.elements.PerformanceTester;
import com.richert.tagtracker.elements.PerformanceTester.Holder;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

public class OfflineDataHelper {
	private final static String TAG=OfflineDataHelper.class.getSimpleName();
	private final static String SHARED_PREFERENCES_NAME="camera_preferences";
	private final static String PREFERENCE_RESOLUTION_WIDTH="resolution_width";
	private final static String PREFERENCE_RESOLUTION_HEIGHT="resolution_height";
	private final static String CAMERA_MATRIX="camera_matrix";
	private final static String DISTORTION_MATRIX="distortion_matrix";
	private final static String PREFERED_ACTIVITY="preferenced_activity";
	private final static String WRITABLE_DIRECTORY="/tracker_screenshots/";
	private final static String DELIM=":";
	private final static String ROW_DELIM=";";
	private SharedPreferences preferences;
	public String loadStringPreference(String key, String defaultValue){
		return preferences.getString(key, defaultValue);
	}
	public int loadIntPreference(String key, int defaultValue){
		return preferences.getInt(key, defaultValue);
	}
	public float loadFloatPreference(String key, float defaultValue){
		return preferences.getFloat(key, defaultValue);
	}
	public boolean loadBoolPreference(String key, boolean defaultValue){
		return preferences.getBoolean(key, defaultValue);
	}
	public Size loadSizePreference(String key, Size defaultValue){
		Set<String> valueList = new HashSet<String>();
		String w = "w"+DELIM+defaultValue.getWidth();
		String h = "h"+DELIM+defaultValue.getHeight();
		valueList.add(w);
		valueList.add(h);
		int W = -1;
		int H = -1;
		try{
			Set<String> result = preferences.getStringSet(key, valueList);
			if(result.size() != 2){
				return defaultValue;
			}
			for(String string : result){
				if(string.startsWith("w"+DELIM)){
					try{
						W = Integer.parseInt(string.substring(2));
					}catch(NumberFormatException e){
						return defaultValue;
					}
					
				}else if(string.startsWith("h"+DELIM)){
					try{
						H = Integer.parseInt(string.substring(2));
					}catch(NumberFormatException e){
						return defaultValue;
					}
				}
			}
		}catch(ClassCastException e){
			
		}
		
		
		
		if(W < 0 && H < 0){
			return defaultValue;
		}else{
			return new Size(W, H);
		}
	}
	public void saveSizePreference(String key, Size value){
		Set<String> valueList = new HashSet<String>();
		valueList.add("w"+DELIM+value.getWidth());
		valueList.add("h"+DELIM+value.getHeight());
		SharedPreferences.Editor editor = preferences.edit();
		editor.putStringSet(key, valueList);
		editor.commit();
	}
	public void saveStringPreference(String key, String value){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, value);
		editor.commit();
	}
	public void saveIntPreference(String key, int value){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}
	public void saveFloatPreference(String key, float value){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putFloat(key, value);
		editor.commit();
	}
	public void saveBoolPreference(String key, boolean value){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
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
	public void saveStatistics(List<Holder> holders, int coreNum){
		int num = 0;
		Boolean first = true;
		StringBuilder sb = new StringBuilder();
		for(int c=0;c<7; c++){
			sb.append("data");
			sb.append(c);
			sb.append(",");
		}
		sb.append("width,height,maxThreads,");
		sb.append("delay,drawing,dropped,frame,");
		for(int c=0; c< coreNum; c++){
			sb.append("freq");
			sb.append(c);
			sb.append(",");
		}
		sb.append("processing,threads\n");
		Holder tmp = new PerformanceTester.Holder(coreNum);
		for(int d=0; d<holders.size(); d++){
			Holder h = holders.get(d);
			if(h.maxThreads == tmp.maxThreads && h.width == tmp.width && h.height == tmp.height){
				num++;
			}else{
				try{
					num++;
					for(int c=0;c<7; c++){
						sb.append(tmp.data[c]/num);
						tmp.data[c] = 0;
						sb.append(",");
					}
					sb.append(tmp.width);
					tmp.width = 0;
					sb.append(",");
					sb.append(tmp.height);
					tmp.height = 0;
					sb.append(",");
					sb.append(tmp.maxThreads);
					tmp.maxThreads = 0;
					sb.append(",");
					sb.append(tmp.delay/num);
					tmp.delay = 0;
					sb.append(",");
					sb.append(tmp.drawing/num);
					tmp.drawing = 0;
					sb.append(",");
					sb.append(tmp.dropped/num);
					tmp.dropped = 0;
					sb.append(",");
					sb.append(tmp.frame/num);
					tmp.frame = 0;
					sb.append(",");
					for(int c=0; c< coreNum; c++){
						sb.append(tmp.frequencies[c]/num);
						tmp.frequencies[c] = 0;
						sb.append(",");
					}
					sb.append(tmp.processing/num);
					tmp.processing = 0;
					sb.append(",");
					sb.append(tmp.threads/num);
					tmp.threads = 0;
					sb.append("\n");
					num=0;
				}catch(Exception e){
					Log.e(TAG, "error!");
				}
			}
			tmp.width = h.width;
			tmp.height = h.height;
			tmp.maxThreads = h.maxThreads;
			for(int c=0;c<7; c++){
				tmp.data[c] += h.data[c];
			}
			tmp.delay += h.delay;
			tmp.drawing += h.drawing;
			tmp.dropped += h.dropped;
			tmp.frame += h.frame;
			for(int c=0; c< coreNum; c++){
				tmp.frequencies[c] += h.frequencies[c];
			}
			tmp.processing += h.processing;
			tmp.threads += h.threads;
		}
		
		
		SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss",Locale.getDefault());
		String date = s.format(new Date());
		String filename = "statistics-"+date+".csv";
		
		
		
		
		FileOutputStream out = null;
		try {
			File path = getScreenShotDir(filename);
			out = new FileOutputStream(path);
			out.write(sb.toString().getBytes());
		} catch (Exception e) {
	    	Log.e(TAG,"Couldn't save image: "+e.getMessage());
		} finally {
	       try{
	           out.close();
	       } catch(Throwable ignore) {}
		}
		
	}
	public void savePreferencedActivity(String activitySimpleName){
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREFERED_ACTIVITY, activitySimpleName);
		editor.commit();
	}
	public String loadPreferedActivity(){
		return preferences.getString(PREFERED_ACTIVITY, "");
	}
}
