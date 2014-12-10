#include "geometry.h"



class CameraCalibrator{
private:
	Size boardSize;
	bool boardFound;
	vector<Point2f> imageCorners;
	vector<Mat> trainingSet;
	vector<Mat> rvecs, tvecs;//Mat rvecs, tvecs;
	vector<vector<Point3f> > objectPoints;
	vector<vector<Point2f> > imagePoints;
	Size imageSize;
    vector<Point3f> objectCorners;
	Mat cameraMatrix, distCoeffs;
	int flag;
	double xxx;
	float squareLenght;
	bool calibrated, addFrame;
public:
    CameraCalibrator(int height, int width){
    	this->squareLenght = 36.0f;
    	this->xxx = 0;
    	this-> flag = CALIB_FIX_K3;
    	this->calibrated = false;
    	this->boardSize.height = height;
    	this->boardSize.width = width;
    	this->boardFound=false;
    	this->addFrame= false;
    	this->cameraMatrix = Mat::eye(3, 3, CV_64F);
    	this->distCoeffs = Mat::zeros(5, 1, CV_64F);

    	/*this->rvecs = Mat();
    	this->tvecs = Mat();*/
    	for(int x = 0; x<boardSize.height; x++){
			for(int y=0;y<boardSize.width;y++){
				objectCorners.push_back(cv::Point3f(float(x)*squareLenght,float(y)*squareLenght,0.0f));
			}
		}

    }
    ~CameraCalibrator(){

    }
    vector<Point2f> processImage(Mat &grayFrame){
    	boardFound = findChessboardCorners(grayFrame,boardSize,imageCorners);


    	if(addFrame){
    		logD("processImage(Mat grayFrame,JNIEnv*env,int rotation)","adding frame to set!");
    		addFrame = false;
    		imageSize = grayFrame.size();
            cornerSubPix(grayFrame, imageCorners, Size(5,5),Size(-1,-1),
                TermCriteria(TermCriteria::MAX_ITER+TermCriteria::EPS,30,0.1));
            imagePoints.push_back(imageCorners);
            objectPoints.push_back(objectCorners);
    	}
    	if(boardFound){
    		logD("processImage(Mat grayFrame,JNIEnv*env,int rotation)","board found!");

    	}
    	return imageCorners;
    }
    void addFrameToSet(){
    	addFrame = true;
    	/*if(boardFound){
			logD("addFrameToSet()","adding to set");
            /*cornerSubPix(grayFrame, imageCorners, Size(5,5),Size(-1,-1),
                TermCriteria(TermCriteria::MAX_ITER+TermCriteria::EPS,30,0.1));*/
            /*imagePoints.push_back(imageCorners);
            objectPoints.push_back(objectCorners);
    	}*/
    }
    void processFrames(){
		logD("processFrames()","processing frames");
    	if(imagePoints.size()>0){
    		logD("processFrames()","imagePoints.size() > 0");
    		xxx = calibrateCamera(objectPoints, //the 3D points
    		                imagePoints,
    		                imageSize,
    		                cameraMatrix, //output camera matrix
    		                distCoeffs,
    		                rvecs,tvecs,
    		                flag);

			calibrated  = checkRange(cameraMatrix) && checkRange(distCoeffs) ;
			if(calibrated){
				logD("processFrames()","calibrated = true");
			}
			objectPoints.clear();
			imagePoints.clear();
    	}
    }
    jobject getCameraMatrix(JNIEnv* env){
		if(calibrated){
			jobject jMatPic = env->NewObject(jMatCls,jMatConsID);
			logD("getCameraMatrix(JNIEnv* env)","new Mat Java object");
			Mat&cMat=*(Mat*)env->CallLongMethod(jMatPic,jMatGetNatAddr);
			logD("getCameraMatrix(JNIEnv* env)","cloning data to java object");
			cMat = cameraMatrix.clone();
			logD("getCameraMatrix(JNIEnv* env)","returning mat");
			return jMatPic;
		}else{
			logD("getCameraMatrix(JNIEnv* env)","returning NULL");
			return NULL;
		}
    }
    jobject getDistortionCoefficient(JNIEnv* env){
    	if(calibrated){
			jobject jMatPic = env->NewObject(jMatCls,jMatConsID);
			Mat&cMat=*(Mat*)env->CallLongMethod(jMatPic,jMatGetNatAddr);
			cMat = distCoeffs.clone();
			return jMatPic;
		}else{
			return NULL;
		}
    }

};



