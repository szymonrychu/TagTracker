#ifndef GEOMETRY_H
#define GEOMETRY_H

#include "helper.h"

static const int ROTATION_LANDSCAPE=1;
static const int ROTATION_PORTRAIT=0;
static const int ROTATION_PORTR_UPSIDE_DOWN=2;
static const int ROTATION_LANDS_UPSIDE_DOWN=3;



class Geometry{
private:
public:
	class Point{
	public:
		Point(){
			this->x = 0;
			this->y = 0;
		}
		int x;
		int y;
		jobject toJava(JNIEnv* env, int rotation, Size matSize){
			jfieldID xID = env->GetFieldID(jPointCls,"x","F");
			jfieldID yID = env->GetFieldID(jPointCls,"y","F");
			jobject result = env->NewObject(jPointCls,jPointConsID);
			switch(rotation){
			case(ROTATION_LANDSCAPE):
				env->SetFloatField(result,xID,((float)x/matSize.width));
				env->SetFloatField(result,yID,((float)y/matSize.height));
			break;
			case(ROTATION_PORTRAIT):
				env->SetFloatField(result,xID,((float)(matSize.height - y)/(matSize.width)));
				env->SetFloatField(result,yID,((float)x/(matSize.height)));
			break;
			case(ROTATION_LANDS_UPSIDE_DOWN):
				env->SetFloatField(result,xID,((float)(matSize.width - x)/matSize.width));
				env->SetFloatField(result,yID,((float)(matSize.height - y)/matSize.height));
			break;
			case(ROTATION_PORTR_UPSIDE_DOWN):
				env->SetFloatField(result,xID,((float)y/matSize.height));
				env->SetFloatField(result,yID,((float)(matSize.width - x)/matSize.width));
			break;
			}
			return result;
		}
	};
	class Kalman{
	private:
		Mat X; // state matrix
		Mat Y; //*innovation matrix
		Mat Z; // measurement vector
		Mat P; // average error for each part of the state
		Mat B; // control matrix
		Mat U; // control vector
		Mat Q; // estimated error covariance
		Mat S; //*innovation covariance matrix
		Mat R; // estimated measurement error covariance
		Mat A; // step transition matrix
		Mat K; //*kalman gain
		Mat H; // observation matrix


