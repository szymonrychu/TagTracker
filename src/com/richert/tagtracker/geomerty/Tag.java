package com.richert.tagtracker.geomerty;

import org.opencv.core.Mat;

public class Tag {
	public Point points[];
	public Point center;
	public int id;
	public Mat homo;
	public Mat preview;
	public double len;
}
