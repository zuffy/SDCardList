#include "com_xunlei_cloud_extstorage_FileSearchManager.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <dirent.h>
#include <time.h>
#include <android/log.h>

using namespace std;  
#define DEBUG 1
#define LOG_TAG "---"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define CHECK_BREAK(ARG) if(ARG){break;}
#define CHECK_ISVIDEO(ARG) (strcmp(ARG,".wmv")==0 || strcmp(ARG,".mp4")==0 || strcmp(ARG,".avi")==0)

#define PATHMAX 1024

#define WRITE_TO_JAVA 1

char savepath[PATHMAX] = {'\0'};

const char * __my_class__ = "com/xunlei/cloud/extstorage/FileSearchManager";
const char * __call_method__ = "addToList";

void searchdir(const char *path);

string s_content;

JNIEnv *env_;
jclass SDCardList;
jobject self;
jmethodID addToListMth;

void addToList(const char * p) {
    while(1){
        CHECK_BREAK( env_ == NULL);
        
        jstring str = env_->NewStringUTF(p);
        
        CHECK_BREAK(str == NULL && SDCardList == NULL && addToListMth == NULL);

        env_->CallVoidMethod(self, addToListMth, str);
        env_->DeleteLocalRef(str);

        break;
    }
}

int totalNum = 0;
float totaltime = .0f;
clock_t t_clock;
void searchdir(const char *path)
{
    DIR *dp;
    struct dirent *dmsg;
    int i=0;
    char addpath[PATHMAX] = {'\0'}, *tmpstr;
    if ((dp = opendir(path)) != NULL)
    {
      
      while ((dmsg = readdir(dp)) != NULL)
      {
        totalNum ++;
  
        if (!strcmp(dmsg->d_name, ".") || !strcmp(dmsg->d_name, ".."))
            continue;
        sprintf(addpath, "%s/%s", path, dmsg->d_name);
        // strcpy(addpath, path);
        // strcat(addpath, "/");
        // strcat(addpath, dmsg->d_name);
        if (dmsg->d_type == DT_DIR ) 
        {
            char *temp = strchr(dmsg->d_name, '.');
            if(temp)
            {
               if((strcmp(temp, dmsg->d_name)==0))
               {
                 // LOGI("%s",addpath);
                 continue;
               }
            }
            // LOGI("%s",addpath);
            searchdir(addpath);
        }
        /*
        else if (dmsg->d_type==8)//普通文件
        {
            char *p=dmsg->d_name + strlen(dmsg->d_name)-4;
            //if (strcmp(p,".wmv")==0)
            {
                //LOGI("file name:%s",dmsg->d_name);
            }
            //LOGI("%s",addpath);
        }*/
    #ifdef DEBUG
        t_clock = clock();
    #endif
        strcat(addpath, ";");
        s_content.append(addpath);
        //addToList(addpath);
    #ifdef DEBUG
        totaltime += (float)(clock()-t_clock)/1000/1000;
    #endif
      }
    }
    closedir(dp);
}

void getVideo(const char *outstr) {
    int i= 0, j=0, length = strlen(outstr);
    char tmp[PATHMAX] = {'\0'};
    while(i < length){
        tmp[j] = outstr[i];
        // LOGI("str1 %c",tmp[i]);
        if(tmp[j] == '\t' || tmp[j] == ';'){
            tmp[j] = '\0';
            // LOGI("str2 %s",tmp);
            // char *p=tmp + strlen(tmp)-4;
            // LOGI("str3 %s",p);
            //if(CHECK_ISVIDEO(p)){
                addToList(tmp);
            //}
            j = -1;
            memset(tmp, '\0', PATHMAX);
        }
        i++;
        j++;
    }
}

JNIEXPORT jfloat JNICALL Java_com_xunlei_cloud_extstorage_FileSearchManager_searchFiles (JNIEnv *env, jobject thiz, jstring inString)
{
    clock_t tick_start,tick_end;
    float dtime = .0f;

    const char *dirpath = env->GetStringUTFChars(inString,NULL);
    sprintf(savepath, "%s%s", dirpath, "test.txt");
    LOGI("begin...................................... savepath: %s", savepath);
    
    s_content.clear();
    totalNum = 0;

#ifdef WRITE_TO_JAVA
    env_ = env;
    self = thiz;
    SDCardList = env->FindClass( __my_class__);
    if (SDCardList == NULL)
    {
        LOGI("no found class %s", __my_class__);
        return dtime;
    }
    addToListMth = env->GetMethodID( SDCardList, __call_method__, "(Ljava/lang/String;)V");
    if (addToListMth == NULL)
    {
        LOGI("no found method %s", __call_method__);
        env->DeleteLocalRef( SDCardList);
        return dtime;
    }
    LOGI("file search start!");
#endif
    
    tick_start=clock();
    searchdir(dirpath);
    // LOGI("con: %s", s_content.c_str());

    getVideo(s_content.c_str());

    tick_end=clock();

    dtime = (float)(tick_end-tick_start)/1000/1000;

#ifdef WRITE_TO_JAVA
    LOGI("file search finished! n:%d t:%f content_len:%d", totalNum, totaltime, s_content.length());
    env_ = NULL;
    self = NULL;
#endif
    
    env->ReleaseStringUTFChars(inString,dirpath);
    return dtime;
}