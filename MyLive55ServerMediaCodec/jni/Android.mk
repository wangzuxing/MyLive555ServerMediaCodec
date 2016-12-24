LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := live555
LOCAL_SRC_FILES := liblive555.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/live555/BasicUsageEnvironment \
	$(LOCAL_PATH)/live555/BasicUsageEnvironment/include \
	$(LOCAL_PATH)/live555/groupsock \
	$(LOCAL_PATH)/live555/groupsock/include \
	$(LOCAL_PATH)/live555/liveMedia \
	$(LOCAL_PATH)/live555/liveMedia/include \
	$(LOCAL_PATH)/live555/UsageEnvironment \
	$(LOCAL_PATH)/live555/UsageEnvironment/include \
	$(LOCAL_PATH)/live555/mediaServer \
	
LOCAL_SHARED_LIBRARIES := live555

LOCAL_MODULE := streamer
LOCAL_SRC_FILES := streamer.cpp
LOCAL_LDLIBS    += -llog -lz

include $(BUILD_SHARED_LIBRARY)
