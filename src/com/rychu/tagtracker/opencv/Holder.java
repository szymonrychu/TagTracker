package com.rychu.tagtracker.opencv;

import org.opencv.core.Mat;

/**
 * Holder class used by native methods to communicate with java.
 * @author szymon
 *
 */
public class Holder{
	public Mat tag;
	public Mat homo;
	public float x[];
	public float y[];
	public float cx;
	public float cy;
	public int id;
}
