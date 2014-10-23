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
    public DriverHelper(Context context, UsbManager usbManager){
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    	context.registerReceiver(this, filter);
    	transreceive = findDevice(usbManager);
    }
    protected void finalize() throws Throwable {
    	context.unregisterReceiver(this);
    };
    @Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) { 
        	transreceive = false;
        	try {
				worker.join();
			} catch (InterruptedException e) {
			}
        }else if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
        	if(!transreceive){
            	transreceive = findDevice(usbManager);
            	worker = new Thread(this);
            	worker.start();
        	}
        }
        
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
            	return false;
            }

            //usbConnection.controlTransfer(0x21, 34, 0, 0, getLineEncoding(19200), 7, 1000);

            usbConnection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
            usbConnection.controlTransfer(0x21, 32, 0, 0, new byte[] { (byte) 0x80,
                    0x25, 0x00, 0x00, 0x00, 0x00, 0x08 }, 7, 0);
            usbConnection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 0); //Baudrate 9600
            
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
	private Boolean sending = false;
	public void send(final byte[] buffer){
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
	}
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
