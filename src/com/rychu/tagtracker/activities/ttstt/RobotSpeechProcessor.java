package com.rychu.tagtracker.activities.ttstt;

import java.util.Arrays;
import java.util.LinkedList;

import android.content.Context;
import android.util.Log;

import com.rychu.tagtracker.activities.ttstt.TextToSpeechToText.SpeechToTextException;
import com.rychu.tagtracker.activities.ttstt.TextToSpeechToText.SpeechToTextListener;
import com.rychu.tagtracker.opencv.Recognizer.TTSCallback;

public abstract class RobotSpeechProcessor implements SpeechToTextListener, TTSCallback, Runnable{
	private final static String TAG = RobotSpeechProcessor.class.getSimpleName();
	public abstract void setFollowedTag(int tagID);
	
	private int[] previousTags;
	private TextToSpeechToText textProcessor;
	private String text = "";
	private String response = "";
	private Boolean waitForResponse = false;
	
	public RobotSpeechProcessor(Context context) {
		previousTags = new int[0];
		textProcessor = new TextToSpeechToText(context);
		textProcessor.setSpeechToTextListener(this);
	}
	private boolean integerInArray(int num, int[] array){
		if(array.length < 1){
			return false;
		}
		for(int n : array){
			if(n == num ){
				return true;
			}
		}
		return false;
	}
	private String speak(Boolean waitForResponse, String text){
		String response = "";
		try {
			response = textProcessor.speak(text, waitForResponse);
		} catch (InterruptedException e) {
		}
		if(waitForResponse){
			return response;
		}else{
			return "";
		}
	}
	@Override
	public void tagsWereFound(int[] tagIds) {
		Boolean addedTags = false;
		LinkedList<Integer> newlyFound = new LinkedList<Integer>();
		for(int num : tagIds){
			if(!integerInArray(num, previousTags)){
				newlyFound.add(num);
				if(!addedTags) addedTags = true;
			}
		}
		Boolean removedTags = false;
		LinkedList<Integer> recentlyRemoved = new LinkedList<Integer>();
		for(int num : previousTags){
			if(!integerInArray(num, tagIds)){
				recentlyRemoved.add(num);
				if(!removedTags) removedTags = true;
			}
		}
		previousTags = tagIds;
		
		if(newlyFound.size()> 0){
			int tag = newlyFound.getFirst();
			textProcessor.speak("Following tag number: "+tag);
			setFollowedTag(tag);
		}
		//TODO);
	}

	@Override
	public void followedTagWasLost() {
		setFollowedTag(-1);
	}

	@Override
	public void onPartialRecognitionResult(String result, float confidence) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRecognitionResult(String result, float confidence) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDoneTalking(String text) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEvent(int type) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void run() {
		try {
			if(waitForResponse){
				response = textProcessor.speak(text, waitForResponse);
			}else{
				response = "";
				textProcessor.speak(text, waitForResponse);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
