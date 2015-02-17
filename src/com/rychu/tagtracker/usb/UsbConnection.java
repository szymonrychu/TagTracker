package com.rychu.tagtracker.usb;

import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public abstract class UsbConnection implements Runnable{
	private static final String TAG = UsbConnection.class.getSimpleName();
	protected static final int MAX_PWM_CHANNELS = 6;
	private volatile UsbEndpoint inEndpoint;
	private volatile UsbEndpoint outEndpoint;
    private UsbDeviceConnection usbConnection;
    private UsbDevice usbDevice;
    private Thread worker;
    private UsbManager usbManager;
    private Context context;
    private Boolean work = true;
    private int timeoutMs = 100;
    public String buffer = "177,177,000,000,000,000,";
    public abstract void steer(float x, float y, float z);
    public abstract void steer(float x, float y);
    private int bufferSize = 10;
    public UsbConnection(Context context) {
		this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		this.context = context;
	}
    public void startWorking(){
    	work = true;
		worker = new Thread(this);
		worker.start();
    }
    public void stopWorking() throws InterruptedException{
    	work = false;
    	worker.join();
    }
    
    
    
    
    
    
    public void alterBuffer(int data[]) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    	if(data.length != MAX_PWM_CHANNELS){
    		throw new IllegalArgumentException("data.length != "+ MAX_PWM_CHANNELS);
    	}
    	StringBuilder sb = new StringBuilder();
    	for(int c=0; c< MAX_PWM_CHANNELS; c++){
    		int d = data[c];
    		if( d > 255 || d < 0){
        		throw new IllegalArgumentException("data["+c+"] not within a range [0, 255] "+d);
        	}
    		sb.append(String.format("%03d", d));
    		sb.append(",");
    	}
    	buffer = sb.toString();
    }
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
	        		//usbManager.requestPermission(usbDevice, pendIntent);
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
}