		bool inited;
		Mat tmp1,tmp2,tmp3;
		Mat diag;
	public:
		void init(float startX, float startY){
			Log::d("init","----");
			X = Mat::zeros(Size(4,1), CV_32FC1);
			X.at<float>(0,0) = startX;
			X.at<float>(2,0) = startY;
			Log::d("init","X");
			A = Mat(Size(4,4), CV_32FC1);
			A = Mat::diag(A);
			Log::d("init","A");
			B = Mat(Size(4,4), CV_32FC1);
			B.at<float>(2,2)=1;
			B.at<float>(3,3)=1;
			Log::d("init","B");
			U = Mat::zeros(Size(4,1), CV_32FC1);
			Log::d("init","U");
			H = Mat(Size(4,4), CV_32FC1);
			H = Mat::diag(H);
			Log::d("init","H");
			P = Mat(Size(4,4), CV_32FC1);
			Mat::diag(P);
			Log::d("init","P");
			Q = Mat::zeros(Size(4,4), CV_32FC1);
			Log::d("init","Q");
			R = Mat(Size(4,4), CV_32FC1);
			Mat::diag(R);
			R = R*0.1;
			Log::d("init","R");
			diag = Mat(Size(4,4), CV_32FC1);
			Mat::diag(diag);
			Log::d("init","diag");
			Z = Mat(Size(4,1), CV_32FC1);
			Log::d("init","Z");
			inited = true;
		}
		Kalman(){}
		Kalman(float startX, float startY){
			this->init(startX, startY);
		}
		Geometry::Point newPrediction(float dT){
			Log::d("newPrediction","big entrance");
			Geometry::Point res;
			if(inited){
				A.at<float>(0,1) = dT;
				A.at<float>(2,3) = dT;
				Log::d("newPrediction","A assigned");
				X = A*X + B*U;
				Log::d("newPrediction","new X processed");
				transpose(A,tmp1);
				Log::d("newPrediction","A transposed");
				P = A*P*tmp1+Q;
				Log::d("newPrediction","P processed");
				res.x = X.at<float>(0,0);
				res.y = X.at<float>(0,1);
				Log::d("newPrediction","prediction counted");
			}
			return res;
		}
		void correction(float x, float y){
			Z.at<float>(0,0) = x;
			Z.at<float>(2,0) = y;
			Log::d("correction","Z filled");
			Y = Z - H*X;
			Log::d("correction","Y processed");
			transpose(H,tmp2);
			Log::d("correction","H transposed");
			S = H*P*tmp2+R;
			Log::d("correction","S processed");
			invert(S,tmp3);
			Log::d("correction","S inverted");
			K = P*tmp2*tmp3;
			Log::d("correction","K processed");
			X=X+K*Y;
			Log::d("correction","X processed");
			P=(diag - K*H)*P;
			Log::d("correction","P processed");
		}

	};
	class Tag{
	private:
		Kalman kalman;
		int maxSkipped;
	public:
		int id;
		Mat preview;
		Mat homo;
		vector<Geometry::Point> points;
		Point center;
		double l;
		int skipped;
		bool draw;
		Tag(){
			draw = false;
			kalman = Kalman();
			this->skipped = 5;
			this->maxSkipped = 5;
		}
		Tag(int maxSkipped){
			draw = false;
			kalman = Kalman();
			this->skipped = maxSkipped;
			this->maxSkipped = maxSkipped;
		}
		void setPoints(vector<Point2f> points,int rotation, Size matSize, double l){
			if(!this->draw)
				this->draw = true;
			skipped = 0;
			vector<Point2f>::iterator it;
			int X = 0;
			int Y = 0;
			int counter = 0;
			for(it=points.begin();it!=points.end();it++){
				Point point;
				point.x = it->x;
				point.y = it->y;
				this->points.push_back(point);
				this->l = l;
				X += point.x;
				Y += point.y;
				counter++;
			}
			this->center.x = X/counter;
			this->center.y = Y/counter;
		}
		void setPointsKalman(vector<Point2f> points,int rotation, Size matSize, double l){
			vector<Point2f>::iterator it;
			int X = 0;
			int Y = 0;
			int counter = 0;
			for(it=points.begin();it!=points.end();it++){
				Point point;
				point.x = it->x;
				point.y = it->y;
				this->points.push_back(point);
				this->l = l;
				X += point.x;
				Y += point.y;
				counter++;
			}
			if(!this->draw){
				Log::d("setPointsKalman","kalman pre init");
				this->kalman.init(X/counter, Y/counter);
				Log::d("setPointsKalman","kalman post init");
				this->draw = true;
			}else{
				kalman.correction(X/counter, Y/counter);
			}
			skipped = 0;
		}
		void predictCenterKalman(){
			if(skipped+1 < maxSkipped){
				skipped++;
				if(this->draw){
					this->center = kalman.newPrediction(1);
				}
			}else if(this->draw){
				this->draw = false;
			}
		}
		jobject toJava(JNIEnv*env, int rotation, Size matSize){
			jfieldID idID = env->GetFieldID(jTagCls,"id","I");
			jfieldID lenID = env->GetFieldID(jTagCls,"len","D");
			jfieldID homoID = env->GetFieldID(jTagCls,"homo","Lorg/opencv/core/Mat;");
			jfieldID previewID = env->GetFieldID(jTagCls,"preview","Lorg/opencv/core/Mat;");
			jfieldID pointsID = env->GetFieldID(jTagCls,"points","[Lorg/opencv/android/local/Point;");
			jfieldID centerID = env->GetFieldID(jTagCls,"center","Lorg/opencv/android/local/Point;");

			jobject result = env->NewObject(jTagCls,jTagConsID);

			env->SetIntField(result,idID,this->id);
			env->SetDoubleField(result,lenID,this->l);
			env->SetObjectField(result,homoID,mat2JMat(env,this->homo));
			env->SetObjectField(result,previewID,mat2JMat(env,this->preview));
			jobjectArray pointsJArray = env->NewObjectArray(this->points.size(),jPointCls,points.at(0).toJava(env,rotation,matSize));
			for(int c=1;c<this->points.size();c++){
				env->SetObjectArrayElement(pointsJArray,c,points.at(c).toJava(env,rotation,matSize));
			}
			jobject pointsJ = reinterpret_cast<jobject>(pointsJArray);
			env->SetObjectField(result,pointsID,pointsJ);
			env->DeleteLocalRef(pointsJ);
			env->SetObjectField(result,centerID,center.toJava(env, rotation, matSize));
			return result;
		}
	};


};
#endif
