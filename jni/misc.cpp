#include "helper.h"

using namespace std;
using namespace cv;

#define APPNAME "Native"

extern "C" {
JNIEXPORT jobject JNICALL Java_org_opencv_android_local_Misc_yuvToRgbNtv(JNIEnv* env, jobject\
		, jlong addrYuv, jint rotation){
    Mat& mYuv = *(Mat*)addrYuv;
    Mat mRGB(mYuv.rows,mYuv.cols,CV_8UC3);
    cvtColor(mYuv,mRGB,COLOR_YUV2RGB_NV21);
    jobject jMat = env->NewObject(jMatCls,jMatConsID);
    Mat&cMat=*(Mat*)env->CallLongMethod(jMat,jMatGetNatAddr);
    rotateMat(mRGB,rotation);
    cMat = mRGB;
    return jMat;
}


}

