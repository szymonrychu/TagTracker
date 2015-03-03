package com.rychu.tagtracker.activities.ttstt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.SparseLongArray;
import android.widget.Toast;

import com.rychu.tagtracker.activities.ttstt.TextToSpeechToText.SpeechToTextException;
import com.rychu.tagtracker.activities.ttstt.TextToSpeechToText.SpeechToTextListener;
import com.rychu.tagtracker.opencv.Recognizer.TTSCallback;

public abstract class RobotSpeechProcessor implements SpeechToTextListener, TTSCallback{
	private final static String TAG = RobotSpeechProcessor.class.getSimpleName();
	private final Context context;
	public abstract void setFollowedTag(int tagID);
	public abstract void makeToast(String text);
	private final static int NO_TAG = -1;
	private final static int MAX_TAG = 32;
	
	private int followedTagID = NO_TAG;
	private int[] previousTags;
	private TextToSpeechToText textProcessor;
	private LanguageHelper translator;
	private HashMap<Integer, Long> nTag;
	private HashMap<Integer, Long> rTag;
	private HashMap<Integer, Long> validTags;
	private HashMap<Integer, Long> latestTimestamps;
	private LinkedList<Integer> tagsToFollow;
	private final static long MAX_TIME_DELTA = 1000;
	private long previousTimestamp = 0;
	private final int STATE_WAITING_FOR_DECISION = 0x01;
	private final int STATE_TAG_WAS_LOST = 0x02;
	private int state = 0x00;
	
	public RobotSpeechProcessor(Context context) {
		this.context = context;
		nTag = new HashMap<Integer, Long>();
		rTag = new HashMap<Integer, Long>();
		validTags = new HashMap<Integer, Long>();
		latestTimestamps = new HashMap<Integer, Long>();
		tagsToFollow = new LinkedList<Integer>();
		previousTags = new int[0];
		
		
		translator = new LanguageHelper(context);
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
	@Override
	public void tagsWereFound(int[] tagIds, long timestamp) {
		long now = System.currentTimeMillis();
		Boolean addedTags = false;
		Boolean removedTags = false;
		Boolean changeValidTags = false;
		Boolean changeFollowedTags = false;
		Boolean followedTagWasFound = false;
		for(int num : previousTags){
			if(!integerInArray(num, tagIds)){
				rTag.put(num,timestamp);
				validTags.remove(num);
				
				removedTags = true;
			}else{
				rTag.remove(num);
			}
		}
		for(int num : tagIds){
			if(!integerInArray(num, previousTags)){
				nTag.put(num, timestamp);
				validTags.put(num, timestamp);
				addedTags = true;
			}else{
				nTag.remove(num);
			}
			latestTimestamps.put(num, now);
		}
		previousTags = tagIds;
		for(int num : validTags.keySet()){
			long tagTime = validTags.get(num);
			if(MAX_TIME_DELTA < now - tagTime){
				if(!tagsToFollow.contains(num)){
					tagsToFollow.add(num);
					changeFollowedTags = true;
				}
			}
		}
		if(changeFollowedTags){
			followedTagID = selectTags(true);
			setFollowedTag(followedTagID);
		}
		for(int num : latestTimestamps.keySet()){
			long tagTimestamp = latestTimestamps.get(num);
			if(now - tagTimestamp > MAX_TIME_DELTA){
				if(tagsToFollow.contains(num)){
					int pos = tagsToFollow.indexOf(num);
					tagsToFollow.remove(pos);
				}
			}
		}
		if(integerInArray(followedTagID, tagIds)){
			previousTimestamp = now;
		}else if(now - previousTimestamp > MAX_TIME_DELTA && followedTagID != NO_TAG){
			followedTagID = selectTags(false);
			setFollowedTag(followedTagID);
			
		}
	}
	
	private int selectTags(Boolean tagFound) {
		int t = NO_TAG;
		String textToTell = "";
		if(tagFound){
			// select tag to follow
			StringBuilder sb = new StringBuilder();
			sb.append(translator.getWhatTagToFollow());
			for(int tag : tagsToFollow){
				sb.append(tag);
				sb.append(", ");
			}
			textToTell = sb.toString();
			textProcessor.speak(textToTell);
			makeToast(textToTell);
			state = STATE_WAITING_FOR_DECISION;
		}else{
			// say that, the tag was lost
			textToTell = translator.getDontSeeTagz();
			textProcessor.speak(textToTell);
			makeToast(textToTell);
			state = STATE_TAG_WAS_LOST;
		}
		return t;
	}
	@Override
	public void onRecognitionResult(String result, float confidence) {
		switch(state){
		case STATE_WAITING_FOR_DECISION:
			for(int num=0;num<MAX_TAG;num++){
				String resultNum =  translator.getNum(num);
				if(result.contains(resultNum)){
					setFollowedTag(num);
					break;
				}
				resultNum = translator.getNumTH(num);
				if(result.contains(resultNum)){
					setFollowedTag(num);
					break;
				}
			}
			
			break;
		case STATE_TAG_WAS_LOST:
			break;
		}
	}

	@Override
	public void onPartialRecognitionResult(String result, float confidence) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onDoneTalking(String text) {
		switch(state){
		case STATE_WAITING_FOR_DECISION:
			textProcessor.recognize();
			break;
		case STATE_TAG_WAS_LOST:
			break;
		}
	}

	@Override
	public void onEvent(int type) {
		// TODO Auto-generated method stub
		
	}
}
