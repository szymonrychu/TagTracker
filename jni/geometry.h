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
	class Tag{
	public:
		int id;
		Mat preview;
		Mat homo;
		vector<Geometry::Point> points;
		Geometry::Point center;
		double l;
		void setPoints(vector<Point2f> points,int rotation, Size matSize, double l){
			vector<Point2f>::iterator it;
			int X = 0;
			int Y = 0;
			int counter = 0;
			for(it=points.begin();it!=points.end();it++){
				Geometry::Point point;
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
		jobject toJava(JNIEnv*env, int rotation, Size matSize){
			jfieldID idID = env->GetFieldID(jTagCls,"id","I");
			jfieldID lenID = env->GetFieldID(jTagCls,"len","D");
			jfieldID homoID = env->GetFieldID(jTagCls,"homo","Lorg/opencv/core/Mat;");
			jfieldID previewID = env->GetFieldID(jTagCls,"preview","Lorg/opencv/core/Mat;");
			jfieldID pointsID = env->GetFieldID(jTagCls,"points","[Lcom/richert/tagtracker/geomerty/Point;");
			jfieldID centerID = env->GetFieldID(jTagCls,"center","Lcom/richert/tagtracker/geomerty/Point;");

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
