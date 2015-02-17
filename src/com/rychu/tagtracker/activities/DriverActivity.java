package com.rychu.tagtracker.activities;

import java.io.IOException;

import com.rychu.tagtracker.R;
import com.rychu.tagtracker.activities.util.FullScreenActivity;
import com.rychu.tagtracker.fragments.CalibrateFragment;
import com.rychu.tagtracker.fragments.ControllerFragment;
import com.rychu.tagtracker.fragments.RecognizerFragment;
import com.rychu.tagtracker.opencv.Misc;
import com.rychu.tagtracker.processing.OfflineStore;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.os.Build;

public class DriverActivity extends FullScreenActivity {
	static {
		Misc.loadLibs();
	}
	public final static String DESIRED_FRAGMENT_KEY = "chosen_fragment";
	private final static String TAG = DriverActivity.class.getSimpleName();
	public final static int FRAGMENT_CONTROLLER = 0x01;
	public final static int FRAGMENT_RECOGNIZER = 0x02;
	public final static int FRAGMENT_CALIBRATE = 0x03;
	private final static int MENU_CALIBRATE = 0;
	private FragmentManager fragmentManager;
	private OfflineStore offlineStore;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		offlineStore = new OfflineStore(this);
		fragmentManager = getFragmentManager();
		setContentView(R.layout.activity_driver);
		Intent intent = getIntent();
		Integer chosenFragment = intent.getIntExtra("chosen_fragment", 0);
		if(chosenFragment == 0){
			chosenFragment = (Integer) offlineStore.loadData(OfflineStore.TYPE_EXTERNAL, DESIRED_FRAGMENT_KEY, 0);
			if(chosenFragment == 0){
				chosenFragment = FRAGMENT_RECOGNIZER;
			}
		}
		switch(chosenFragment){
		case FRAGMENT_CONTROLLER:
			ControllerFragment controllerFragment = new ControllerFragment();
			setFragment(controllerFragment, FRAGMENT_CONTROLLER);
			break;
		case FRAGMENT_RECOGNIZER:
			RecognizerFragment recognizerFragment = new RecognizerFragment();
			setFragment(recognizerFragment, FRAGMENT_RECOGNIZER);
			break;
		case FRAGMENT_CALIBRATE:
			CalibrateFragment calibrateFragment = new CalibrateFragment();
			setFragment(calibrateFragment, FRAGMENT_CALIBRATE);
			break;
		}
		
		super.onCreate(savedInstanceState);
	}
	private void setFragment(Fragment fragment, Integer previousFragment){
		try {
			offlineStore.saveData(OfflineStore.TYPE_EXTERNAL, DESIRED_FRAGMENT_KEY, previousFragment);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fragmentManager.beginTransaction().add(R.id.container, fragment).commit();
	}
	private void switchFragment(Fragment fragment){
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.layout.fragment_driver_calibrate, fragment);
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_CALIBRATE, 0, "calibrate servos"); //getResources().getString(R.string.mess_1);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch(id){
		case MENU_CALIBRATE:
			switchFragment(new CalibrateFragment());
			return true;
		default:
			return false;
		}
	}




	@Override
	protected void onSystemBarsVisible() {
		// TODO Auto-generated method stub
		
	}




	@Override
	protected void onSystemBarsHided() {
		// TODO Auto-generated method stub
		
	}

	
}
