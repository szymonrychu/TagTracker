package com.richert.tagtracker.elements;

import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnMultitouch implements OnTouchListener{
	private SparseArray<Pointer> pointers;
	
	
	public SparseArray<Pointer> getPoints(){
		return pointers;
	}
	public OnMultitouch(View view) {
		view.setOnTouchListener(this);
		pointers  = new SparseArray<Pointer>();
	}
	public void getDelta(int x, int y, int id){
		Pointer pntr = pointers.get(id);
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
			float x = event.getX(id);
			float y = event.getY(id);
			Pointer p = new Pointer(id,x,y);
			p.x = event.getX(p.id);
			p.y = event.getY(p.id);
			pointers.put(id, p);
			
			return true;
		case MotionEvent.ACTION_MOVE:
		      for (int size = event.getPointerCount(), i = 0; i < size; i++) {
		          Pointer point = pointers.get(event.getPointerId(i));
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
