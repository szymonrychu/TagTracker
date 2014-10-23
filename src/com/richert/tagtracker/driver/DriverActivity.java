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
		
		driverHelper = new DriverHelper(this, (UsbManager) getSystemService(Context.USB_SERVICE));
	}
	void drawControls(Canvas canvas){
		
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
		canvas.drawText("X="+X, 50, 50, paint);
		canvas.drawText("Y="+Y, 50, 100, paint);
		canvas.drawText("Connected="+driverHelper.transreceive, 50, 150, paint);
		canvas.drawText("sent="+sent, 50, 200, paint);
		/*
		SparseArray<Pointer> pointers = listener.getPoints();
		for(int c=0;c<pointers.size();c++){
			Pointer p = pointers.valueAt(c);
			float color[] = {(360*(float)p.id/5)%360, 1.0f, 1.0f};
			paint.setColor(Color.HSVToColor(color));
			canvas.drawPoint(p.x, p.y, paint);
		}*/
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
	float X=0;
	float Y=0;
	String sent="";
	int steerMax = 240;
	int steerMin = 115;
	
	@Override
	public void getPivotPosition(float procX, float procY) {
		X = procX;
		Y = procY;
		int steer, lFront, lBack, rFront, rBack;
		int steerCenter = (steerMax - steerMin)/2 + steerMin;
		steer = steerCenter - (int)(procX*((steerMax - steerMin)/2));

		lBack = procY > 0 ? procX > 0 ? (int)(procY*255) : Math.max((int)(procY*255)+(int)(procX*50),0) : 0;//leftT
		lFront = procY < 0 ? procX > 0 ? -(int)(procY*255) : Math.max(-(int)(procY*255)+(int)(procX*50),0) : 0;//leftP
		rBack = procY > 0 ? procX < 0 ? (int)(procY*255) : Math.max((int)(procY*255)-(int)(procX*50),0) : 0;//rightT
		rFront = procY < 0 ? procX < 0 ? -(int)(procY*255) : Math.max(-(int)(procY*255)-(int)(procX*50),0) : 0;//rightP
		/*
		lBack = procY < 0 ? -(int)(procY*255) : 0;
		rFront = procY > 0 ? (int)(procY*255) : 0;
		rBack = procY < 0 ? -(int)(procY*255) : 0;*/
		
		
		
		sent = ""+steer+","+lFront+","+lBack+","+rFront+","+rBack+",";
		driverHelper.send(sent.getBytes());
		
		
		// TODO Auto-generated method stub
		
	}
}
