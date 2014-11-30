#ifndef HELPER_H
#define HELPER_H

#include <jni.h>
#include <vector>
#include <string>
#include <math.h>
#include <sys/time.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/contrib/detection_based_tracker.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/calib3d/calib3d.hpp>

#include "opencv2/objdetect/objdetect.hpp"


using namespace cv;
using namespace std;

bool debug = true;


#define APPNAME "Native"

extern "C" {
	static jclass jMatCls;
	static jmethodID jMatGetNatAddr;
	static jmethodID jMatConsID;
	static jclass jPointCls;
	static jmethodID jPointConsID;
	static jclass jTagCls;
	static jmethodID jTagConsID;

	jint JNI_OnLoad(JavaVM* vm, void* reserved){
		JNIEnv* env;
		if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
			return -1;
		}
		jclass jMatTmp = env->FindClass("org/opencv/core/Mat");
		jMatCls = (jclass)env->NewGlobalRef(jMatTmp);
		jMatGetNatAddr = env->GetMethodID(jMatCls,"getNativeObjAddr","()J");
		jMatConsID = env->GetMethodID(jMatCls,"<init>","()V");

		jclass jPointTmp = env->FindClass("com/richert/tagtracker/geomerty/Point");
		jPointCls = (jclass)env->NewGlobalRef(jPointTmp);
		jPointConsID = env->GetMethodID(jPointCls,"<init>","()V");

		jclass jTagTmp = env->FindClass("com/richert/tagtracker/geomerty/Tag");
		jTagCls = (jclass)env->NewGlobalRef(jTagTmp);
		jTagConsID = env->GetMethodID(jPointCls,"<init>","()V");


		return JNI_VERSION_1_6;
	}
}

void rotate(cv::Mat& src, cv::Mat& dst, double angle){
	Point2f src_center(src.cols/2.0F, src.rows/2.0F);
	Mat rot_mat = getRotationMatrix2D(src_center, angle, 1.0);
	warpAffine(src, dst, rot_mat, src.size());
}
void logD(const string tag, const string frmt, ...){
	va_list ap;
	va_start(ap, frmt);
	if(debug)__android_log_vprint(ANDROID_LOG_DEBUG, tag.c_str(), frmt.c_str(), ap);
}
void logV(const string tag, const string frmt, ...){
	va_list ap;
	va_start(ap, frmt);
	if(debug)__android_log_vprint(ANDROID_LOG_VERBOSE, tag.c_str(), frmt.c_str(), ap);
}
void logE(const string tag, const string frmt, ...){
	va_list ap;
	va_start(ap, frmt);
	if(debug)__android_log_vprint(ANDROID_LOG_ERROR, tag.c_str(), frmt.c_str(), ap);
}
void logW(const string tag, const string frmt, ...){
	va_list ap;
	va_start(ap, frmt);
	if(debug)__android_log_vprint(ANDROID_LOG_WARN, tag.c_str(), frmt.c_str(), ap);
}
void rotateMat(Mat &mat, int rotation){
	if(rotation%2!=1){
		flip(mat.t(),mat, 1);
	}
}
static jobject mat2JMat(JNIEnv*env, Mat mat){
	jobject jMat = env->NewObject(jMatCls,jMatConsID);
	Mat&cMat=*(Mat*)env->CallLongMethod(jMat,jMatGetNatAddr);
	cMat = mat;
	return jMat;
}
#endif
