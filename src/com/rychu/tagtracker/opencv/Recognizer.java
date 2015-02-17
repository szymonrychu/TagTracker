package com.rychu.tagtracker.opencv;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import com.rychu.tagtracker.camera.Camera2DrawerPreview.Camera2ProcessingCallback;
import com.rychu.tagtracker.usb.UsbConnection;
import com.rychu.tagtracker.usb.UsbRobotController;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.util.Size;


public class Recognizer implements Camera2ProcessingCallback {
	public Recognizer() {}
	public Recognizer(UsbConnection usbConnection){
		this.usbConnection = usbConnection;
	}
	public interface TTSCallback{
		public void tagsWereFound(int[] tagIds);
		public void followedTagWasLost();
	}
	private TTSCallback ttsCallback;
	private UsbConnection usbConnection;
	private final static String TAG = Recognizer.class.getSimpleName();
	private long ptr = 0;
	private native long newRecognizerNtv();
	private native void delRecognizerNtv(long ptr);
	private native Object[] findTagsNtv(long recognizerAddr,long addrYuv, int imgW, int imgH, int prevW, int prevH, int rotation);
	private Paint paint;
	private Tag[] tagz;
	private float tagPreviewX, tagPreviewY;
	private Tag followed;
	private final static long MAX_UNFOLLOW_TIME = 5000;
	private Boolean followedTagWasFound = false;
	private long timestamp;
	private int ids[];
	private float pivotZ;
	private float closestDistance = 0.45f;
	public void setTTSCallback(TTSCallback c){
		this.ttsCallback = c;
	}
	public void setTrackedTagId(int id){
		timestamp = System.currentTimeMillis();
		followed.id = id;
	}
	@Override
	public void init(Context context) {
		ptr = newRecognizerNtv();
		paint = new Paint();
		paint.setStrokeWidth(3);
		paint.setTextSize(20);
		followed = new Tag();
		followed.id = -1;
		ids = null;
	}
	@Override
	public void processImage(Mat mat, int imgW, int imgH, int prevW, int prevH, int r) {
		tagz = (Tag[]) findTagsNtv(ptr, mat.getNativeObjAddr(), imgW, imgH, prevW, prevH, r);
	}
	@Override
	public void redraw(Canvas canvas, int w, int h) {
		tagPreviewX = tagPreviewY = 0;
		followedTagWasFound = false;
		if(tagz != null){
			float meanY = 0;
			float minY = Float.MAX_VALUE;
			float maxY = Float.MIN_VALUE;
			Paint paint = null;
			ids = new int[tagz.length];
			ArrayList<Tag> newTagz = new ArrayList<Tag>();
			int counter = 0;
			for(Tag t : tagz){
				ids[counter++]=t.id;
				if(followed.id == t.id){
					paint = new Paint(this.paint);
					paint.setColor(Color.GREEN);
					followed = t;
					followedTagWasFound = true;
				}else{
					paint = new Paint(this.paint);
					paint.setColor(Color.RED);
				}
				for(int c=0; c<4; c++){
					canvas.drawLine(t.points[c].x, t.points[c].y, t.points[(c+1)%4].x, t.points[(c+1)%4].y, paint);
					meanY += t.points[c].y;
					if(t.points[c].y > maxY){
						maxY = t.points[c].y;
					}
					if(t.points[c].y < minY){
						minY = t.points[c].y;
					}
				}
				canvas.drawText(""+t.id, t.center.x, t.center.y, paint);
				if(tagPreviewX+t.preview.cols() > w){
					tagPreviewY += t.preview.rows();
				}
				canvas.drawBitmap(Misc.mat2Bitmap(t.preview), tagPreviewX, tagPreviewY, paint);
				tagPreviewX+=t.preview.cols();
			}
			meanY = meanY/(tagz.length*4);
			
			
			
			float y = 0.0f;
			float x = 0.0f;
			paint.setColor(Color.BLUE);
			if(followed.id > 0){
				
				

				float minX = Float.MAX_VALUE;
				float maxX = Float.MIN_VALUE;
				for(Point p : followed.points){
					if(p.x > maxX){
						maxX = p.x;
					}
					if(p.x < minX){
						minX = p.x;
					}
					
				}
				float xDeltaL = followed.center.x - minX;
				float xDeltaR = maxX - followed.center.x;
				canvas.drawLine(xDeltaL, 0, xDeltaL, canvas.getHeight(), paint);
				canvas.drawLine(canvas.getWidth()-xDeltaR, 0, canvas.getWidth()-xDeltaR, canvas.getHeight(), paint);
				
				float xSpace = canvas.getWidth() - (xDeltaL + xDeltaR);
				float dist = xSpace / canvas.getWidth();
				float distance = dist * dist;
				if(distance > closestDistance){
					y = -(distance - closestDistance) / (1 - closestDistance);
				}else{
					y = 1 -distance / closestDistance;
					
				}
				
				
				x = 2*((followed.center.x-xDeltaL) / xSpace) -1;
			}

			float yDeltaU = meanY - minY;
			float yDeltaD = maxY - meanY;
			canvas.drawLine(0, yDeltaU, canvas.getWidth(), yDeltaU, paint);
			canvas.drawLine(0, canvas.getHeight()-yDeltaD, canvas.getWidth(), canvas.getHeight()-yDeltaD, paint);
			float ySpace = canvas.getHeight() - (yDeltaU + yDeltaD);
			
			
			
			
			pivotZ = 1- 2*((meanY - yDeltaU) / ySpace);
			if(usbConnection != null){
				usbConnection.steer(x,y,pivotZ);
			}
			if(ttsCallback != null){
				ttsCallback.tagsWereFound(ids);
			}
		}else{
			if(ttsCallback != null){
				ttsCallback.tagsWereFound(new int[0]);
			}
			usbConnection.steer(0,0,pivotZ);
		}

		if(followedTagWasFound){
			timestamp = System.currentTimeMillis();
		}else if(System.currentTimeMillis() - timestamp > MAX_UNFOLLOW_TIME){
			usbConnection.steer(0,0,pivotZ);
			if(ttsCallback != null){
				ttsCallback.followedTagWasLost();
			}
		}
		canvas.drawText(usbConnection.buffer, 50, 50, paint);
	}
	
	@Override
	public void destroy() {
		usbConnection.steer(0,0,pivotZ);
		delRecognizerNtv(ptr);
	}
	@Override
	public Size setCameraProcessingSize(int viewWidth, int viewHeight, Size[] sizes) {
		final int MAX_WIDTH = 1000;
        final float TARGET_ASPECT = 16.f / 9.f;
        final float ASPECT_TOLERANCE = 0.2f;
        Size outputSize = sizes[0];
        float outputAspect = (float) outputSize.getWidth() / outputSize.getHeight();
		for (Size candidateSize : sizes) {
			//Log.v(TAG, "preview size candidate: "+candidateSize);
            if (candidateSize.getWidth() > MAX_WIDTH) continue;
            float candidateAspect = (float) candidateSize.getWidth() / candidateSize.getHeight();
            boolean goodCandidateAspect =
                    Math.abs(candidateAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            boolean goodOutputAspect =
                    Math.abs(outputAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            if ((goodCandidateAspect && !goodOutputAspect) ||
                    candidateSize.getWidth() > outputSize.getWidth()) {
                outputSize = candidateSize;
                outputAspect = candidateAspect;
            }
        }
		Log.v(TAG, "size.w:"+outputSize.getWidth() + " size.h:"+outputSize.getHeight());
		return outputSize;
	}
	
	

	
	
}
