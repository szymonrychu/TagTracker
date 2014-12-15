package com.richert.tagtracker.elements;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import com.richert.tagtracker.usb.UsbConnectionService;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

public class DriverHelper extends BroadcastReceiver implements Runnable{
	private static final String TAG = DriverHelper.class.getSimpleName();
	private volatile UsbEndpoint inEndpoint;
	private volatile UsbEndpoint outEndpoint;
    private UsbDeviceConnection usbConnection;
    private UsbDevice usbDevice;
    private Thread worker;
    private int timeoutMs = 100;
    private int bufferSize = 3;
    private UsbManager usbManager;
    private Context context;
	private Boolean sending = false;
	private int steerMax = 250;
	private int steerMin = 130;
	private int pivotMax = 250;
	private int pivotMin = 50;
    private String buffer = "177,177,000,000,000,000";
    private int maxVals = 6;
    private Boolean work = true;
    private Thread monitor;
    public DriverHelper(Context context, UsbManager usbManager){
    	this.context = context;
    	this.usbManager = usbManager;
    	this.usbManager = usbManager;
        
		StringBuilder sb = new StringBuilder();
		for(int c=0;c<maxVals;c++){
			sb.append(String.format("%03d,", 0));
		}
		buffer = sb.toString();
    }
    public void unregisterReceiver(){
    	try{
        	work = false;
    		if(worker != null){
				worker.join();
    		}
    		context.unregisterReceiver(this);
			monitor.join();
    	}catch(Exception e ){}
    	
    }
    public void steer(final float procX, final float procY, final float procZ){
		int steerings[] = new int[6];
		int steerCenter = (steerMax - steerMin)/2 + steerMin;
		int pivotCenter = (pivotMax - pivotMin)/2 + pivotMin;
		steerings[0] = steerCenter + (int)(procX*((steerMax - steerMin)/2));
		steerings[1] = pivotCenter - (int)(procZ*((pivotMax - pivotMin)/2));
		if(steerings[0] < steerMin){
			steerings[0] = steerMin;
		}
		if(steerings[0] > steerMax){
			steerings[0] = steerMax;
		}
		if(steerings[1] < pivotMin){
			steerings[1] = pivotMin;
		}
		if(steerings[1] > pivotMax){
			steerings[1] = pivotMax;
		}
		int maxValue = 200;
		steerings[2] = procY > 0 ? procX > 0 ? (int)(procY*maxValue) : Math.max((int)(procY*maxValue)+(int)(procX*50),0) : 0;//leftT
		steerings[3] = procY < 0 ? procX > 0 ? -(int)(procY*maxValue) : Math.max(-(int)(procY*maxValue)+(int)(procX*50),0) : 0;//leftP
		steerings[4] = procY > 0 ? procX < 0 ? (int)(procY*maxValue) : Math.max((int)(procY*maxValue)-(int)(procX*50),0) : 0;//rightT
		steerings[5] = procY < 0 ? procX < 0 ? -(int)(procY*maxValue) : Math.max(-(int)(procY*maxValue)-(int)(procX*50),0) : 0;//rightP
		
		
		
		StringBuilder sb = new StringBuilder();
		for(int steer : steerings){
			sb.append(String.format("%03d,", steer));
		}
		buffer = sb.toString();
    }
    public String getBuffer(){
    	return buffer;
    }
    public Boolean transreceiving(){
    	return usbConnection != null;
    }
    @Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
        	work = true;
        }else if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
        	work = true;
        }
        
    }
    private String checkIfNull(Object obj){
    	if(obj == null){
    		return "null";
    	}else{
    		return "valid";
    	}
    }
    public String getState(){
    	StringBuilder sb = new StringBuilder();
    	sb.append(" device=");
    	sb.append(checkIfNull(usbDevice));
    	sb.append(" connection=");
    	sb.append(checkIfNull(usbConnection));
    	sb.append(" inEndpoint=");
    	sb.append(checkIfNull(inEndpoint));
    	sb.append(" outEndpoint=");
    	sb.append(checkIfNull(outEndpoint));
    	return sb.toString();
    }
    public String getDeviceInfo(){
    	StringBuilder sb = new StringBuilder();
		sb.append("");
    	if(usbDevice != null){
    		sb.append("VId=");
    		sb.append(usbDevice.getVendorId());
    		sb.append(" PId=");
            sb.append(usbDevice.getProductId());
            sb.append(" DId=");
            sb.append(usbDevice.getDeviceId());
            sb.append(" DevN=");
            sb.append(usbDevice.getDeviceName());
            sb.append(" DPr=");
            sb.append(usbDevice.getDeviceProtocol());
            sb.append(" DCls=");
            sb.append(usbDevice.getDeviceClass());
            sb.append(" DScls=");
            sb.append(usbDevice.getDeviceSubclass());
            sb.append(" IntrfC=");
            sb.append(usbDevice.getInterfaceCount());
    	}
    	return sb.toString();
    }
    public void startMonitor(Context context){
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    	filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    	this.context.registerReceiver(this, filter);
    	
		
    	work = true;
    	worker = new Thread(this);
    	worker.start();
    }
    private byte[] getLineEncoding(int baudRate) {
        final byte[] lineEncodingRequest = { (byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
        switch (baudRate) {
        case 14400:
            lineEncodingRequest[0] = 0x40;
            lineEncodingRequest[1] = 0x38;
            break;

        case 19200:
            lineEncodingRequest[0] = 0x00;
            lineEncodingRequest[1] = 0x4B;
            break;
        }

        return lineEncodingRequest;
    }
    private void openDevice(){
    	usbConnection = usbManager.openDevice(usbDevice);
        if(usbConnection != null){
        	UsbInterface usbInterface = usbDevice.getInterface(1);
            if(!usbConnection.claimInterface(usbInterface, true)){
            	usbConnection.close();
            	usbConnection = null;
            }else{
            	final int RQSID_SET_LINE_CODING = 0x20;
	            final int RQSID_SET_CONTROL_LINE_STATE = 0x22;
	            
	            
	            int usbResult;
	            usbResult = usbConnection.controlTransfer(
	              0x21,        //requestType
	              RQSID_SET_CONTROL_LINE_STATE, //SET_CONTROL_LINE_STATE 
	              0,     //value
	              0,     //index
	              null,    //buffer
	              0,     //length
	              0);    //timeout
	            //baud rate = 9600
	            //8 data bit
	            //1 stop bit
	            byte[] encodingSetting = 
	              new byte[] {(byte)0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
	            usbResult = usbConnection.controlTransfer(
	              0x21,       //requestType
	              RQSID_SET_LINE_CODING,   //SET_LINE_CODING
	              0,      //value
	              0,      //index
	              encodingSetting,  //buffer
	              7,      //length
	              0);     //timeout
	            
	            
	            
	            
	            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
	                if (usbInterface.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
	                    if (usbInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
	                        inEndpoint = usbInterface.getEndpoint(i);
	                    } else if (usbInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT) {
	                        outEndpoint = usbInterface.getEndpoint(i);
	                    }
	                }
	            }
	            
	            if (inEndpoint == null || outEndpoint == null) {
	                Log.e(TAG, "No endpoint found!");
	                usbConnection.close();
	                usbConnection = null;
	            }
            }
        }
    }
    Intent intent;
	@Override
	public void run() {
		while(work){
			if(usbDevice != null && usbConnection != null && inEndpoint != null && outEndpoint != null){
				usbConnection.bulkTransfer(outEndpoint, buffer.getBytes(), buffer.getBytes().length, timeoutMs);
	        }else{
	        	usbDevice = null;
	        	usbConnection = null;
	        	inEndpoint = null;
	        	outEndpoint = null;
	        	HashMap<String, UsbDevice> usbDeviceList = usbManager.getDeviceList();
		        Iterator<UsbDevice> deviceIterator = usbDeviceList.values().iterator();
		        if (deviceIterator.hasNext()) {
		            usbDevice = deviceIterator.next();
		        }
		        if (usbDevice != null)  {
		        	intent = new Intent(context, UsbConnectionService.class);
		    		intent.putExtra(usbManager.EXTRA_PERMISSION_GRANTED, false);  // for extra data if needed..

		    		Random generator = new Random();

		    		PendingIntent i=PendingIntent.getActivity(context, generator.nextInt(), intent,PendingIntent.FLAG_UPDATE_CURRENT);
		    		usbManager.requestPermission(usbDevice, i);
		    		if(intent.getBooleanExtra(usbManager.EXTRA_PERMISSION_GRANTED, false)){
						openDevice();
		    		}
		        }
	        }
        	try {
	        	Thread.sleep(timeoutMs);
			} catch (InterruptedException e) {
			}
			
		}
	}
	protected byte[] receive(byte[] buffer){
		byte[] inBuffer = new byte[bufferSize];
		if(usbConnection!=null && inEndpoint != null){
			usbConnection.bulkTransfer(inEndpoint, inBuffer, bufferSize, timeoutMs);
		}
		return inBuffer;
	}

    public static int byte2int(byte[] arr){
    	return (arr[0]<<24)&0xff000000|
    		       (arr[1]<<16)&0x00ff0000|
    		       (arr[2]<< 8)&0x0000ff00|
    		       (arr[3]<< 0)&0x000000ff;
    }
    public static byte[] int2byte(int value){
    	return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    	
    }
	public int getTimeoutMs() {
		return timeoutMs;
	}
	public void setTimeoutMs(int timeoutMs) {
		this.timeoutMs = timeoutMs;
	}
	public int getBufferSize() {
		return bufferSize;
	}
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	
}
