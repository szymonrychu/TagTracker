package com.rychu.tagtracker.fragments;

import com.rychu.tagtracker.R;
import com.rychu.tagtracker.activities.ttstt.RobotSpeechProcessor;
import com.rychu.tagtracker.camera.Camera2DrawerPreview;
import com.rychu.tagtracker.camera.Camera2DrawerPreview.Camera2SetupCallback;
import com.rychu.tagtracker.opencv.Recognizer;
import com.rychu.tagtracker.opencv.Recognizer.TTSCallback;
import com.rychu.tagtracker.processing.LoadBalancer.InvalidStateException;
import com.rychu.tagtracker.usb.UsbConnection;
import com.rychu.tagtracker.usb.UsbRobotController;

import android.app.Fragment;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class RecognizerFragment extends Fragment implements Camera2SetupCallback{
	private View rootView;
	private Camera2DrawerPreview preview;
	private static final String TAG = RecognizerFragment.class.getSimpleName();
	private UsbRobotController usbRobotController;
	private Boolean usbConnect;
	private RobotSpeechProcessor speechProcessor;
	private Recognizer recognizer = null;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_driver_recognizer, container, false);
		preview = (Camera2DrawerPreview) rootView.findViewById(R.id.recognizer_camera2preview);
		usbRobotController = new UsbRobotController(getActivity());
		speechProcessor = new RobotSpeechProcessor(getActivity()){
			@Override
			public void setFollowedTag(int tagID) {
				Log.v(TAG, "following tag number: "+tagID);
				recognizer.setTrackedTagId(tagID);
			}
			@Override
			public void makeToast(final String text) {
				getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
					}
				});
				
			}
		};
		recognizer = new Recognizer(usbRobotController);
		recognizer.setTTSCallback(speechProcessor);
		return rootView;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		usbConnect = false;
		Intent intent = getActivity().getIntent();
		int chosenFragment = intent.getIntExtra("chosen_fragment", 0);
		if(!(chosenFragment > 0)){
			usbConnect = true;
		}
		super.onCreate(savedInstanceState);
	}
	@Override
	public void onResume() {
		if(usbConnect){
			usbRobotController.startWorking();
		}
		preview.setCamera2ProcessingCallback(recognizer);
		try {
			preview.startPreview(this);
			preview.setMaxPoolSize(4);
			preview.setMaxThreadsNum(4);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}catch (InvalidStateException e) {
			e.printStackTrace();
		}
		super.onStart();
	}
	@Override
	public void onPause() {
		try {
			preview.stopPreview();
		}catch (InvalidStateException e) {}

		if(usbConnect){
			try {
				usbRobotController.stopWorking();
			} catch (InterruptedException e) {}
		}
		super.onStop();
	}
	
	@Override
	public Builder setupCamera(CameraDevice cameraDevice) {
		CaptureRequest.Builder previewBuilder = null;
		try {
			previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF_KEEP_STATE);
			
			previewBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO);
			previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
			
			previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO);

			previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
			previewBuilder.set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_HIGH_QUALITY);
			previewBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);
			previewBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			previewBuilder.set(CaptureRequest.SHADING_MODE, CameraMetadata.SHADING_MODE_HIGH_QUALITY);
			previewBuilder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_HIGH_QUALITY);

			previewBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON);
			
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
		//previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
		
		return previewBuilder;
	}
	@Override
	public void onCameraOpen(CameraCharacteristics characteristics) {
		// TODO Auto-generated method stub
	}
	@Override
	public Size setCameraPreviewSize(int viewWidth, int viewHeight, Size[] sizes) {
		final int MAX_WIDTH = 4000;
        final float TARGET_ASPECT = 16.f / 9.f;
        final float ASPECT_TOLERANCE = 0.2f;
        Size outputSize = sizes[0];
        float outputAspect = (float) outputSize.getWidth() / outputSize.getHeight();
		for (Size candidateSize : sizes) {
			//Log.v(TAG, "preview size candidate: "+candidateSize);
            if (candidateSize.getWidth() > MAX_WIDTH) continue;
            float candidateAspect = (float) candidateSize.getWidth() / candidateSize.getHeight();
            boolean goodCandidateAspect =
                    Math.abs(candidateAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            boolean goodOutputAspect =
                    Math.abs(outputAspect - TARGET_ASPECT) < ASPECT_TOLERANCE;
            if ((goodCandidateAspect && !goodOutputAspect) ||
                    candidateSize.getWidth() > outputSize.getWidth()) {
                outputSize = candidateSize;
                outputAspect = candidateAspect;
            }
        }
		return outputSize;
	}
	@Override
	public void onError(int error) {
		// TODO Auto-generated method stub
	}
	
}
