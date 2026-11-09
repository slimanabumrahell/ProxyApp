TOP_PATH := $(call my-dir)
include $(TOP_PATH)/hev-socks5-tunnel/Android.mk

LOCAL_PATH := $(TOP_PATH)
include $(CLEAR_VARS)
LOCAL_MODULE := hev-jni
LOCAL_SRC_FILES := hev-jni.c
LOCAL_STATIC_LIBRARIES := hev-socks5-tunnel
include $(BUILD_SHARED_LIBRARY)
