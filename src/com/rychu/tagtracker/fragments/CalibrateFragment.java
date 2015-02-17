package com.rychu.tagtracker.fragments;

import com.appyvet.rangebar.RangeBar;
import com.rychu.tagtracker.R;
import com.rychu.tagtracker.camera.CanvasTextureView;
import com.rychu.tagtracker.processing.OfflineStore;
import com.rychu.tagtracker.touch.ControlsHelper;
import com.rychu.tagtracker.usb.UsbRobotController;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class CalibrateFragment extends Fragment {
	private final static String TAG = CalibrateFragment.class.getSimpleName();
	private UsbRobotController robotController;
	private OfflineStore offlineStore;
	private RangeBar calibrateServoXRB;
	private RangeBar calibrateServoZRB;
	private TextView calibrateServoXTV;
	private TextView calibrateServoZTV;
	private int centerX, spaceX, minX, maxX, centerZ, spaceZ, minZ, maxZ;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_driver_calibrate, container, false);
		calibrateServoXRB = (RangeBar) rootView.findViewById(R.id.calibrate_servo1_rb);
		calibrateServoZRB = (RangeBar) rootView.findViewById(R.id.calibrate_servo2_rb);
		calibrateServoXTV = (TextView) rootView.findViewById(R.id.calibrate_servo1_tv);
		calibrateServoZTV = (TextView) rootView.findViewById(R.id.calibrate_servo2_tv);
		
		
		prepareControls();
		return rootView;
	}
	private void prepareControls(){
		robotController = new UsbRobotController(getActivity());
		offlineStore = new OfflineStore(getActivity());
		
		centerX = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, UsbRobotController.KEY_X_CENTER, UsbRobotController.KEY_X_CENTER_DEFAULT);
		spaceX = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, UsbRobotController.KEY_X_SPACE, UsbRobotController.KEY_X_SPACE_DEFAULT);
		minX = centerX - (spaceX / 2);
		maxX = centerX + (spaceX / 2);
		centerZ = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, UsbRobotController.KEY_Z_CENTER, UsbRobotController.KEY_Z_CENTER_DEFAULT);
		spaceZ = (Integer)offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, UsbRobotController.KEY_Z_SPACE, UsbRobotController.KEY_Z_SPACE_DEFAULT);
		minZ = centerZ - (spaceZ / 2);
		maxZ = centerZ + (spaceZ / 2);
		Log.v(TAG, "loaded: space1="+spaceX+" center1="+spaceX);
		Log.v(TAG, "loaded: spaceZ="+spaceZ+" centerZ="+spaceZ);
		Log.v(TAG, "loaded: min1="+minX+" max1="+maxX);
		Log.v(TAG, "loaded: minZ="+minZ+" maxZ="+maxZ);
		

		calibrateServoXRB.setTickStart(0);
		calibrateServoXRB.setTickEnd(255);
		calibrateServoXRB.setRangePinsByIndices(minX, maxX);
    	String servo1Range = getResources().getString(R.string.servo_X_range_string);
    	calibrateServoXTV.setText(String.format(servo1Range+"%03d-%03d", minX, maxX));
		calibrateServoXRB.setTickInterval(1);
		calibrateServoXRB.setPinRadius(70);
		calibrateServoXRB.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
			int previousMin = minX;
			int previousMax = maxX;
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                    int rightPinIndex,
                    String leftPinValue, String rightPinValue) {
            	minX = leftPinIndex;
            	maxX = rightPinIndex;
            	if(previousMax != rightPinIndex){
            		robotController.steerCalibrate(rightPinIndex, centerZ);
            		previousMax = rightPinIndex;
            	}
            	if(previousMin != leftPinIndex){
            		robotController.steerCalibrate(leftPinIndex, centerZ);
            		previousMin = leftPinIndex;
            	}
            	robotController.setXBounds(leftPinIndex, rightPinIndex);
            	String servo1Range = getResources().getString(R.string.servo_X_range_string);
            	calibrateServoXTV.setText(String.format(servo1Range+"%03d-%03d", leftPinIndex, rightPinIndex));
            }

        });

		calibrateServoZRB.setTickStart(0);
		calibrateServoZRB.setTickEnd(255);
		calibrateServoZRB.setRangePinsByIndices(minZ, maxZ);
    	String servo2Range = getResources().getString(R.string.servo_Z_range_string);
    	calibrateServoZTV.setText(String.format(servo2Range+"%03d-%03d", minZ, maxZ));
		calibrateServoZRB.setTickInterval(1);
		calibrateServoZRB.setPinRadius(70);
		calibrateServoZRB.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
			int previousMin = minZ;
			int previousMax = maxZ;
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                    int rightPinIndex,
                    String leftPinValue, String rightPinValue) {
            	minZ = leftPinIndex;
            	maxZ = rightPinIndex;
            	if(previousMax != rightPinIndex){
            		robotController.steerCalibrate(centerX, rightPinIndex);
            		previousMax = rightPinIndex;
            	}
            	if(previousMin != leftPinIndex){
            		robotController.steerCalibrate(centerX, leftPinIndex);
            		previousMin = leftPinIndex;
            	}
            	robotController.setZBounds(leftPinIndex, rightPinIndex);
            	String servo2Range = getResources().getString(R.string.servo_Z_range_string);
            	calibrateServoZTV.setText(String.format(servo2Range+"%03d-%03d", leftPinIndex, rightPinIndex));
            }

        });

	}
	@Override
	public void onResume() {
		robotController.startWorking();
		super.onResume();
	}
	@Override
	public void onPause() {
		try {
			robotController.stopWorking();
		} catch (InterruptedException e) {}
		super.onPause();
	}
}
