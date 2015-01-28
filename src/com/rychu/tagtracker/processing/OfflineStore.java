package com.rychu.tagtracker.processing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.opencv.core.Mat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class OfflineStore {
	private final static String TAG=OfflineStore.class.getSimpleName();
	private final static String SHARED_PREFERENCES_NAME="camera_preferences";
	private final static String WRITABLE_DIRECTORY="/tracker_data/";
	private final static String DELIM=":";
	private final static String ROW_DELIM=";";
	public final static int TYPE_LOCAL = 0x01;
	public final static int TYPE_EXTERNAL = 0x02;
	private SharedPreferences preferences;
	
	public OfflineStore(Context context) {
		preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME,0);
	}
	
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	public boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	private File getWritableFile(String fileName) throws IOException {
		if(isExternalStorageReadable()){
			File file = new File(Environment.getExternalStoragePublicDirectory(
		            WRITABLE_DIRECTORY), fileName);
		    file.getParentFile().mkdirs();
		    return file;
		}else{
			throw new IOException("provided path is not writable");
		}
	    
	}
	public void saveData(int type, String key, Serializable data) throws IOException, IllegalArgumentException{

		
		switch(type){
		case TYPE_LOCAL:
			ByteArrayOutputStream raw = new ByteArrayOutputStream();
			ObjectOutputStream oosL = new ObjectOutputStream(raw);
			oosL.writeObject(data);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(key, raw.toString());
			editor.commit();
			oosL.close();
			break;
		case TYPE_EXTERNAL:
			File path = getWritableFile(key);
			FileOutputStream fosE = new FileOutputStream(path);
			ObjectOutputStream oosE = new ObjectOutputStream(fosE);
			oosE.writeObject(data);
			oosE.close();
			fosE.close();
			break;
		default:
			throw new IllegalArgumentException("unsupported type!");
		}
	}
	
	public Serializable loadData(int type, String key) throws IOException, IllegalArgumentException{
		switch(type){
		case TYPE_LOCAL:
			String raw = preferences.getString(key, null);
			if(raw != null){
				byte[] data = raw.getBytes();
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
				try {
					return (Serializable) ois.readObject();
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException("class not found!");
				}
			}else{
				throw new IOException("data not found!");
			}
		case TYPE_EXTERNAL:
			try{
				File path = getWritableFile(key);
				FileInputStream fis = new FileInputStream(path);
				ObjectInputStream ois = new ObjectInputStream(fis);
				Serializable result = (Serializable) ois.readObject();
				ois.close();
				return result;
			}catch (IOException e) {
				throw new IOException("file not found!");
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("class not found!");
			}
		default:
			throw new IllegalArgumentException("unsupported type!");
		}
	}
	public Serializable loadData(int type, String key, Serializable def){
		try{
			return loadData(type, key);
		}catch(Exception e){
			return def;
		}
	}
	public Serializable Mat2Serializable(Mat mat){
		byte[] imageInBytes = new byte[(safeLongToInt(mat.total())) * mat.channels()];
	    mat.get(0, 0, imageInBytes);
	    return (Serializable) imageInBytes;
	}
	public int safeLongToInt(long l) {
	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
	        throw new IllegalArgumentException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}
}
