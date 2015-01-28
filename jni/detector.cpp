#include "geometry.h"

class Detector{
private:
public:
	Detector(){

	}
	vector<Geometry::Point> findFeaturesFAST(Mat grayScene, int threshold){
		vector<Geometry::Point> result;
		vector<KeyPoint> keypoints;
		FAST(grayScene,keypoints,threshold,true);
		vector<KeyPoint>::iterator keypIt;
		for(keypIt = keypoints.begin(); keypIt != keypoints.end(); ++keypIt){
			Geometry::Point pointG;
			pointG.x = keypIt->pt.x;
			pointG.y = keypIt->pt.y;
			result.push_back(pointG);
		}
		return result;
	}

};
extern "C"{
JNIEXPORT jlong JNICALL Java_org_opencv_android_local_FeatureDetectorFast_newDetectorNtv(JNIEnv* env, jobject\
		){
	Detector*detector = new Detector();
	return (jlong)detector;
}
JNIEXPORT void JNICALL Java_org_opencv_android_local_FeatureDetectorFast_delDetectorNtv(JNIEnv* env, jobject\
		,jlong detectorAddr){
	if(detectorAddr != 0){
		Detector*detector = (Detector*)detectorAddr;
		delete detector;
	}
}

JNIEXPORT jobjectArray JNICALL Java_org_opencv_android_local_FeatureDetectorFast_findPointsFASTNtv(JNIEnv* env, jobject\
		,jlong detectorAddr,jlong addrYuv, jint threshold, jint rotation, jint w, jint h){
	Mat& mYuv = *(Mat*)addrYuv;
	Size imgSize(w,h);
	if(detectorAddr != 0){
		Detector*detector = (Detector*)detectorAddr;
		Mat& mYuv = *(Mat*)addrYuv;
		Mat mGray(mYuv.rows,mYuv.cols,CV_8UC3);
		cvtColor(mYuv,mGray,COLOR_YUV2GRAY_420);
		vector<Geometry::Point> points = detector->findFeaturesFAST(mGray, threshold);
		int size = points.size();
		if(size > 0 ){
			jobjectArray result = env->NewObjectArray(points.size(),jPointCls,points.at(0).toJava(env,rotation,imgSize));
			for(int c=1;c<points.size();c++){
				jobject p = points.at(c).toJava(env,rotation,imgSize);
				env->SetObjectArrayElement(result,c,p);
				env->DeleteLocalRef(p);
			}
			return result;
		}
		return NULL;
	}else{
		return NULL;
	}
}


}
