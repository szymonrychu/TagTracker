package com.richert.tagtracker.elements;

import java.util.List;

import com.richert.tagtracker.R;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MarkerArrayAdapter extends ArrayAdapter<Integer> {
	private final static String TAG = MarkerArrayAdapter.class.getSimpleName();
	private final Context context;
	private final List<Integer> data;
	public static class ViewHolder{
		public TextView text;
		public ImageView image;
	}
	public MarkerArrayAdapter(Context context, List<Integer> data) {
		super(context,R.layout.activity_markergenerator_listitem, data);
		this.context = context;
		this.data = data;
		// TODO Auto-generated constructor stub
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if(convertView == null){
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
			convertView = inflater.inflate(R.layout.activity_markergenerator_listitem, null);
			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.marker_text_view);
			holder.image = (ImageView) convertView.findViewById(R.id.marker_image_view);
			convertView.setTag(holder);
		}
		holder = (ViewHolder) convertView.getTag();
		String markerName = "Marker numer " + (position +1);
		holder.text.setText(markerName);
		holder.image.setImageResource(data.get(position));
		return convertView;
	}
}
