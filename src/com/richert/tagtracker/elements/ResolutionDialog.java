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

public abstract class ResolutionDialog extends DialogFragment{
	List<String> resolutions = null;
	List<Camera.Size> sizes;
	public ResolutionDialog(List<Camera.Size> sizes) {
		this.sizes = sizes;
		resolutions = new ArrayList<String>();
		for(Camera.Size size : sizes){
			StringBuilder resStrBuilder = new StringBuilder();
			resStrBuilder.append(size.width);
			resStrBuilder.append(":");
			resStrBuilder.append(size.height);
			resolutions.add(resStrBuilder.toString());
		}
	}
	/**
	 * Method called everytime user select an option. 
	 * @param dialog
	 * @param size
	 */
	public abstract void onListItemClick(DialogInterface dialog, Camera.Size size);
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.recognize_action_resolution);
		builder.setItems(resolutions.toArray(new String[resolutions.size()]), new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onListItemClick(dialog, sizes.get(which));
			}
		});
		return builder.create();
	}
}
