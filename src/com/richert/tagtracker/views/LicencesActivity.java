package com.richert.tagtracker.views;

import org.opencv.android.OpenCVLoader;

import com.richert.tagtracker.R;
import com.richert.tagtracker.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class LicencesActivity extends Activity {
	private final static String opencvLicence="OpenCV:\nBy downloading, copying, installing or using the software you agree to this license.\nIf you do not agree to this license, do not download, install, copy or use the software.\nLicense Agreement\nFor Open Source Computer Vision Library\n(3-clause BSD License)\nRedistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:\nRedistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.\nRedistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.\nNeither the names of the copyright holders nor the names of the contributors may be used to endorse or promote products derived from this software without specific prior written permission.\nThis software is provided by the copyright holders and contributors “as is” and any express or implied warranties, including, but not limited to, the implied warranties of merchantability and fitness for a particular purpose are disclaimed. In no event shall copyright holders or contributors be liable for any direct, indirect, incidental, special, exemplary, or consequential damages (including, but not limited to, procurement of substitute goods or services; loss of use, data, or profits; or business interruption) however caused and on any theory of liability, whether in contract, strict liability, or tort (including negligence or otherwise) arising in any way out of\nthe use of this software, even if advised of the possibility of such damage.";
	private final static String androidSupportLibraryLicence="Android Support Library:\nCopyright (C) 2011 The Android Open Source Project\nLicensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License.\nYou may obtain a copy of the License at\n\nhttp://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing, software\ndistributed under the License is distributed on an \"AS IS\" BASIS,\nWITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\nSee the License for the specific language governing permissions and\nlimitations under the License.";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_licences);
		TextView textView = (TextView) findViewById(R.id.licences_text_view);
		textView.setText(opencvLicence+"\n\n"+androidSupportLibraryLicence);
		textView.setMovementMethod(new ScrollingMovementMethod());
	}

}
