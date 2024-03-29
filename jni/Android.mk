TOP_PATH := $(call my-dir)/..



include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}/sdk/native/jni/OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := recognizer
LOCAL_SRC_FILES := recognizer.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/nonfree
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}/sdk/native/jni/OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := calibrate
LOCAL_SRC_FILES := calibrate.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/nonfree
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}/sdk/native/jni/OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := misc
LOCAL_SRC_FILES := misc.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/nonfree
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include ${OPENCV_HOME}/sdk/native/jni/OpenCV.mk
LOCAL_PATH=$(TOP_PATH)/jni
LOCAL_MODULE    := detector
LOCAL_SRC_FILES := detector.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/nonfree
LOCAL_LDLIBS +=  -llog -ldl
include $(BUILD_SHARED_LIBRARY)
