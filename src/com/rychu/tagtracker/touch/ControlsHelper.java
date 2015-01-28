package com.rychu.tagtracker.touch;

import com.rychu.tagtracker.opencv.Point;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

public class ControlsHelper{
	private Paint paint;
	private MultitouchListener listener;
	private ControlsCallback callback = null;
	private static final String TAG = ControlsHelper.class.getSimpleName();
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
		public void drawJoypad(Canvas canvas, SparseArray<Point> pointers){
			int bW = canvas.getWidth()/bX;
			int bH = canvas.getHeight()/bY;
			int bR = Math.min(canvas.getHeight(), canvas.getWidth())/this.bR;
			int pR = bR/this.pR;
			Point base = new Point(bW ,bH ,0);
			base.r = bR;
			paint.setColor(Color.BLUE);
			RectF baseRect = new RectF(bW-bR,bH-bR,bW+bR,bH+bR);
			canvas.drawRoundRect(baseRect, base.r/2, base.r/2, paint);
			Point pivot = new Point(bW,bH,0);
			pivot.r = pR;
			paint.setColor(Color.RED);
			Boolean neutral = true;
			if(pointers.size()>0){
				for(int id=0; id<pointers.size(); id++){
					Point p = pointers.get(id);
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
				Point p = pointers.get(joyPntrId);
				if(p != null){
					float x = p.getDX() > bW ? Math.min(p.getDX()-bW, pR) : Math.max(p.getDX()-bW, -pR);
					float y = p.getDY() > bH ? Math.min(p.getDY()-bH, pR) : Math.max(p.getDY()-bH, -pR);
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
	private Joystick joy;
	public ControlsHelper(View drawerView) {
		paint = new Paint();
		paint.setStrokeWidth(5.0f);
		listener = new MultitouchListener(drawerView);
		joy = new Joystick();
		// TODO Auto-generated constructor stub
	}
	private Boolean sentNeutral = false;
	public void drawControls(Canvas canvas){
		SparseArray<Point> pointers = listener.getPoints();
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int d = canvas.getDensity();
		joy.drawJoypad(canvas, pointers);
	}
}