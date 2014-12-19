package org.opencv.android.local;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import com.richert.tagtracker.processing.LoadBalancer;
import com.richert.tagtracker.processing.LoadBalancer.InvalidStateException;
import com.richert.tagtracker.processing.LoadBalancer.Task;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


public class RecognizerService extends Service {
	private final static String TAG = RecognizerService.class.getSimpleName();
	private long ptr = 0;
	private native long newRecognizerNtv();
	private native void delRecognizerNtv(long ptr);
	private native Object[] findTagsNtv(long ptr, long yuvAddr);
	private native void notifySizeChangedNtv(long ptr, int width, int height, int rotation);
	private native void insightNtv(long ptr, long mYuv);
	private MatProcessingBinder binder;
	private LoadBalancer loadBalancer;

	public interface ProcessingCallback{
		void post(Tag[] tagz);
	}
	public class MatProcessingBinder extends Binder {
		public MatProcessingBinder() {}
		private Mat frame;
		private ProcessingCallback post;
		int blockSize = 75;
		double adaptThresh = 7.0;
		public void setProcessingCallback(ProcessingCallback c){
			this.post = c;
		}
		public void processMat(Mat yuvFrame){
			this.frame = yuvFrame;
			LoadBalancer.Task t = new Task() {
				@Override
				public void work() {
					Tag[] tagz = (Tag[]) findTagsNtv(ptr, frame.getNativeObjAddr());
					if(post != null){
						post.post(tagz);
					}
				}
			};
			try{
				loadBalancer.setNextTaskIfEmpty(t);
			}catch(IndexOutOfBoundsException e){
				Log.v(TAG, "there was a problem adding when adding task to pool (IndexOufOfBound)!");
			}catch (NullPointerException e) {
				Log.v(TAG, "there was a problem adding when adding task to pool (NullPointer)!");
			}
		}
		public void notifySizeChanged(Camera.Size size, int rotation){
			notifySizeChangedNtv(ptr, size.width, size.height, rotation);
		}
		public void setMaxThreadsNum(int num) throws InvalidStateException{
			loadBalancer.setMaxThreadsNum(num);
		}
		public void setMaxPoolSize(int num) throws InvalidStateException{
			loadBalancer.setMaxPoolSize(num);
		}
		public void startProcessing() throws InterruptedException{
			loadBalancer.startWorking();
		}
		public void stopProcessing() throws InterruptedException{
			loadBalancer.stopWorking();
		}
		public Mat insight(Mat mYuv){
			insightNtv(ptr, mYuv.getNativeObjAddr());
			return mYuv;
		}
	}
	
	

	
	
	@Override
	public void onCreate() {
		loadBalancer = new LoadBalancer();
		ptr = newRecognizerNtv();
		super.onCreate();
	}
	@Override
	public void onDestroy() {
		delRecognizerNtv(ptr);
		super.onDestroy();
	}
	@Override
	public IBinder onBind(Intent intent) {
		binder = new MatProcessingBinder();
		return binder;
	}
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
}
