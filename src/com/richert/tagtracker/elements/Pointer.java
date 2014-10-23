package com.richert.tagtracker.elements;

public class Pointer{
	public int id;
	public float x;
	public float y;
	public float startX;
	public float startY;
	public Pointer(int id){
		this.id = id;
	}
	public Pointer(int id, float sX, float sY){
		this.id = id;
		this.startX = sX;
		this.startY = sY;
	}
	public float getDX(){
		return x - startX;
	}
	public float getDY(){
		return y - startY;
	}
	public double getDistance(double x, double y){
		double X = x - this.x;
		double Y = y - this.y;
		return Math.sqrt(X*X+Y*Y);
	}
}