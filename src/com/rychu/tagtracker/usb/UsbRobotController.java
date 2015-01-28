package com.rychu.tagtracker.usb;

import com.rychu.tagtracker.processing.OfflineStore;

import android.content.Context;
import android.util.Log;

public class UsbRobotController extends UsbConnection {
	private final static String TAG = UsbRobotController.class.getSimpleName();
	private final static int MAX_BYTE = 255;
	private final static String KEY_X_CENTER = "x_center";
	private final static String KEY_Z_CENTER = "z_center";
	private final static String KEY_X_SPACE = "x_center";
	private final static String KEY_Z_SPACE = "z_center";
	private int xCenter;
	private int zCenter;
	private int xSpace;
	private int zSpace;
	private OfflineStore offlineStore;
	
	public UsbRobotController(Context context) {
		super(context);
		offlineStore = new OfflineStore(context);
		xCenter = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_X_CENTER, 177);
		zCenter = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_X_CENTER, 177);
		xSpace = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_X_CENTER, 70);
		zSpace = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_X_CENTER, 70);
		Log.v(TAG, "xCenter="+xCenter);
		Log.v(TAG, "zCenter="+zCenter);
		Log.v(TAG, "xSpace="+xSpace);
		Log.v(TAG, "zSpace="+zSpace);
	}
	private int standarize(int center, int space, float value){
		float scale = space;
		float normalized = scale*value;
		return (int)(normalized + center);
	}
	public void steer(float x, float y, float z){
		int data[] = new int[UsbConnection.MAX_PWM_CHANNELS];
		int XX = standarize(xCenter, xSpace, x);
		int YY = Math.abs((int)(MAX_BYTE*y));
		int ZZ = standarize(zCenter, zSpace, z);
		data[0]=XX;
		data[1]=ZZ;
		if(y < 0){
			data[2]=0;
			data[3] = YY;
			data[4]=0;
			data[5] = YY;
		}else{
			data[2] = YY;
			data[3]=0;
			data[4] = YY;
			data[5]=0;
		}
		alterBuffer(data);
	}
	public void steer(float x, float y){
		steer(x,y, 0.0f);
	}
}
