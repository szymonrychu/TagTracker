package com.richert.tagtracker.driver;

import java.util.HashMap;
import java.util.Iterator;

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
    public Boolean transreceive = false;
    private Thread worker;
    private int timeoutMs = 100;
    private int bufferSize = 3;
    private UsbManager usbManager;
    private Context context;
	private Boolean sending = false;
	private int steerMax = 240;
	private int steerMin = 115;
	private int pivotMax = 240;
	private int pivotMin = 115;
    private float prevX;
    private float prevY;
    private String buffer = "";
    public DriverHelper(Context context, UsbManager usbManager){
    	this.context = context;
    	this.usbManager = usbManager;
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    	context.registerReceiver(this, filter);
    	transreceive = findDevice(usbManager);
    }
    public void unregisterReceiver(){
    	context.unregisterReceiver(this);
    }
    public void steer(final float procX, final float procY, final float procZ){
    	if((prevX != procX || prevY != procY) && !sending){
    		Thread th = new Thread(){
				@Override
				public void run() {
					sending = true;
					prevX = procX;
					prevY = procY;
					int steer, lFront, lBack, rFront, rBack, pivot;
					int steerCenter = (steerMax - steerMin)/2 + steerMin;
					int pivotCenter = (pivotMax - pivotMin)/2 + pivotMin;
					steer = steerCenter + (int)(procX*((steerMax - steerMin)/2));
					pivot = pivotCenter + (int)(procZ*((pivotMax - pivotMin)/2));
			
					lBack = procY > 0 ? procX > 0 ? (int)(procY*255) : Math.max((int)(procY*255)+(int)(procX*50),0) : 0;//leftT
					lFront = procY < 0 ? procX > 0 ? -(int)(procY*255) : Math.max(-(int)(procY*255)+(int)(procX*50),0) : 0;//leftP
					rBack = procY > 0 ? procX < 0 ? (int)(procY*255) : Math.max((int)(procY*255)-(int)(procX*50),0) : 0;//rightT
					rFront = procY < 0 ? procX < 0 ? -(int)(procY*255) : Math.max(-(int)(procY*255)-(int)(procX*50),0) : 0;//rightP
					StringBuilder sb = new StringBuilder();
					sb.append(String.format("%03d,", steer));
					sb.append(String.format("%03d,", pivot));
					sb.append(String.format("%03d,", lFront));
					sb.append(String.format("%03d,", lBack));
					sb.append(String.format("%03d,", rFront));
					sb.append(String.format("%03d,", rBack));
					buffer = sb.toString();
					if(usbConnection!=null && outEndpoint != null)	{
						usbConnection.bulkTransfer(outEndpoint, buffer.getBytes(), buffer.getBytes().length, timeoutMs);
					}
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sending=false;
					super.run();
				}
			};
			th.start();
			
    	}
    }
    public String getBuffer(){
    	return buffer;
    }
    @Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) { 
        	transreceive = findDevice(usbManager);
        	if(!transreceive){
		    	try {
		    		if(worker != null){
						worker.join();
		    		}
				} catch (InterruptedException e) {
				}
        	}
        }else if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
        	if(!transreceive){
            	transreceive = findDevice(usbManager);
            	if(transreceive){
		        	worker = new Thread(this);
		        	worker.start();
            	}
        	}
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
    	sb.append("device:");
    	sb.append(checkIfNull(usbDevice));
    	sb.append(":connection:");
    	sb.append(checkIfNull(usbConnection));
    	sb.append(":inEndpoint:");
    	sb.append(checkIfNull(inEndpoint));
    	sb.append(":outEndpoint:");
    	sb.append(checkIfNull(outEndpoint));
    	return sb.toString();
    }
	private Boolean findDevice(UsbManager usbManager) {
        HashMap<String, UsbDevice> usbDeviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = usbDeviceList.values().iterator();
        usbDevice = null;
        if (deviceIterator.hasNext()) {
            usbDevice = deviceIterator.next();

            // Print device information. If you think your device should be able
            // to communicate with this app, add it to accepted products below.
            Log.d(TAG, "VendorId: " + usbDevice.getVendorId());
            Log.d(TAG, "ProductId: " + usbDevice.getProductId());
            Log.d(TAG, "DeviceName: " + usbDevice.getDeviceName());
            Log.d(TAG, "DeviceId: " + usbDevice.getDeviceId());
            Log.d(TAG, "DeviceClass: " + usbDevice.getDeviceClass());
            Log.d(TAG, "DeviceSubclass: " + usbDevice.getDeviceSubclass());
            Log.d(TAG, "InterfaceCount: " + usbDevice.getInterfaceCount());
            Log.d(TAG, "DeviceProtocol: " + usbDevice.getDeviceProtocol());

            
        }
        if (usbDevice == null) {
            return false;
        } else {
            usbConnection = usbManager.openDevice(usbDevice);
            if(usbConnection== null){
            	return false;
            }
            UsbInterface usbInterface = usbDevice.getInterface(1);
            if(!usbConnection.claimInterface(usbInterface, true)){
            	usbConnection.close();
            	usbConnection = null;
            	return false;
            }

            /*connection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
            connection.controlTransfer(0x21, 32, 0, 0, new byte[] { (byte) 0x80,
                    0x25, 0x00, 0x00, 0x00, 0x00, 0x08 }, 7, 0);
            connection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 0); //Baudrate 9600*/
            

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
            
            if (inEndpoint == null) {
                Log.e(TAG, "No in endpoint found!");
                usbConnection.close();
                return false;
            }

            if (outEndpoint == null) {
                Log.e(TAG, "No out endpoint found!");
                usbConnection.close();
                return false;
            }
        }
        return true;
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
	@Override
	public void run() {
		while(transreceive){
			byte[] outBuffer = null;
			if(!sending){
				byte[] inBuffer = new byte[bufferSize];
				receive(inBuffer);
			}
		}
	}
	/*public void send(final byte[] buffer){
		if(usbConnection!=null && outEndpoint != null && buffer != null && !sending){
			Thread th = new Thread(){
				@Override
				public void run() {
					sending = true;
					usbConnection.bulkTransfer(outEndpoint, buffer, buffer.length, timeoutMs);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sending=false;
					super.run();
				}
			};
			th.start();
		}
	}*/
	protected void receive(byte[] buffer){
		if(usbConnection!=null && inEndpoint != null){
			usbConnection.bulkTransfer(inEndpoint, buffer, buffer.length, timeoutMs);
		}
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