extern "C" {
JNIEXPORT jlong JNICALL Java_org_opencv_android_local_Calibrator_newCalibratorNtv(JNIEnv* env, jobject\
		, jint x, jint y){
	CameraCalibrator*calibrator = new CameraCalibrator(x,y);
	return (jlong)calibrator;
}
JNIEXPORT void JNICALL Java_org_opencv_android_local_Calibrator_delCalibratorNtv(JNIEnv* env, jobject\
		,jlong calibratorAddr){
	if(calibratorAddr != 0){

		CameraCalibrator*calibrator = (CameraCalibrator*)calibratorAddr;
		delete calibrator;
	}
}
JNIEXPORT jobjectArray JNICALL Java_org_opencv_android_local_Calibrator_detectChessBoardNtv(JNIEnv* env, jobject\
		,jlong calibratorAddr,jlong addrYuv, jint rotation){
    Mat& mYuv = *(Mat*)addrYuv;
	if(calibratorAddr != 0){
		CameraCalibrator&calibrator = *(CameraCalibrator*)calibratorAddr;
		Mat mGray(mYuv.rows,mYuv.cols,CV_8UC1);
		cvtColor(mYuv,mGray,COLOR_YUV2GRAY_NV21);
		vector<Point2f> imageCorners = calibrator.processImage(mGray);
		if(imageCorners.size() > 0){
			unsigned int size = imageCorners.size();
			Geometry::Point initialPoint;
			jobjectArray jPointResult = env->NewObjectArray(size,jPointCls,initialPoint.toJava(env,rotation,Size(mGray.cols, mGray.rows)));
			for(int count=0;count<size;count++){
				Geometry::Point point;
				if(rotation%2==1){
					point.x = mGray.rows - (int)imageCorners[count].y;
					point.y = (int)imageCorners[count].x;
				}else{
					point.x = (int)imageCorners[count].x;
					point.y = (int)imageCorners[count].y;
				}
				env->SetObjectArrayElement(jPointResult,count,point.toJava(env,rotation,Size(mGray.cols, mGray.rows)));
			}
			logD("detectChessBoardNtv","returning array : %d",size);
			return jPointResult;
		}else{
			return NULL;
		}
	}else{
		return NULL;
	}
}

JNIEXPORT void JNICALL Java_org_opencv_android_local_Calibrator_addFrameToSetNtv(JNIEnv* env, jobject\
		,jlong calibratorAddr){
	if(calibratorAddr != 0){
		CameraCalibrator*calibrator = (CameraCalibrator*)calibratorAddr;
		calibrator->addFrameToSet();
	}
}
JNIEXPORT void JNICALL Java_org_opencv_android_local_Calibrator_processFramesNtv(JNIEnv* env, jobject\
		,jlong calibratorAddr){
	if(calibratorAddr != 0){
		CameraCalibrator*calibrator = (CameraCalibrator*)calibratorAddr;
		calibrator->processFrames();
	}
}
JNIEXPORT jobject JNICALL Java_org_opencv_android_local_Calibrator_getCameraMatrixNtv(JNIEnv* env, jobject\
		,jlong calibratorAddr){
	if(calibratorAddr != 0){
		CameraCalibrator*calibrator = (CameraCalibrator*)calibratorAddr;
		return calibrator->getCameraMatrix(env);
	}else{
		return NULL;
	}
}
JNIEXPORT jobject JNICALL Java_org_opencv_android_local_Calibrator_getDistortionCoefficientNtv(JNIEnv* env, jobject\
		,jlong calibratorAddr){
	if(calibratorAddr != 0){
		CameraCalibrator*calibrator = (CameraCalibrator*)calibratorAddr;
		return calibrator->getDistortionCoefficient(env);
	}else{
		return NULL;
	}
}
}

