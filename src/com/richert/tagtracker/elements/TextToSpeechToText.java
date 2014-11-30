package com.richert.tagtracker.elements;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.sax.TextElementListener;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.widget.Toast;

public class TextToSpeechToText {
	
	public interface SpeechToTextListener{
		public void onPartialRecognitionResult(String result, float confidence);
		public void onRecognitionResult(String result, float confidence);
		public void onDoneTalking(String text);
		public void onEvent(int type);
	}
	private static final String TAG = TextToSpeechToText.class.getSimpleName();
	private SpeechToTextListener speechToTextListener;
	private Context context;
	private TextToSpeech tts;
	private SpeechRecognizer recognizer;
	public String recognitionResult = null;
	protected float rmsdB = 0.0f;
	protected Bundle params;
	protected int eventType;
	protected byte[] receivedBuffer;
	private LanguageHelper langHelper;
	public String[] getAvailableCountries(){
		return Locale.getISOCountries();
	}
	public String[] getAvailableLanguages(){
		return Locale.getISOLanguages();
	}
	
	private Runnable onUIThread = new Runnable() {
        @Override
        public void run() {
        	if(SpeechRecognizer.isRecognitionAvailable(context) && speakAndRecognize){
        		recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        		recognizer.setRecognitionListener(new RecognitionListener() {
					
					@Override
					public void onRmsChanged(float rmsdBL) {
						rmsdB = rmsdBL;
						speechToTextListener.onEvent(LanguageHelper.EVENT_RMS_CHANGED);
					}
					
					@Override
					public void onResults(Bundle results) {
						if(!speakAndRecognize){
							return;
						}
						recognitionResult = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
						Log.v(TAG,recognitionResult);
						float[] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
						// TODO Auto-generated method stub
						speechToTextListener.onRecognitionResult(recognitionResult, confidence[0]);
					}
					
					@Override
					public void onReadyForSpeech(Bundle p) {
						params = p;
						speechToTextListener.onEvent(LanguageHelper.EVENT_READY_FOR_SPEECH);
					}
					
					@Override
					public void onPartialResults(Bundle partialResults) {
						if(!speakAndRecognize){
							return;
						}
						recognitionResult = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
						Log.v(TAG,recognitionResult);
						float[] confidence = partialResults.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
						// TODO Auto-generated method stub
						speechToTextListener.onPartialRecognitionResult(recognitionResult, confidence[0]);
					}
					
					@Override
					public void onEvent(int eventType, Bundle params) {
					}
					
					@Override
					public void onError(int error) {
						speechToTextListener.onEvent(error);
					}
					
					@Override
					public void onEndOfSpeech() {
						speechToTextListener.onEvent(LanguageHelper.EVENT_END_OF_SPEECH);
					}
					
					@Override
					public void onBufferReceived(byte[] bufferL) {
						receivedBuffer = bufferL;
						speechToTextListener.onEvent(LanguageHelper.EVENT_BUFFER_RECEIVED);
					}
					
					@Override
					public void onBeginningOfSpeech() {
						speechToTextListener.onEvent(LanguageHelper.EVENT_BEGINNING_OF_SPEECH);
					}
				});
        		if(speakAndRecognize){
					Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
	        		recognizer.startListening(intent);
        		}
        	}
        }
	};
	
	
	
	public TextToSpeechToText(Context context, LanguageHelper langHelper){
		this.context = context;
		this.langHelper = langHelper;
	}
	private Boolean speakAndRecognize;
	public void setSpeechToTextListener(SpeechToTextListener listener){
		speechToTextListener = listener;
		speakAndRecognize = true;
	}
	public void onPause(){
		speakAndRecognize = false;
		if(recognizer != null){
			recognizer.stopListening();
			recognizer.destroy();
		}
		if(tts != null){
			tts.stop();
			tts.shutdown();
		}
	}
	public String recognizeText(){
		if(speechToTextListener == null){
			return null;
		}
		if(!speakAndRecognize){
			return null;
		}
		recognitionResult = null;
		((Activity) context).runOnUiThread(onUIThread);
		return recognitionResult;
	}
	
	public void speak(final String text){
		if(speechToTextListener == null || !speakAndRecognize){
			return;
		}
		tts = new TextToSpeech(context, new OnInitListener() {
			@Override
			public void onInit(int status) {
				if(status == TextToSpeech.SUCCESS){
					tts.setLanguage(Locale.US);
					tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
						@Override
						public void onStart(String utteranceId) {
						}
	
						@Override
						public void onDone(String utteranceId) {
							if(tts != null){
								tts.shutdown();
								tts = null;
							}
							speechToTextListener.onDoneTalking(text);
							
						}
	
						@Override
						public void onError(String utteranceId) {
							if(tts != null){
								tts.shutdown();
								tts = null;
							}
							speechToTextListener.onDoneTalking(text);
						}
						
					});
	        		if(speakAndRecognize){
	        			tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,String.valueOf(System.currentTimeMillis()));
	        		}
				}else{
		            Toast t = Toast.makeText(context,
		                    "Opps! Your device doesn't support Text to Speech",
		                    Toast.LENGTH_SHORT);
		            t.show();
					
				}
			}
		});
		
	}
}
