package com.richert.tagtracker.elements;

import com.richert.tagtracker.multitouch.OnMultitouch;
import com.richert.tagtracker.multitouch.Pointer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.View;

public class ControlsHelper{
	private Paint paint;
	private OnMultitouch listener;
	private ControlsCallback callback = null;
	public interface ControlsCallback{
		void getPivotPosition(float procX, float procY);
	}
	public void setControlsCallback(ControlsCallback c){
		this.callback = c;
	}
	class Joystick{
		private int bX = 0;
		private int bY = 0;
		private int bR = 0;
		private int pR = 0;
		public Joystick(){
			this.bX = 4;
			this.bY = 2;
			this.bR = 5;
			this.pR = 2;
		}
		public Joystick(int bX, int bY, int bR, int pR) {
			this.bX = bX;
			this.bY = bY;
			this.bR = bR;
			this.pR = pR;
		}
		int joyPntrId=-1;
		public void drawJoypad(Canvas canvas, SparseArray<Pointer> pointers){
			int bW = canvas.getWidth()/bX;
			int bH = canvas.getHeight()/bY;
			int bR = Math.min(canvas.getHeight(), canvas.getWidth())/this.bR;
			int pR = bR/this.pR;
			Pnt base = new Pnt(bW ,bH ,bR);
			paint.setColor(Color.BLUE);
			RectF baseRect = new RectF(bW-bR,bH-bR,bW+bR,bH+bR);
			canvas.drawRoundRect(baseRect, base.r/2, base.r/2, paint);
			Pnt pivot = new Pnt(bW,bH,pR);
			paint.setColor(Color.RED);
			Boolean neutral = true;
			if(pointers.size()>0){
				for(int id=0; id<pointers.size(); id++){
					Pointer p = pointers.get(id);
					if(p != null){
						if(joyPntrId != -1 || baseRect.contains(p.x, p.y) ){
							joyPntrId = id;
							break;
						}
					}
					
					
					
				}
			}else{
				joyPntrId = -1;
			}
			
			if(joyPntrId==-1){
				canvas.drawCircle(bW, bH, pR, paint);
				if(callback != null && !sentNeutral){
					sentNeutral = true;
					callback.getPivotPosition(0,0);
				}
			}else{
				Pointer p = pointers.get(joyPntrId);
				if(p != null){
					float x = p.getDX() > 0 ? Math.min(p.getDX(), pR) : Math.max(p.getDX(), -pR);
					float y = p.getDY() > 0 ? Math.min(p.getDY(), pR) : Math.max(p.getDY(), -pR);
					if(callback != null){
						sentNeutral = false;
						callback.getPivotPosition((x)/(pR), (y)/(pR));
					}
					canvas.drawCircle(x+bW, y+bH, pR, paint);
					neutral = false;
				}
			}
			
		}
	}
	private class Pnt{
		int x;
		int y;
		int r;
		public Pnt() {}
		public Pnt(int x, int y){
			this.x=x;
			this.y=y;
		}
		public Pnt(int x, int y, int r){
			this.x=x;
			this.y=y;
			this.r=r;
		}
	}
	private Joystick joy;
	public ControlsHelper(View drawerView) {
		paint = new Paint();
		paint.setStrokeWidth(5.0f);
		listener = new OnMultitouch(drawerView);
		joy = new Joystick();
		// TODO Auto-generated constructor stub
	}
	private Boolean sentNeutral = false;
	public void drawControls(Canvas canvas){
		SparseArray<Pointer> pointers = listener.getPoints();
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int d = canvas.getDensity();
		joy.drawJoypad(canvas, pointers);
	}
}
