LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := filesearch
LOCAL_MODULE_NAME := libfilesearch
LOCAL_SRC_FILES := com_xunlei_cloud_extstorage_FileSearchManager.cpp

LOCAL_LDLIBS:=-L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)