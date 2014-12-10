package com.richert.tagtracker.views;

import java.util.Arrays;
import java.util.List;

import com.richert.tagtracker.R;
import com.richert.tagtracker.R.id;
import com.richert.tagtracker.R.layout;
import com.richert.tagtracker.R.menu;
import com.richert.tagtracker.elements.MarkerArrayAdapter;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

public class MarkerGeneratorActivity extends Activity {
	private ListView listView;
	private final Integer ids[] = {
			R.drawable.tag1, R.drawable.tag2,
			R.drawable.tag3, R.drawable.tag4,
			R.drawable.tag5, R.drawable.tag6,
			R.drawable.tag7, R.drawable.tag8,
			R.drawable.tag9, R.drawable.tag10,
			R.drawable.tag11, R.drawable.tag12,
			R.drawable.tag13, R.drawable.tag14,
			R.drawable.tag15, R.drawable.tag16,
			R.drawable.tag17, R.drawable.tag18,
			R.drawable.tag19, R.drawable.tag20,
			R.drawable.tag21, R.drawable.tag22,
			R.drawable.tag23, R.drawable.tag24,
			R.drawable.tag25, R.drawable.tag26,
			R.drawable.tag27, R.drawable.tag28,
			R.drawable.tag29, R.drawable.tag30,
			R.drawable.tag31, R.drawable.tag32
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_markergenerator);
		listView = (ListView) findViewById(R.id.markergen_listview);
		MarkerArrayAdapter adapter = new MarkerArrayAdapter(this, Arrays.asList(ids));
		listView.setAdapter(adapter);
	}

}
