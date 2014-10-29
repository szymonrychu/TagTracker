package com.richert.tagtracker.elements;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.widget.Toast;

public class TextToSpeechToText {
	private Context context;
	private TextToSpeech tts;
	public Boolean isSpeaking = false;
	public Boolean isWaiting = false;
	public TextToSpeechToText(Context context){
		this.context = context;
	}
	public String getResponse(int requestCode, int resultCode, Intent data, int uniqueID){
		String result = null;
		if(requestCode == uniqueID && resultCode == Activity.RESULT_OK){
			ArrayList<String> text = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			result = text.get(0);
			isWaiting = false;
		}
		return result;
		
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
	public void ask(final String text, final int uniqueID){
		if(isSpeaking != true && isWaiting!= true){
			isWaiting = true;
			isSpeaking = true;
			tts = new TextToSpeech(context, new OnInitListener() {
				@Override
				public void onInit(int status) {
					if(status == TextToSpeech.ERROR){
			            Toast t = Toast.makeText(context, "Opps! Your device doesn't support Text to Speech", Toast.LENGTH_SHORT);
			            t.show();
					}else{
						tts.setLanguage(Locale.US);
						tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
							@Override
							public void onStart(String utteranceId) {
							}
	
							@Override
							public void onDone(String utteranceId) {
								Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
						        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
								if(tts != null){
									tts.shutdown();
									tts = null;
									isSpeaking = false;
								}
						        try {
						            ((Activity) context).startActivityForResult(intent, uniqueID);
						        } catch (ActivityNotFoundException a) {
						            Toast t = Toast.makeText(context,
						                    "Opps! Your device doesn't support Speech to Text",
						                    Toast.LENGTH_SHORT);
						            t.show();
						        }
							}
	
							@Override
							public void onError(String utteranceId) {
								if(tts != null){
									tts.shutdown();
									tts = null;
									isSpeaking = false;
								}
							}
							
						});
						tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,String.valueOf(System.currentTimeMillis()));
					}
					
				}
			});
		}
	}
	public void speak(final String text){
		if(isSpeaking != true){
			isSpeaking = true;
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
									isSpeaking = false;
								}
							}
		
							@Override
							public void onError(String utteranceId) {
								if(tts != null){
									tts.shutdown();
									tts = null;
									isSpeaking = false;
								}
							}
							
						});
						tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,String.valueOf(System.currentTimeMillis()));
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
}
