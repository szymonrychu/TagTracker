package com.richert.tagtracker.elements;

import org.opencv.android.OpenCVLoader;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public abstract class FullScreenActivity extends Activity{
	private View decorView;
	private ActionBar actionBar;
	private int delayTime = 5000;
	/**
	 * Method called everytime, when system bars are visible.
	 */
	protected abstract void onSystemBarsVisible();
	/**
	 * Method called everytime, when system bars are visible.
	 */
	protected abstract void onSystemBarsHided();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		decorView = this.getWindow().getDecorView();
		if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1 ){
			actionBar = getActionBar();
			decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
				@Override
				public void onSystemUiVisibilityChange(int visibility) {
				    // Note that system bars will only be "visible" if none of the
				    // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
				    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
				    	actionBar.show();
				    	Handler h = new Handler();
				    	h.postDelayed(new Runnable() {
							@Override
							public void run() {
								hideSystemUI();
						    	actionBar.hide();
							}
						}, delayTime);
				    	onSystemBarsVisible();
				    } else {
				    	onSystemBarsHided();
				    }
				}
				});
		}
		super.onCreate(savedInstanceState);
	}
	protected void showSystemUI() {
	    decorView.setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
	}
	protected void hideSystemUI() {
	    decorView.setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
	            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
	            | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	        super.onWindowFocusChanged(hasFocus);
	    if (hasFocus) {
			if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1 ){
				hideSystemUI();
			}
	    }
	}
}
