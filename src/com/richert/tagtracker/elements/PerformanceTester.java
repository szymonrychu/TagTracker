package com.richert.tagtracker.elements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.local.RecognizerService;

import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.richert.tagtracker.activities.RecognizeActivity;
import com.richert.tagtracker.monitor.CpuInfo;
import com.richert.tagtracker.processing.OfflineDataHelper;

public class PerformanceTester implements Runnable{
	private static final String TAG = PerformanceTester.class.getSimpleName();
	public interface TestResultCallback{
		void onTestStarted(int maxStates);
		void onTestProgress(int state);
		void onTestInterrupted();
		void onTestResult();
	}
	private Thread tester;
	private RecognizeActivity subject;
	private CameraDrawerPreview preview;
	private RecognizerService recognizer;
	private CpuInfo cpuInfo;
	private OfflineDataHelper helper;
	private long testTime = 15;
	private Boolean test = true;
	private Parameters params;
	private Boolean collectData = false;
	private int threadNum = 1;
	private int width, height, dropped, threads;
	private long drawing, processing, frame, delay;
	private int frequencies[] ;
	private float data[][];
	private List<Holder> holders;
	private Size previousSize;
	private TestResultCallback callback;
	public static class Holder{
		public Holder(int coreNum){
			width = 0;
			height = 0;
			maxThreads = 0;
			data = new float[8];
			for(int c=0;c<7; c++){
				data[c] = 0;
			}
			delay = 0;
			drawing = 0;
			dropped = 0;
			frame = 0;
			frequencies = new int[coreNum];
			for(int c=0; c< coreNum; c++){
				frequencies[c] = 0;
			}
			processing = 0;
			threads = 0;
		}
		public int width, height, dropped, threads, maxThreads;
		public long drawing, processing, frame, delay;
		public int frequencies[] ;
		public float data[];
	}
	public PerformanceTester(RecognizeActivity subject, CameraDrawerPreview preview, RecognizerService recognizer, CpuInfo cpuInfo, OfflineDataHelper helper) {
		this.subject = subject;
		this.preview = preview;
		this.recognizer = recognizer;
		this.cpuInfo = cpuInfo;
		this.helper = helper;
		width = height = 0;
		callback = null;
	}
	public void addTestResultCallback(TestResultCallback c){
		callback = c;
	}
	int previousThreads = 0;
	public void startTests(){
		test = true;
		previousThreads = preview.getMaxThreads();
		this.params = preview.getCameraParameters();
		this.previousSize = params.getPreviewSize();
		holders = new ArrayList<PerformanceTester.Holder>();
		tester = new Thread(this);
		this.tester.start();
		
	}
	public void stopTests(){
		if(callback != null){
			callback.onTestInterrupted();
		}
		try {
			test = false;
			collectData = false;
			if(this.tester != null){
				this.tester.join();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void changeResolution(Size size){
		params.setPreviewSize(size.width, size.height);
		preview.reloadCameraSetup(params);
		WindowManager manager = (WindowManager) subject.getSystemService(Context.WINDOW_SERVICE);
		Display display = (manager).getDefaultDisplay();
		int rotation = display.getRotation();
		//recognizer.notifySizeChanged(size, rotation);
	}
	private void setThreadNum(int num){
		preview.setMaxThreads(num);
	}
	private void getPreviewStatistics(){
		drawing = preview.drawingTime ;
		processing = preview.processingTime ;
		frame = preview.frameDelayTime ;
		delay = preview.oneThreadPDelayTime + preview.oneThreadDDelayTime ;
		dropped = preview.droppedFrames;
		threads = preview.threads;
	}
	private void getCPUStatistics(){
		frequencies = cpuInfo.frequency;
		data = cpuInfo.data;
	}
	public void getStatistics(){
		if(collectData){
			getPreviewStatistics();
			getCPUStatistics();
			Thread t = new Thread(){
				public void run() {
					Holder h = new Holder(cpuInfo.getNumCores());
					h.drawing = drawing;
					h.processing = processing;
					h.frame = frame;
					h.delay = delay;
					h.dropped = dropped;
					h.threads = threads;
					h.maxThreads = threadNum;
					h.frequencies = frequencies.clone();
					h.data = data[cpuInfo.getNumCores()].clone();
					h.width = width;
					h.height = height;
					if(holders != null){
						holders.add(h);
					}
				};
			};
			t.start();
		}
		
	}
	@Override
	public void run() {
		List<Size> sizes = params.getSupportedPreviewSizes();
		int maxStates = 4 * sizes.size();
		if(callback != null){
			callback.onTestStarted(maxStates);
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {}
		int counter = 0;
		for(int c=0; c<=3 && test; c++){
			threadNum = (int) Math.pow(2,c);
			if(test)setThreadNum(threadNum);
			for(Size size : sizes){
				if(test)changeResolution(size);
				width = size.width;
				height = size.height;
				if(callback != null){
					counter++;
					callback.onTestProgress(counter);
				}
				try {
					if(test)Thread.sleep(100);
					collectData = true;
					Log.v(TAG, "t="+threadNum+" w="+size.width+" h="+size.height);
					if(test)Thread.sleep(testTime*1000);
					collectData = false;
				} catch (InterruptedException e) {}
			}
		}
		helper.saveStatistics(holders, cpuInfo.getNumCores());
		try{
			changeResolution(previousSize);
		}catch(Exception e){
			Log.e(TAG, "cannot reset preview size!");
		}
		try{
			setThreadNum(previousThreads);
		}catch(Exception e){
			Log.e(TAG, "cannot reset threads num!");
		}
		if(callback != null){
			callback.onTestResult();
		}
	}
}
