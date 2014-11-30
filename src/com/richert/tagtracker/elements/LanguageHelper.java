package com.richert.tagtracker.elements;

import java.text.NumberFormat;
import java.util.Locale;

import com.richert.tagtracker.R;

import android.app.ListActivity;
import android.content.Context;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class LanguageHelper {
	public final static int ERROR_NETWORK_TIMEOUT = SpeechRecognizer.ERROR_NETWORK_TIMEOUT;
	public final static int ERROR_NETWORK = SpeechRecognizer.ERROR_NETWORK;
	public final static int ERROR_AUDIO = SpeechRecognizer.ERROR_AUDIO;
	public final static int ERROR_SERVER = SpeechRecognizer.ERROR_SERVER;
	public final static int ERROR_CLIENT = SpeechRecognizer.ERROR_CLIENT;
	public final static int ERROR_SPEECH_TIMEOUT = SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
	public final static int ERROR_NO_MATCH = SpeechRecognizer.ERROR_NO_MATCH;
	public final static int ERROR_RECOGNIZER_BUSY = SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
	public final static int ERROR_INSUFFICIENT_PERMISSIONS = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
	public final static int EVENT_RMS_CHANGED = 11;
	public final static int EVENT_READY_FOR_SPEECH = 12;
	public final static int EVENT_BEGINNING_OF_SPEECH = 13;
	public final static int EVENT_END_OF_SPEECH = 14;
	public final static int EVENT_BUFFER_RECEIVED = 15;
	private String errorCodes[] = null;
	private String num1[] = null;
	private String num10[] = null;
	private String num100[] = null;
	private Context context;
	private final static String TAG = LanguageHelper.class.getSimpleName(); 
	public LanguageHelper(Context context){
		this.context = context;
		this.errorCodes = new String[9];
		this.errorCodes[0] = context.getResources().getString(R.string.ERROR_CODE_1);
		this.errorCodes[1] = context.getResources().getString(R.string.ERROR_CODE_2);
		this.errorCodes[2] = context.getResources().getString(R.string.ERROR_CODE_3);
		this.errorCodes[3] = context.getResources().getString(R.string.ERROR_CODE_4);
		this.errorCodes[4] = context.getResources().getString(R.string.ERROR_CODE_5);
		this.errorCodes[5] = context.getResources().getString(R.string.ERROR_CODE_6);
		this.errorCodes[6] = context.getResources().getString(R.string.ERROR_CODE_7);
		this.errorCodes[7] = context.getResources().getString(R.string.ERROR_CODE_8);
		this.errorCodes[8] = context.getResources().getString(R.string.ERROR_CODE_9);
		this.num1 = new String[20];
		this.num1[0] = context.getResources().getString(R.string.num0);
		this.num1[1] = context.getResources().getString(R.string.num1);
		this.num1[2] = context.getResources().getString(R.string.num2);
		this.num1[3] = context.getResources().getString(R.string.num3);
		this.num1[4] = context.getResources().getString(R.string.num4);
		this.num1[5] = context.getResources().getString(R.string.num5);
		this.num1[6] = context.getResources().getString(R.string.num6);
		this.num1[7] = context.getResources().getString(R.string.num7);
		this.num1[8] = context.getResources().getString(R.string.num8);
		this.num1[9] = context.getResources().getString(R.string.num9);
		this.num1[10] = context.getResources().getString(R.string.num10);
		this.num1[11] = context.getResources().getString(R.string.num11);
		this.num1[12] = context.getResources().getString(R.string.num12);
		this.num1[13] = context.getResources().getString(R.string.num13);
		this.num1[14] = context.getResources().getString(R.string.num14);
		this.num1[15] = context.getResources().getString(R.string.num15);
		this.num1[16] = context.getResources().getString(R.string.num16);
		this.num1[17] = context.getResources().getString(R.string.num17);
		this.num1[18] = context.getResources().getString(R.string.num18);
		this.num1[19] = context.getResources().getString(R.string.num19);
		this.num10 = new String[8];
		this.num10[0] = context.getResources().getString(R.string.num20);
		this.num10[1] = context.getResources().getString(R.string.num30);
		this.num10[2] = context.getResources().getString(R.string.num40);
		this.num10[3] = context.getResources().getString(R.string.num50);
		this.num10[4] = context.getResources().getString(R.string.num60);
		this.num10[5] = context.getResources().getString(R.string.num70);
		this.num10[6] = context.getResources().getString(R.string.num80);
		this.num10[7] = context.getResources().getString(R.string.num90);
		this.num100 = new String[9];
		this.num100[0] = context.getResources().getString(R.string.num100);
		this.num100[1] = context.getResources().getString(R.string.num200);
		this.num100[2] = context.getResources().getString(R.string.num300);
		this.num100[3] = context.getResources().getString(R.string.num400);
		this.num100[4] = context.getResources().getString(R.string.num500);
		this.num100[5] = context.getResources().getString(R.string.num600);
		this.num100[6] = context.getResources().getString(R.string.num700);
		this.num100[7] = context.getResources().getString(R.string.num800);
		this.num100[8] = context.getResources().getString(R.string.num900);
	}
	public static String[] getAvailableCountries(){
		return Locale.getISOCountries();
	}
	public static String[] getAvailableLanguages(){
		return Locale.getISOLanguages();
	}
	public static void setDefaultLocale(String language, String country){
		Locale.setDefault(new Locale(language,country));
	}
	public static void setDefaultLocale(String language){
		Locale.setDefault(new Locale(language));
	}
	public static void setDefaultLocale(Locale locale){
		Locale.setDefault(locale);
	}
	public static Locale getDefaultLocale(){
		return Locale.getDefault();
	}
	public String getNum(int num){
		StringBuilder sb = new StringBuilder();
		if(num < 0 ){
			sb.append(context.getResources().getString(R.string.numminus));
		}
		int absNum = Math.abs(num);
		if(absNum < 20){
			sb.append(" ");
			sb.append(num1[absNum]);
		}else if(absNum < 100){
			int one = absNum%10;
			int ten = absNum - one;
			ten = ten / 10;
			sb.append(" ");
			sb.append(num10[ten-2]);
			if(one != 0){
				sb.append(" ");
				sb.append(num1[one]);
			}
		}else if(absNum < 1000){
			int one = absNum%10;
			int ten = absNum%100 - one;
			ten = ten / 10;
			int houndred = absNum - (ten + one);
			houndred = houndred / 100;
			sb.append(" ");
			sb.append(num100[houndred]);
			if(ten != 0){
				sb.append(" ");
				sb.append(num10[ten-2]);
			}
			if(one != 0){
				sb.append(" ");
				sb.append(num1[one]);
			}
		}
		String res = sb.toString();
		Log.d(TAG,"number = "+res);
		return res;
	}
	public String getErrorString(int errorCode){
		String result = null;
		if(errorCode > 0 && errorCode < 10){
			result = this.errorCodes[errorCode];
		}
		return result;
	}
	public int stringToBool(String response){
		if(response.contains(context.getResources().getString(R.string.response_yes))){
			return 1;
		}else if(response.contains(context.getResources().getString(R.string.response_no))){
			return 0;
		}else{
			return -1;
		}
	}
	public String getWhatTagToFollow(){
		return context.getResources().getString(R.string.WHAT_TAG_FOLLOW);
	}
	public String getDontSeeTagz(){
		return context.getResources().getString(R.string.DONT_SEE_TAGZ);
	}
	public String getFollowing(){
		return context.getResources().getString(R.string.FOLLOWING);
	}
	public String getIDidntCatchThat(){
		return context.getResources().getString(R.string.I_DIDNT_CATCH_THAT);
	}
	public String getImNotSure(){
		return context.getResources().getString(R.string.I_AM_NOT_SURE);
	}
	public String getPleaseRepeat(){
		return context.getResources().getString(R.string.PLEASE_REPEAT);
	}
	public String getISItOk(){
		return context.getResources().getString(R.string.IS_IT_OK);
	}
	public String getError(){
		return context.getResources().getString(R.string.ERROR);
	}
	public String getFatalError(){
		return context.getResources().getString(R.string.FATAL_ERROR);
	}
	public int stringToInt(String response){
		for(int c = 1; c<=32;c++){
			if(response.contains(getNum(c))){
				return c;
			}
		}
		return -1;
	}
}
