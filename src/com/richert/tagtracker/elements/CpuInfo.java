package com.richert.tagtracker.elements;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class CpuInfo implements Runnable{
	private static final String TAG = CpuInfo.class.getSimpleName();
	private int cpuNum = 0;
	private Queue<float[]> queue;
	private float[][] data;
	private long[][] rawData;
	private long total[];
	private static final String[] legend = {"user", "nice", "system", "idle", "iowait", "irq", "sirq"};
	private RandomAccessFile reader;
	private Boolean opened;
	private Thread worker;
	private Paint paint;
	private int window;
	public CpuInfo(int window) {
		this.window = window;
		cpuNum = getNumCoresPrv();
		queue = new LinkedBlockingQueue<float[]>(window);
		data = new float[cpuNum+1][7];
		rawData = new long[cpuNum+1][7];
		total = new long[cpuNum+1];
		for(int c=0;c<cpuNum+1; c++){
			for(int n=0;n<6; n++){
				data[c][n]=0;
				rawData[c][n]=0;
			}
			total[c] = 1;
		}
		
		worker = new Thread(this);

		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.GREEN);
		paint.setStrokeWidth(2.0f);
		paint.setTextSize(15.0f);
	}

	private void closeReader(){
       	try {
			if(reader != null){
				reader.close();
		       	reader = null;
		       	opened = false;
	       	}
		} catch (IOException e) {}
	}
	private void openReader(){
		try {
			reader = new RandomAccessFile("/proc/stat", "r");
			opened = true;
		} catch (FileNotFoundException e) {
	       	Log.e(TAG, "can't read /proc/stat!");
			reader = null;
		}
	}
	
	public int getNumCores(){
		return cpuNum;
	}
	public String getCpuUsage(int coreNum){
		StringBuilder sb = new StringBuilder();
		sb.append("cpu");
		sb.append(coreNum);
		for(int c=0; c<7; c++){
			sb.append(":");
			sb.append(legend[c]);
			sb.append(":");
			sb.append(String.format("%3.2f",data[coreNum][c]));
		}
		return sb.toString();
	}
	public void drawCpuUsage(Canvas canvas, float x1, float y1, float x2, float y2){
		synchronized(queue){
			int len = queue.size();
			if(len < 2){
				return;
			}
			paint.setColor(Color.CYAN);
			canvas.drawLine(x1, y1, x1, y2, paint);
			canvas.drawLine(x1, y2, x2, y2, paint);
			paint.setColor(Color.RED);
			float deltaX = (x2 - x1) / window;
			float deltaY = (y2 - y1) ;
			float posX = x1;
			Iterator<float[]> it = queue.iterator();
			float prev[] = it.next();
			while(it.hasNext()){
				float data[] = it.next();
				for(int c=0; c<7; c++){
					float tmpVal[] = {(float)360*c/6.0f, 1.0f, 1.0f};
					int tmpColor = Color.HSVToColor(tmpVal);
					paint.setColor(tmpColor);
					float py = deltaY*prev[c]/100 + y1;
					float cy = deltaY*data[c]/100 + y1;
					canvas.drawLine(posX, py, posX+deltaX, cy, paint);
				}
				prev = data;
				posX+=deltaX;
			}
		}
		float posY=y1;
		for(int c=0; c<7; c++){
			float tmpVal[] = {(float)360*c/6.0f, 1.0f, 1.0f};
			int tmpColor = Color.HSVToColor(tmpVal);
			paint.setColor(tmpColor);
			canvas.drawText(legend[c], x2+2, posY+=16, paint);
		}
	}
	
	
	
	
	private int getNumCoresPrv() {
	    //Private Class to display only CPU devices in the directory listing
	    class CpuFilter implements FileFilter {
	        @Override
	        public boolean accept(File pathname) {
	            //Check if filename is "cpu", followed by a single digit number
	            if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
	                return true;
	            }
	            return false;
	        }      
	    }

	    try {
	        //Get directory containing CPU info
	        File dir = new File("/sys/devices/system/cpu/");
	        //Filter to only list the devices we care about
	        File[] files = dir.listFiles(new CpuFilter());
	        //Return the number of cores (virtual CPU devices)
	        return files.length;
	    } catch(Exception e) {
	        //Default to return 1 core
	        return 1;
	    }
	}
	public void stopReading(){
		try {
			closeReader();
			worker.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void startReading(){
		openReader();
		if(!worker.isAlive()){
			worker = new Thread(this);
			worker.start();
		}
		
	}
	public void setState(Boolean read){
		if(read){
			startReading();
		}else{
			stopReading();
		}
	}
	private long[] parseLine(String line){
		CharSequence raw = line.subSequence(5, line.length());
		String info[] = raw.toString().split(" ");
		long data[] = new long[8];
		long total = 0;
		for(int c=0; c<6; c++){
			data[c] = Long.parseLong(info[c]);
			total += data[c];
		}
		data[7] = total;
		return data;
	}
	@Override
	public void run() {
		while(opened){
			String line = "";
			try{
	        	reader.seek(0);
				line = reader.readLine();
				if(opened && line.startsWith("cpu")){
					long raw[] = parseLine(line);
					total[cpuNum] = raw[7] - total[cpuNum];
					for(int c=0; opened && c<7; c++){
						rawData[cpuNum][c] = raw[c] - rawData[cpuNum][c];
						data[cpuNum][c] = 100*((float)rawData[cpuNum][c])/((float)total[cpuNum]);
						synchronized(queue){
							if(opened && queue.size() >= window){
								queue.poll();
							}
							queue.add(data[cpuNum].clone());
						}
					}
				}
				for(int cpu=0; opened && cpu<cpuNum; cpu++){
					line = reader.readLine();
					if(opened && line.startsWith("cpu"+cpu)){
						long raw[] = parseLine(line);
						total[cpu] = raw[7] - total[cpu];
						for(int c=0; opened && c<7; c++){
							rawData[cpu][c] = 100*raw[c] - rawData[cpu][c];
							data[cpu][c] = ((float)rawData[cpu][c])/((float)total[cpu]);
						}
					}
				}
				Thread.sleep(250);
				
			}catch(IOException e){} catch (InterruptedException e) {}
			
		}
	}
		
}
