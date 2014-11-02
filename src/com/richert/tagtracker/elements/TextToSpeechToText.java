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
	}
	private static final String TAG = TextToSpeechToText.class.getSimpleName();
	private SpeechToTextListener speechToTextListener;
	private Context context;
	private TextToSpeech tts;
	private SpeechRecognizer recognizer;
	public String recognitionResult = null;
	private Runnable onUIThread = new Runnable() {
        @Override
        public void run() {
        	if(SpeechRecognizer.isRecognitionAvailable(context) && speakAndRecognize){
        		recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        		recognizer.setRecognitionListener(new RecognitionListener() {
					
					@Override
					public void onRmsChanged(float rmsdB) {
						// TODO Auto-generated method stub
						
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
					public void onReadyForSpeech(Bundle params) {
						// TODO Auto-generated method stub
						
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
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onError(int error) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onEndOfSpeech() {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onBufferReceived(byte[] buffer) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onBeginningOfSpeech() {
						// TODO Auto-generated method stub
						
					}
				});
        		if(speakAndRecognize){
					Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
	        		recognizer.startListening(intent);
        		}
        	}
           //Your code to run in GUI thread here
        }//public void run() {
	};
	
	
	
	public TextToSpeechToText(Context context){
		this.context = context;
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
	public int stringToInt(String response){
		try{
			int buf = Integer.parseInt(response);
			return buf;
		}catch (Exception e){}
		try{
			if(response.contains("one") || response.contains("first")){
				return 1;
			}else if(response.contains("two") || response.contains("second")){
				return 2;
			}else if(response.contains("three") || response.contains("third")){
				return 3;
			}else if(response.contains("four") || response.contains("fourth")){
				return 4;
			}else if(response.contains("five") || response.contains("fifth")){
				return 5;
			}else if(response.contains("six") || response.contains("sixth")){
				return 6;
			}else if(response.contains("seven") || response.contains("seventh")){
				return 7;
			}else if(response.contains("eight") || response.contains("eighth")){
				return 8;
			}else if(response.contains("nine") || response.contains("ninth")){
				return 9;
			}else if(response.contains("ten") || response.contains("tenth")){
				return 10;
			}else if(response.contains("eleven") || response.contains("eleventh")){
				return 11;
			}else if(response.contains("twelve") || response.contains("twelveth")){
				return 12;
			}else if(response.contains("thirteen") || response.contains("thirteenth")){
				return 13;
			}else if(response.contains("fourteen") || response.contains("fourteenth")){
				return 14;
			}else if(response.contains("fiveteen") || response.contains("fiveteenth")){
				return 15;
			}else if(response.contains("sixteen") || response.contains("sixteenth")){
				return 16;
			}else if(response.contains("seventeen") || response.contains("seventeenth")){
				return 17;
			}else if(response.contains("eighteen") || response.contains("eighteenth")){
				return 18;
			}else if(response.contains("nineteen") || response.contains("nineteenth")){
				return 19;
			}else if(response.contains("twenty") || response.contains("twentieth")){
				return 20;
			}else if(response.contains("twenty one") || response.contains("twenty first")){
				return 21;
			}else if(response.contains("twenty two") || response.contains("twenty second")){
				return 22;
			}else if(response.contains("twenty three") || response.contains("twenty third")){
				return 23;
			}else if(response.contains("twenty four") || response.contains("twenty fourth")){
				return 24;
			}else if(response.contains("twenty five") || response.contains("twenty fivth")){
				return 25;
			}else if(response.contains("twenty six") || response.contains("twenty sixth")){
				return 26;
			}else if(response.contains("twenty seven") || response.contains("twenty seventh")){
				return 27;
			}else if(response.contains("twenty eight") || response.contains("twenty eighth")){
				return 28;
			}else if(response.contains("twenty nine") || response.contains("twenty nineth")){
				return 29;
			}else if(response.contains("thirty") || response.contains("thirtieth")){
				return 30;
			}else if(response.contains("thirty one") || response.contains("thity first")){
				return 31;
			}else if(response.contains("thirty two") || response.contains("thirty second")){
				return 32;
			}else{
				return -1;
			}
		}catch (Exception e){
			return -2;
		}
	}
	public void speak(final String text){
		if(speechToTextListener == null || !speakAndRecognize){
			return;
		}
		tts = new TextToSpeech(context, new OnInitListener() {
			@Override
			public void onInit(int status) {
				if(status != TextToSpeech.ERROR){
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