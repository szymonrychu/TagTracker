package com.richert.tagtracker.elements;

import java.util.ArrayList;
import java.util.List;

import com.richert.tagtracker.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.hardware.Camera;
import android.os.Bundle;

public abstract class ThreadDialog extends DialogFragment{
	List<String> texts = null;
	List<Integer> threadList = null;
	public ThreadDialog(int stop) {
		threadList = new ArrayList<Integer>();
		texts = new ArrayList<String>();
		for(int c = 1; c <= stop; c++){
			threadList.add(c);
			StringBuilder builder = new StringBuilder();
			builder.append(c);
			builder.append(" threads");
			texts.add(builder.toString());
		}

		threadList.add(-1);
		texts.add("No limits");
		
	}
	/**
	 * Method called everytime user select an option. 
	 * @param dialog
	 * @param size
	 */
	public abstract void onListItemClick(DialogInterface dialog, Integer threads);
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.recognize_action_set_threads);
		builder.setItems(texts.toArray(new String[texts.size()]), new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onListItemClick(dialog, threadList.get(which));
			}
		});
		return builder.create();
	}
}
