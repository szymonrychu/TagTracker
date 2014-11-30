TOP_PATH := $(call my-dir)/..

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}C:\Users\szymonri\Desktop\Android\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := recognize
LOCAL_SRC_FILES := recognize.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}C:\Users\szymonri\Desktop\Android\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := facefollower
LOCAL_SRC_FILES := facefollower.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}C:\Users\szymonri\Desktop\Android\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := calibrate
LOCAL_SRC_FILES := calibrate.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}C:\Users\szymonri\Desktop\Android\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := misc
LOCAL_SRC_FILES := misc.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)