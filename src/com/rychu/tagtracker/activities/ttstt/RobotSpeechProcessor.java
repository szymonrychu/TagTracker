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
		for(int n : array){
			if(n == num ){
				return true;
			}
		}
		return false;
	}
	@Override
	public void tagsWereFound(int[] tagIds) {
		LinkedList<Integer> newlyFound = new LinkedList<Integer>();
		for(int num : tagIds){
			if(!integerInArray(num, previousTags)){
				newlyFound.add(num);
			}
		}

		Boolean deltaFound = false;
		StringBuilder sb = new StringBuilder();
		sb.append("newly found tags: ");
		for(Integer d : newlyFound){
			sb.append(d);
			sb.append(",");
			if(!deltaFound)deltaFound = true;
		}
		if(deltaFound){
			Log.v(TAG, sb.toString());
			setFollowedTag(newlyFound.getLast());
		}
		previousTags = tagIds;
	}

	@Override
	public void followedTagWasLost() {
		// TODO Auto-generated method stub
		
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
			response = textProcessor.speak(text, waitForResponse);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
