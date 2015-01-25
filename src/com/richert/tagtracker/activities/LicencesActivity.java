package com.richert.tagtracker.activities;

import java.nio.channels.ClosedSelectorException;

import org.opencv.android.OpenCVLoader;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.richert.tagtracker.R;
import com.richert.tagtracker.R.layout;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class LicencesActivity extends Activity {
	private final static String opencvLicence="OpenCV:\nBy downloading, copying, installing or using the software you agree to this license.\nIf you do not agree to this license, do not download, install, copy or use the software.\nLicense Agreement\nFor Open Source Computer Vision Library\n(3-clause BSD License)\nRedistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:\nRedistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.\nRedistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.\nNeither the names of the copyright holders nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.\nThis software is provided by the copyright holders and contributors “as is” and any express or implied warranties, including, but not limited to, the implied warranties of merchantability and fitness for a particular purpose are disclaimed. In no event shall copyright holders or contributors be liable for any direct, indirect, incidental, special, exemplary, or consequential damages (including, but not limited to, procurement of substitute goods or services; loss of use, data, or profits; or business interruption) however caused and on any theory of liability, whether in contract, strict liability, or tort (including negligence or otherwise) arising in any way out of\nthe use of this software, even if advised of the possibility of such damage.";
	private final static String androidSupportLibraryLicence="Android Support Library:\nCopyright (C) 2011 The Android Open Source Project\nLicensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License.\nYou may obtain a copy of the License at\n\nhttp://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing, software\ndistributed under the License is distributed on an \"AS IS\" BASIS,\nWITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\nSee the License for the specific language governing permissions and\nlimitations under the License.";
	private final static String TAG = LicencesActivity.class.getSimpleName();
	Thread worker;
	Boolean work = true;
	ZContext zContext;
	ZMQ.Socket publisher, subscriber;
	private String z_address = "tcp://0.0.0.0:6666";
	private String z_topic = "basic";
	LicencesActivity context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_licences);
		TextView textView = (TextView) findViewById(R.id.licences_text_view);
		textView.setText(opencvLicence+"\n\n"+androidSupportLibraryLicence);
		textView.setMovementMethod(new ScrollingMovementMethod());
		this.context = this;
		/*
		URL url = new URL("http://checkip.amazonaws.com/");
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		System.out.println(br.readLine());
		*/
		textView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Thread t = new Thread(new Runnable() {
					
					@Override
					public void run() {
						try{
							publisher.send(z_topic,  ZMQ.SNDMORE);
							publisher.send("hello world!", 0);
							Log.d(TAG, "succesfully sent data");
						}catch(ZMQException e){
							Log.e(TAG, "couldn't sent data");
							e.printStackTrace();
						}
						
					}
				});
		    	t.start();
			}
		});
	}
	@Override
	protected void onResume() {
		Thread preparator = new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					zContext = new ZContext();
					
					subscriber = zContext.createSocket(ZMQ.SUB);
					//subscriber.connect(z_address);
					subscriber.connect("tcp://192.168.1.149:6666");
					subscriber.subscribe(z_topic.getBytes());
					
					publisher = zContext.createSocket(ZMQ.PUB);
					publisher.bind(z_address);
					
					Log.d(TAG, "succesfully prepared sockets");
					work = true;
				}catch(ZMQException e){
					work = false;
					Log.e(TAG, "couldn't prepare sockets");
					e.printStackTrace();
				}
				
				
				
			}
		});
		preparator.start();
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				Log.d(TAG, "starting subscriber thread");
				worker = new Thread(new Runnable() {
					
					@Override
					public void run() {
						Log.d(TAG, "subscriber thread started");
						while(work){
							try{
								final String topic = subscriber.recvStr();
								if(z_topic.contentEquals(topic) && subscriber != null){
									final String message = new String(subscriber.recvStr());
									Log.d(TAG, "topic:"+topic+"\nmessage:"+message);
									context.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(context, "topic:"+topic+"\nmessage:"+message, Toast.LENGTH_SHORT).show();
										}
									});
								}
							}catch(ClosedSelectorException e){}catch(ZMQException e){}
							if(work){
								try{
									Thread.sleep(10);
								}catch(InterruptedException e){}
							}
						}
						Log.d(TAG, "stopping subscriber thread");
						
					}
				});
				worker.start();
			}
		}, 1000);
		
		super.onResume();
	}
	@Override
	protected void onPause() {
		Thread ender = new Thread(new Runnable() {
			public void run() {
				work = false;

		    	zContext.destroySocket(subscriber);
		    	zContext.destroySocket(publisher);
		    	zContext.destroy();
		        try {
					worker.join();
				} catch (InterruptedException e) {}
				Log.d(TAG, "stopping activity");
			}
		});
		ender.start();
		try {
			ender.join();
		} catch (InterruptedException e1) {}
		
		super.onPause();
	}

}
