package com.richert.tagtracker.ttstt;

import com.richert.tagtracker.activities.DriverActivity;
import com.richert.tagtracker.activities.RecognizeActivity;
import com.richert.tagtracker.elements.DriverHelper;
import com.richert.tagtracker.processing.OfflineDataHelper;
import com.richert.tagtracker.ttstt.TextToSpeechToText.SpeechToTextListener;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class TextProcessingService extends Service implements SpeechToTextListener{
	public static class SpeechToTextException extends Exception{
		private static final long serialVersionUID = -3578323972079959937L;
		public final static int ERROR_NETWORK_TIMEOUT = SpeechRecognizer.ERROR_NETWORK_TIMEOUT;
		public final static int ERROR_NETWORK = SpeechRecognizer.ERROR_NETWORK;
		public final static int ERROR_AUDIO = SpeechRecognizer.ERROR_AUDIO;
		public final static int ERROR_SERVER = SpeechRecognizer.ERROR_SERVER;
		public final static int ERROR_CLIENT = SpeechRecognizer.ERROR_CLIENT;
		public final static int ERROR_SPEECH_TIMEOUT = SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
		public final static int ERROR_NO_MATCH = SpeechRecognizer.ERROR_NO_MATCH;
		public final static int ERROR_RECOGNIZER_BUSY = SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
		public final static int ERROR_INSUFFICIENT_PERMISSIONS = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
		int type;
		public SpeechToTextException(int type) {
			this.type = type;
		}
	}
	private static final String TAG = TextProcessingService.class.getSimpleName();
	private Context context;
	private Application application;
	private String action, preferedActivity;
	private Boolean latch;
	public static final String INTENT_EXTRA = "intent type";
	private Text2Speech2TextBinder binder;
	private TextToSpeechToText ttstt;
	private LanguageHelper langHelper;
	private int[] waitingForResponse;
	private int[] waitingForEndTalking;
	private String response;
	private float confidence;
	private int eventType;
	private Boolean interrupted;
	
	
	
	public class Text2Speech2TextBinder extends Binder {
		public String speak(String text, Boolean waitForResponse) throws InterruptedException, SpeechToTextException{
			interrupted = false;
			ttstt.speak(text);
			response = null;
			eventType = 0;
			if(waitForResponse){
				waitingForResponse.wait();
			}else{
				waitingForEndTalking.wait();
			}
			if(interrupted){
				throw new InterruptedException();
			}
			if(eventType > 0 && eventType < 11){
				throw new SpeechToTextException(eventType);
			}
			return response;
		}
		public float getConfidence(){
			return confidence;
		}
		public void interrupt(){
			ttstt.speak("");
			interrupted = true;
			try{
				waitingForEndTalking.notify();
			}catch(Exception e){}
			try{
				waitingForResponse.notify();
			}catch(Exception e){}
		}
	}
	
	
	
	
	
	
	
	@Override
	public void onCreate() {
		this.confidence = 0.0f;
		this.eventType = 0;
		context = getBaseContext();
		application = getApplication();
		binder = new Text2Speech2TextBinder();
		langHelper = new LanguageHelper(this);
		ttstt = new TextToSpeechToText(this, langHelper);
		ttstt.setSpeechToTextListener(this);
		
		
		// TODO Auto-generated method stub
		super.onCreate();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return binder;
	}







	@Override
	public void onPartialRecognitionResult(String result, float confidence) {
		// TODO Auto-generated method stub
		
	}







	@Override
	public void onRecognitionResult(String result, float confidence) {
		this.response = result;
		this.confidence = confidence;
		try{
			this.waitingForResponse.notify();
		}catch(Exception e){}
	}







	@Override
	public void onDoneTalking(String text) {
		try{
			this.waitingForEndTalking.notify();
		}catch(Exception e){}
	}







	@Override
	public void onEvent(int type) {
		eventType = type;
		try{
			this.waitingForEndTalking.notify();
		}catch(Exception e){}
		try{
			this.waitingForResponse.notify();
		}catch(Exception e){}
		
		// TODO Auto-generated method stub
		
	}

}
