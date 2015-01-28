package com.rychu.tagtracker.fragments;

import com.rychu.tagtracker.R;
import com.rychu.tagtracker.camera.CanvasTextureView;
import com.rychu.tagtracker.camera.CanvasTextureView.RedrawingCallback;
import com.rychu.tagtracker.touch.ControlsHelper;
import com.rychu.tagtracker.touch.ControlsHelper.ControlsCallback;
import com.rychu.tagtracker.usb.UsbRobotController;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ControllerFragment extends Fragment implements Runnable, RedrawingCallback, ControlsCallback{
	private static final String TAG = ControllerFragment.class.getSimpleName();
	private CanvasTextureView canvasTextureView;
	private Boolean work = true;
	private ControlsHelper helper;
	private Thread worker;
	private UsbRobotController usbRobotController;
	private Boolean usbConnect = false;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_driver_controller, container, false);
		canvasTextureView = (CanvasTextureView) rootView.findViewById(R.id.controller_drawerView);
		canvasTextureView.init(this);
		usbRobotController = new UsbRobotController(getActivity());
		helper = new ControlsHelper(canvasTextureView);
		helper.setControlsCallback(this);
		return rootView;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		usbConnect = false;
		Intent intent = getActivity().getIntent();
		int chosenFragment = intent.getIntExtra("chosen_fragment", 0);
		if(!(chosenFragment > 0)){
			usbConnect = true;
		}
		super.onCreate(savedInstanceState);
	}
	@Override
	public void onResume() {
		worker = new Thread(this);
		worker.start();
		if(usbConnect){
			usbRobotController.startWorking();
		}
		super.onResume();
	}
	@Override
	public void onPause() {
		work = false;
		try {
			worker.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(usbConnect){
			try {
				usbRobotController.stopWorking();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		super.onPause();
	}
	@Override
	public void run() {
		while(work){
			canvasTextureView.requestRedraw();
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {}
		}
		// TODO Auto-generated method stub
		
	}
	@Override
	public void redraw(Canvas canvas) {
		if(usbRobotController != null){
			Paint paint = new Paint();
			paint.setTextSize(40.0f);
			paint.setColor(Color.BLUE);
			canvas.drawText(usbRobotController.buffer, 50, 50, paint);
		}
		helper.drawControls(canvas);
	}
	@Override
	public void getPivotPosition(float procX, float procY) {
		Log.v(TAG, "procX"+procX+";procY"+procY);
		usbRobotController.steer(procX, procY);
	}
}
