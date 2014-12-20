#include "geometry.h"
using namespace std;
using namespace cv;

class Recognizer{
private:
	bool sizeSet;
	bool processImages;
	vector<Mat> tags;
	int outline;
	Size imageSize;
	int rotation;
	Point2f norm2DPts[4];
	int tagSize, tagKernel;
	struct PointStr{
		int x;
		int y;
	};
	vector<Geometry::Tag> tagz;
	int fieldNum;
	int getTagId(Mat normTag){
		int result=0;
		switch(this->rotation){
		case(ROTATION_PORTRAIT):
			for(int C=0;C<tagSize-1;C++){
				for(int R=4;R>=0;R--){
					int r=R*tagKernel;
					int c=C*tagKernel;
					Mat roi = normTag(Range(r,r+tagKernel-1),Range(c,c+tagKernel-1));
					Scalar res = mean(roi);
					if((R==0 || C==0 || R==tagSize-1 || C==tagSize-1)){
						if(res.val[0]>122){
							return -1;
						}
					}else{
						if(R==1 && C==1){
							if(res.val[0]>122){
								return -1;
							}
						}else if(R==1 && C==tagSize-2){
							if(res.val[0]>122){
								return -1;
							}
						}else if(R==tagSize-2 && C==1){
							if(res.val[0]<122){
								return -1;
							}
						}else if(R==tagSize-2 && C==tagSize-2){
							if(res.val[0]>122){
								return -1;
							}
						}else{
							result=result*2;
							if(res.val[0]<122){
								result+=1;
							}
						}
					}
				}
			}
		break;
		case(ROTATION_LANDSCAPE):
			for(int R=0;R<tagSize;R++){
				for(int C=0;C<tagSize;C++){
					int r=R*tagKernel;
					int c=C*tagKernel;
					Mat roi = normTag(Range(r,r+tagKernel-1),Range(c,c+tagKernel-1));
					Scalar res = mean(roi);
					if((R==0 || C==0 || R==tagSize-1 || C==tagSize-1)){
						if(res.val[0]>122){
							return -1;
						}
					}else if(R==1 && C==1){
						if(res.val[0]<122){
							return -1;
						}
					}else if(R==1 && C==tagSize-2){
						if(res.val[0]>122){
							return -1;
						}
					}else if(R==tagSize-2 && C==1){
						if(res.val[0]>122){
							return -1;
						}
					}else if(R==tagSize-2 && C==tagSize-2){
						if(res.val[0]>122){
							return -1;
						}
					}else{
						result=result*2;
						if(res.val[0]<122){
							result+=1;
						}
					}
				}
			}
		break;
		case(ROTATION_LANDS_UPSIDE_DOWN):
			for(int R=tagSize-1;R>=0;R--){
				for(int C=tagSize-1;C>=0;C--){
					int r=R*tagKernel;
					int c=C*tagKernel;
					Mat roi = normTag(Range(r,r+tagKernel-1),Range(c,c+tagKernel-1));
					Scalar res = mean(roi);
					if((R==0 || C==0 || R==tagSize-1 || C==tagSize-1)){
						if(res.val[0]>122){
							return -1;
						}
					}else if(R==1 && C==1){
						if(res.val[0]>122){
							return -1;
						}
					}else if(R==1 && C==tagSize-2){
						if(res.val[0]>122){
							return -1;
						}
					}else if(R==tagSize-2 && C==1){
						if(res.val[0]>122){
							return -1;
						}
					}else if(R==tagSize-2 && C==tagSize-2){
						if(res.val[0]<122){
							return -1;
						}
					}else{
						result=result*2;
						if(res.val[0]<122){
							result+=1;
						}
					}
				}
			}
		break;
		}
		return result+1;
	}
	double minOutline, maxOutline;
public:
	bool cornerSubP;
	bool dilatE;
	bool kalmanFilter;
	bool preview;
	double maxTagSize, minTagSize;
    int blockSize;
    double adaptThresh;
    int normSize;
	Recognizer(){
		this->processImages = false;
		this->sizeSet = false;
		this->imageSize = Size(0,0);
		this->outline = 0;
		this->preview = true;
		this->rotation = 0;





		this->kalmanFilter = false;
		this->cornerSubP = true;
		this->dilatE = true;
		this->preview = false;
		this->maxTagSize = 0.8;
		this->minTagSize = 0.01;
		this->blockSize = 45;
		this->adaptThresh = 7.0;
		this->normSize = 25;

		this->norm2DPts[0] = Point2f(0,0);
		this->norm2DPts[1] = Point2f(normSize-1,0);
		this->norm2DPts[2] = Point2f(normSize-1,normSize-1);
		this->norm2DPts[3] = Point2f(0,normSize-1);
	}
	~Recognizer(){

	}
	int getRotation(){
		return this->rotation;
	}
	Size getImageSize(){
		return this->imageSize;
	}
	void insight(Mat&mYuv){
		Mat mGray(mYuv.rows,mYuv.cols,CV_8UC3);
		cvtColor(mYuv,mGray,COLOR_YUV2GRAY_NV21);
		if(!sizeSet){
			return ;
		}
		Mat mBin;
		adaptiveThreshold(mGray,mBin,255,CV_ADAPTIVE_THRESH_GAUSSIAN_C,CV_THRESH_BINARY_INV,blockSize,adaptThresh);
		if(dilatE){
			dilate(mBin,mBin,Mat());
		}
		vector<vector<Point> > contours;
	    findContours(mBin.clone(),contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
	    unsigned int size = 0;
	    vector<vector<Point> >::iterator itVVP;
	    for(itVVP = contours.begin(); itVVP!=contours.end(); itVVP++){
	    	double len = arcLength(*itVVP,true);
	    	if(len > minOutline && len < maxOutline){
	    		vector<Point> polygon;
	    		approxPolyDP(*itVVP,polygon,len*0.02,true);
	    		if(polygon.size()==4 && isContourConvex(Mat (polygon))){
	    			int c = 0;
	    			for(int c=0;c<4;c++){
		    			line(mGray,polygon[c],polygon[(c+1)%4],Scalar(255,122,122),3);
	    			}
	    		}
	    	}
	    }
	    mYuv = mGray;
	}
	vector<Geometry::Tag> recognizeTags(Mat mYuv){
	    vector<Geometry::Tag> result;
		if(!sizeSet){
			return result;
		}
		int c = 0;
		/*vector<Geometry::Tag> tags;
		vector<Geometry::Tag>::iterator tagIt;
		for(tagIt = tagz.begin();kalmanFilter && tagIt!=tagz.end();tagIt++){
			Geometry::Tag tmp = *tagIt;
			tmp.draw = false;
			tags.push_back(tmp);
		}*/

		Mat mGray(mYuv.rows,mYuv.cols,CV_8UC3);
		cvtColor(mYuv,mGray,COLOR_YUV2GRAY_NV21);
		Mat mBin;
		adaptiveThreshold(mGray.clone(),mBin,255,CV_ADAPTIVE_THRESH_MEAN_C,CV_THRESH_BINARY_INV,blockSize,adaptThresh);

		if(dilatE){
			dilate(mBin,mBin,Mat());
		}
		vector<vector<Point> > contours;
	    findContours(mBin.clone(),contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
	    unsigned int size = 0;
	    vector<vector<Point> >::iterator itVVP;
	    for(itVVP = contours.begin(); itVVP!=contours.end(); itVVP++){
	    	double len = arcLength(*itVVP,true);
	    	if(len > minOutline && len < maxOutline){
	    		vector<Point> polygon;
	    		approxPolyDP(*itVVP,polygon,len*0.02,true);
	    		if(polygon.size()==4 && isContourConvex(Mat (polygon))){
	    			int minY, maxY, minX, maxX;
	    			minY = maxY = polygon.at(0).y;
	    			minX = maxX = polygon.at(0).x;
	    		    vector<cv::Point>::iterator itVP;
    				vector<Point2f> refinedVertices;
    				int counter=0;
    				double d;
    				double dmin=(4*outline);
    				int v1=-1;
	    			for(itVP = polygon.begin(); itVP!=polygon.end();itVP++,counter++){
	    				if(itVP->x>maxX){
	    					maxX = itVP->x;
	    				}
	    				if(itVP->x<minX){
	    					minX = itVP->x;
	    				}
	    				if(itVP->y>maxY){
	    					maxY = itVP->y;
	    				}
	    				if(itVP->y<minY){
	    					minY = itVP->y;
	    				}
						refinedVertices.push_back(*itVP);
						d = norm(*itVP);
						if (d<dmin) {
							dmin=d;
							v1=counter;
						}
	    			}
	    			Rect roi(minX, minY, maxX-minX+1, maxY-minY+1);
	    			if(this->cornerSubP){
	    				cornerSubPix(mGray, refinedVertices, Size(3,3), Size(-1,-1), TermCriteria(1, 3, 1));
	    			}
    	    		Point2f roi2DPts[4];
					for(counter=0; counter<4;counter++){
						roi2DPts[counter] = Point2f(refinedVertices.at((4+v1-counter)%4).x - minX, refinedVertices.at((4+v1-counter)%4).y - minY);
					}
    	    		Mat homo(3,3,CV_32F);
    				homo = getPerspectiveTransform(roi2DPts,norm2DPts);
    	    		Mat subImg = mGray(cv::Range(roi.y, roi.y+roi.height), cv::Range(roi.x, roi.x+roi.width));
    				Mat normROI = Mat(normSize,normSize,CV_8UC1);
    				warpPerspective( subImg, normROI, homo, Size(normSize,normSize));
    	    		int id = getTagId(normROI);
    				if(id!=-1){
						Geometry::Tag tag;
						tag.id = id;
						tag.preview = normROI;
						tag.setPoints(refinedVertices,rotation,Size(mGray.cols,mGray.rows), len);
						result.push_back(tag);
    					/*Log::d("tag number", "tag id=%d",id-1);
						if(kalmanFilter){
							tags[id-1].preview = normROI;
							tags[id-1].setPointsKalman(refinedVertices,rotation,Size(mGray.cols,mGray.rows), len);
						}else{
							//TODO
						}*/
    				}
	    		}
	    	}
	    }
		/*or(tagIt = tags.begin();kalmanFilter && tagIt!=tags.end();tagIt++){
			Geometry::Tag tag = *tagIt;
			if(tag.draw){
				result.push_back(tag);
			}
		}*/
		return result;
	}
	void notifyImageSizeChanged(Size newSize,int rotation, int tagSize){
		this->tagSize = tagSize;
		this->tagKernel = normSize / tagSize;
		double scaleW = imageSize.width / newSize.width;
		double scaleH = imageSize.height / newSize.height;
		this->processImages = false;
		this->imageSize = newSize;
		this->rotation = rotation;
		this->outline = (newSize.width + newSize.height) * 2;
		minOutline = minTagSize*outline;
		maxOutline = maxTagSize*outline;
		this->fieldNum = (tagSize-2)*(tagSize-4)+2*(tagSize-4);
		tagz.clear();
		for(int c=0;c<fieldNum;c++){
			Geometry::Tag t;
			t.id = c+1;
			tagz.push_back(t);
		}
		this->sizeSet = true;
	}


};
extern "C"{
JNIEXPORT jlong JNICALL Java_org_opencv_android_local_RecognizerService_newRecognizerNtv(JNIEnv* env, jobject\
		){
    Recognizer*recognizer = new Recognizer();
	return (jlong)recognizer;
}
JNIEXPORT void JNICALL Java_org_opencv_android_local_RecognizerService_delRecognizerNtv(JNIEnv* env, jobject\
		,jlong calibratorAddr){
	if(calibratorAddr != 0){
		Recognizer*recognizer = (Recognizer*)calibratorAddr;
		delete recognizer;
	}
}
JNIEXPORT jobjectArray JNICALL Java_org_opencv_android_local_RecognizerService_findTagsNtv(JNIEnv* env, jobject\
		,jlong recognizerAddr,jlong addrYuv){
	Mat& mYuv = *(Mat*)addrYuv;
	if(recognizerAddr != 0){
		Recognizer*recognizer = (Recognizer*)recognizerAddr;
		vector<Geometry::Tag> tags = recognizer->recognizeTags(mYuv);
		int rotation = recognizer->getRotation();
		Size imgSize = recognizer->getImageSize();
		if(tags.size()>0){
			jobjectArray result = env->NewObjectArray(tags.size(),jTagCls,tags.at(0).toJava(env,rotation,imgSize));
			for(int c=1;c<tags.size();c++){
				env->SetObjectArrayElement(result,c,tags.at(c).toJava(env,rotation,imgSize));
			}
			return result;
		}else{
			return NULL;
		}
	}else{
		return NULL;
	}
}
JNIEXPORT void JNICALL Java_org_opencv_android_local_RecognizerService_notifySizeChangedNtv(JNIEnv* env, jobject\
		,jlong recognizerAddr,jint width, jint height, jint rotation){
	if(recognizerAddr != 0){
		Recognizer*recognizer = (Recognizer*)recognizerAddr;
		Size size(width,height);
		recognizer->notifyImageSizeChanged(size, rotation, 5);
	}
}
JNIEXPORT void JNICALL Java_org_opencv_android_local_RecognizerService_insightNtv(JNIEnv* env, jobject\
		,jlong recognizerAddr,jlong addrYuv){
	if(recognizerAddr != 0){
		Mat& mYuv = *(Mat*)addrYuv;
		Recognizer*recognizer = (Recognizer*)recognizerAddr;
		recognizer->insight(mYuv);
	}
}


}
