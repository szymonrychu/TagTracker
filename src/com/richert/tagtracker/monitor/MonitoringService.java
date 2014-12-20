package com.richert.tagtracker.monitor;

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MonitoringService extends Service implements Runnable {
	private static final String TAG = MonitoringService.class.getSimpleName();
	private final static int WINDOW_DEFAULT = 400;
	private final static int DELAY_DEFAULT = 250;
	private CpuInfo cpuInfo;
	private Context context;
	private Thread collector;
	private boolean work;
	private long delay;
	private long queueWaitingTime = 0;
	private long queueWaitingProcessingTime = 0;
	private MonitoringBinder monitoringBinder;
	
	private static class Holder{
		public String key;
		protected final static int TYPE_INT = 0;
		protected final static int TYPE_LONG = 1;
		protected final static int TYPE_DOUBLE = 2;
		public Holder(String k, int v) {
			key = k; valI = v; type = TYPE_INT;
		}
		public Holder(String k, long v) {
			key = k; valL = v; type = TYPE_LONG;
		}
		public Holder(String k, double v) {
			key = k; valD = v; type = TYPE_DOUBLE;
		}
		int type;
		int valI;
		long valL;
		double valD;
	}
	public interface MonitoringCallback{
		public void setWaitingTime(long time);
		public void setWaitingProcessingTime(long time);
		
		
	}
	public class MonitoringBinder extends Binder implements MonitoringCallback{
		
		
		public void startCollecting(Context context, int window, long delay){
			startCollector(context, window, delay);
		}
		public void stopCollecting(){
			stopCollector();
		}
		public float drawData(float x, float y, Canvas canvas, Paint paint){
			
			float yPos = y;
			try{
				float textSize = paint.getTextSize();
				canvas.drawText("waiting delay = " + queueWaitingTime, x, yPos+=textSize, paint);
				canvas.drawText("total delay = " + queueWaitingProcessingTime, x, yPos+=textSize, paint);
				for(int cpu=0; cpu<4;cpu++){
					canvas.drawText("cpu"+cpu+":"+cpuInfo.getCpuUsage(cpu) + queueWaitingProcessingTime, x, yPos+=textSize, paint);
				}
				cpuInfo.drawCpuUsage(canvas, x, yPos, x+1000, yPos+400);
			}catch(NullPointerException e){}
			
			
			
			return yPos;
			
		}
		@Override
		public void setWaitingTime(long time) {
			queueWaitingTime = time;
		}
		@Override
		public void setWaitingProcessingTime(long time) {
			queueWaitingProcessingTime = time;
		}
	}
	public void startCollector(Context context, int window, long delay){
		cpuInfo = new CpuInfo(window);
		this.delay = delay;
		work = true;
		cpuInfo.startReading();
		collector = new Thread(this);
		collector.start();
	}
	public void stopCollector(){
		work = false;
		try {
			collector.join();
		} catch (InterruptedException e) {}
		cpuInfo.stopReading();
	}
	
	@Override
	public void run() {
		while(work){
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {}
			
			cpuInfo.parseCPUInfo();
		}
		// TODO Auto-generated method stub
		
	}
	@Override
	public IBinder onBind(Intent intent) {
		monitoringBinder = new MonitoringBinder();
		return monitoringBinder;
	}
	
}