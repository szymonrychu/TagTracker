package com.rychu.tagtracker.usb;

import java.io.IOException;

import com.rychu.tagtracker.processing.OfflineStore;

import android.content.Context;
import android.util.Log;

public class UsbRobotController extends UsbConnection {
	private final static String TAG = UsbRobotController.class.getSimpleName();
	private final static int MAX_BYTE = 255;
	public final static String KEY_X_CENTER = "x_center";
	public final static String KEY_Z_CENTER = "z_center";
	public final static String KEY_X_SPACE = "x_space";
	public final static String KEY_Z_SPACE = "z_space";
	public final static int KEY_X_CENTER_DEFAULT = 177;
	public final static int KEY_Z_CENTER_DEFAULT = 177;
	public final static int KEY_X_SPACE_DEFAULT = 70;
	public final static int KEY_Z_SPACE_DEFAULT = 70;
	private static float WHEEL_BASE_M = 0.15f; //rozstaw osi
	private static float AXIS_WIDTH_M = 0.10f; //szerokość osi
	private static float MAX_WHEEL_TURN_ANGLE_D = 40; //maksymalny kąt skrętu w stopniach
	private static float MAX_WHEEL_TURN_ANGLE_R = 0.69f; //maksymalny kąt skrętu w radianach
	private int xCenter;
	private int zCenter;
	private int xSpace;
	private int zSpace;
	private OfflineStore offlineStore;
	
	public UsbRobotController(Context context) {
		super(context);
		offlineStore = new OfflineStore(context);
		xCenter = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_X_CENTER, KEY_X_CENTER_DEFAULT);
		zCenter = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_Z_CENTER, KEY_Z_CENTER_DEFAULT);
		xSpace = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_X_SPACE, KEY_X_SPACE_DEFAULT);
		zSpace = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, KEY_Z_SPACE, KEY_Z_SPACE_DEFAULT);
	}
	private int standarize(int center, int space, float value){
		float scale = space;
		float normalized = scale*value/2;
		return (int)(normalized + center);
	}
	public void steerCalibrate(int srv1, int srv2){
		int data[] = new int[UsbConnection.MAX_PWM_CHANNELS];
		data[0]=srv1;
		data[1]=srv2;
		data[2]=0;
		data[3]=0;
		data[4]=0;
		data[5]=0;
		alterBuffer(data);
	}
	private float differRatioL2perL1(float angle){
		float A = angle * MAX_WHEEL_TURN_ANGLE_R;
		if(A > 0){
			
		}else{
			A = -A;
			
		}
		double result = 1+ AXIS_WIDTH_M/(WHEEL_BASE_M/Math.tan(A) - AXIS_WIDTH_M/2);
		
		return (float)result;
	}
	public void steer(float x, float y, float z){
		int data[] = new int[UsbConnection.MAX_PWM_CHANNELS];
		int XX = standarize(xCenter, xSpace, x);
		int ZZ = standarize(zCenter, zSpace, z);

		double left = 0.0;
		double right = 0.0;
		
		if(x>0){
			right = 1 + WHEEL_BASE_M/(AXIS_WIDTH_M/Math.tan(MAX_WHEEL_TURN_ANGLE_R*x)-AXIS_WIDTH_M/2);
			left = 1/right;
		}else{
			left = 1 + WHEEL_BASE_M/(AXIS_WIDTH_M/Math.tan(MAX_WHEEL_TURN_ANGLE_R*-x)-AXIS_WIDTH_M/2);
			right = 1/left;
		}
		double divider = right > left ? right : left;
		
		int YY1 = (int)(y*right*MAX_BYTE/divider);
		int YY2 = (int)(y*left *MAX_BYTE/divider);
		data[0]=XX;
		data[1]=ZZ;
		if(y < 0){
			data[2]=0;
			data[3] = -YY1;
			data[4]=0;
			data[5] = -YY2;
		}else{
			data[2] = YY1;
			data[3]=0;
			data[4] = YY2;
			data[5]=0;
		}
		alterBuffer(data);
	}
	public void steer(float x, float y){
		steer(x,y, 0.0f);
	}
	public void setZBounds(int start, int stop){
		Log.v(TAG, "start="+start);
		Log.v(TAG, "stop="+stop);
		zSpace = stop - start;
		zCenter = zSpace/2 + start;
		try {
			offlineStore.saveData(OfflineStore.TYPE_EXTERNAL, KEY_Z_CENTER, zCenter);
			offlineStore.saveData(OfflineStore.TYPE_EXTERNAL, KEY_Z_SPACE, zSpace);
		} catch (IllegalArgumentException e) {} catch (IOException e) {}
	}
	public void setXBounds(int start, int stop){
		xSpace = stop - start;
		xCenter = xSpace/2 + start;
		try {
			offlineStore.saveData(OfflineStore.TYPE_EXTERNAL, KEY_X_CENTER, xCenter);
			offlineStore.saveData(OfflineStore.TYPE_EXTERNAL, KEY_X_SPACE, xSpace);
		} catch (IllegalArgumentException e) {} catch (IOException e) {}

	}
}
