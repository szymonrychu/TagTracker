package com.rychu.tagtracker.touch;

import com.rychu.tagtracker.opencv.Point;

import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class MultitouchListener implements OnTouchListener{
	
	
	private SparseArray<Point> pointers;
	
	
	public SparseArray<Point> getPoints(){
		return pointers;
	}
	public MultitouchListener(View view) {
		view.setOnTouchListener(this);
		pointers  = new SparseArray<Point>();
	}
	public void getDelta(int x, int y, int id){
		Point pntr = pointers.get(id);
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int index = event.getActionIndex();
		int id = event.getPointerId(index);
		int mask = event.getActionMasked();
		
		
		
		switch(mask){
	    case MotionEvent.ACTION_DOWN:
	    case MotionEvent.ACTION_POINTER_DOWN:
			id = event.getActionIndex();
			Point p = new Point();
			p.id = id;
			p.x = event.getX(p.id);
			p.y = event.getY(p.id);
			pointers.put(id, p);
			
			return true;
		case MotionEvent.ACTION_MOVE:
		      for (int size = event.getPointerCount(), i = 0; i < size; i++) {
		          Point point = pointers.get(event.getPointerId(i));
		          if (point != null) {
		            point.x = event.getX(i);
		            point.y = event.getY(i);
		          }
		        }
			return true;
	    case MotionEvent.ACTION_UP:
	    case MotionEvent.ACTION_POINTER_UP:
	    case MotionEvent.ACTION_CANCEL:
			pointers.remove(id);
			return true;
		}
		
		return false;
	}

}