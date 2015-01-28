package com.rychu.tagtracker.opencv;

import org.opencv.core.Mat;

public class Tag {
	public Point points[];
	public Point center;
	public int id;
	public Mat homo;
	public Mat preview;
	public double len;
}
