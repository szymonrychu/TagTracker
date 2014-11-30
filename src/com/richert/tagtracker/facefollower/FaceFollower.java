package com.richert.tagtracker.facefollower;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import com.richert.tagtracker.R;

import android.content.Context;
import android.hardware.Camera;
import android.provider.SyncStateContract.Helpers;
import android.util.Log;

import com.richert.tagtracker.geomerty.Point;
import com.richert.tagtracker.geomerty.Tag;
import com.richert.tagtracker.natUtils.Misc;


public class FaceFollower {
	static {
		if (!OpenCVLoader.initDebug()) {
	        // Handle initialization error
	    } else {
	    	//TODO here load native library
	    }
	}
    private CascadeClassifier mJavaDetector;
	private final static String TAG = FaceFollower.class.getSimpleName();
	private long ptr = 0;
	private native long newFaceFollowerNtv(int width, int height, String filename);
	private native void delFaceFollowerNtv(long ptr);
	private native Object[] findFacesNtv(long ptr, long yuvAddr);
	private native void notifySizeChangedNtv(long ptr, int width, int height, int rotation);
	

    private int mAbsoluteFaceSize = 0;
    private float mRelativeFaceSize = 0.2f;
    private File cascadeFile;
	public FaceFollower(Context context) {
		try {
            // load cascade file from application resources
            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (mJavaDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
	}
	public void setFaceSize(float faceSize){
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
	}
	public Tag[] findTags(Mat yuvFrame, int rotation){
		
		Mat gray = Misc.yuv2Rgb(yuvFrame, rotation);

		if (mAbsoluteFaceSize == 0) {
            int height = gray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }
        MatOfRect faces = new MatOfRect();
		mJavaDetector.detectMultiScale(gray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
		
		ArrayList<Tag> tags= new ArrayList<Tag>();
		Rect[] facesArray = faces.toArray();
		int c = 0;
		float X=0;
		float Y=0;
		for(Rect rect : facesArray){
			Tag t = new Tag();
	    	t.points = new Point[4];
	    	Point p = new Point();
	    	p.x = rect.x;
	    	p.y = rect.y;
	    	X+=p.x; Y+=p.y;
	    	t.points[0] = p;
	    	p = new Point();
	    	p.x = rect.x + rect.width;
	    	p.y = rect.y;
	    	X+=p.x; Y+=p.y;
	    	t.points[1] = p;
	    	p = new Point();
	    	p.x = rect.x;
	    	p.y = rect.y + rect.height;
	    	X+=p.x; Y+=p.y;
	    	t.points[3] = p;
	    	p = new Point();
	    	p.x = rect.x + rect.width;
	    	p.y = rect.y + rect.height;
	    	X+=p.x; Y+=p.y;
	    	t.points[2] = p;
	    	t.center = new Point();
	    	t.center.x = X/4;
	    	t.center.y = Y/4;
	    	tags.add(t);
		}
        if(tags.size()> 0){
        	Tag[] result = new Tag[tags.size()];
            c = 0;
            for(Tag t : tags){
            	result[c++] = t;
            }
            return result;
        }else{
        	return null;
        }
        
	}
}
