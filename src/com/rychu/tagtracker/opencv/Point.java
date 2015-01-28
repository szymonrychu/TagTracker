package com.rychu.tagtracker.opencv;

public class Point {
	public int id;
	public float x;
	public float y;
	public float startX;
	public float startY;
	public int r;
	public Point(){
		
	}
	public Point(int id){
		this.id = id;
	}
	public Point(int id, float sX, float sY){
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
