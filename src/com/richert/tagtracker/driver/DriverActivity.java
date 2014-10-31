package com.richert.tagtracker.driver;

import java.io.UnsupportedEncodingException;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;

import com.richert.tagtracker.R;
import com.richert.tagtracker.R.id;
import com.richert.tagtracker.R.layout;
import com.richert.tagtracker.R.menu;
import com.richert.tagtracker.elements.ControlsHelper;
import com.richert.tagtracker.elements.FullScreenActivity;
import com.richert.tagtracker.elements.OnMultitouch;
import com.richert.tagtracker.elements.Pointer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnTouchListener;

public class DriverActivity extends FullScreenActivity implements Callback, Runnable, ControlsHelper.ControlsCallback {
	private String TAG = DriverActivity.class.getSimpleName();
	private SurfaceView driverView;
	private SurfaceHolder holder;
	private Boolean draw = false;
	private Thread refreshThread;
	private int time;;
	private int refreshMs;
	private Paint paint;
	private ControlsHelper controlsHelper;
	private DriverHelper driverHelper;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_driver);
		driverView = (SurfaceView) findViewById(R.id.driver_driverView);
		holder = driverView.getHolder();
		holder.addCallback(this);
		refreshThread = new Thread(this);
		refreshMs = 10;
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(100.0f);
		paint.setTextSize(25);
		paint.setColor(Color.RED);
		controlsHelper = new ControlsHelper(driverView);
		controlsHelper.setControlsCallback(this);
		
	}
	void drawControls(Canvas canvas){
		
	}
	@Override
	protected void onResume() {
		driverHelper = new DriverHelper(this, (UsbManager) getSystemService(Context.USB_SERVICE));
		super.onResume();
	}
	@Override
	protected void onPause() {
		if(driverHelper != null){
			driverHelper.unregisterReceiver();
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.driver, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		draw = true;
		if(refreshThread.isAlive()){
			try {
				refreshThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		refreshThread = new Thread(this);
		refreshThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		draw = false;
		try {
			refreshThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void redraw(Canvas canvas){
		controlsHelper.drawControls(canvas);
		canvas.drawText("Connected="+driverHelper.transreceive, 50, 50, paint);
	}
	@Override
	public void run() {
		while(draw){
			Canvas canvas=holder.lockCanvas();
			if(canvas!=null)
					synchronized(canvas){
					canvas.drawColor(Color.WHITE, Mode.CLEAR);
					redraw(canvas);
					holder.unlockCanvasAndPost(canvas);
			}
			try {
				Thread.sleep(refreshMs);
			} catch (InterruptedException e) {}
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
	
	@Override
	public void getPivotPosition(float procX, float procY) {
		driverHelper.steer(procX,procY);
		
		// TODO Auto-generated method stub
		
	}
}
