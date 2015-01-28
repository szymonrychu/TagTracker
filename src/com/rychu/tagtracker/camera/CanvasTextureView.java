package com.rychu.tagtracker.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

public class CanvasTextureView extends TextureView  {
	private boolean locked;
	public interface RedrawingCallback{
		void redraw(Canvas canvas);
	}
	private RedrawingCallback callback;
	private void init(Context context){
		setOpaque(false);
		locked = false;
	}
	public CanvasTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}
	public CanvasTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	public CanvasTextureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public CanvasTextureView(Context context) {
		super(context);
		init(context);
	}
	public void init(RedrawingCallback c){
		this.callback = c;
	}
	public void requestRedraw() throws IllegalArgumentException{
		if(callback == null ){
			throw new IllegalArgumentException("Please init the view first (Redrawing Callback == null)");
		}
		if(!locked){
			locked = true;
			final Canvas canvas = lockCanvas(null);
			if(canvas != null){
				synchronized(canvas){
					try{
						canvas.drawColor(0, Mode.CLEAR);
						callback.redraw(canvas);
					}catch(Exception e){
						e.printStackTrace();
					}
					unlockCanvasAndPost(canvas);
				}
			}
			locked = false;
		}
		
		
	}

}
