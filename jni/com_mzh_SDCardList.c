#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <time.h>
#include <sys/stat.h>
#include <android/log.h>
  
#define LOG_TAG "---"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
  
#define PATHMAX 1024

#define WRITE_TO_JAVA 1
// #define WRITE_TO_MEN 1

char savepath[PATHMAX] = {'\0'};

const char * __my_class__ = "com/mzh/SDCardList";
const char * __call_method__ = "addToList";
void searchdir(char *path);
  
int total = 0;
int fd;

std::string c_content;

const int O_ACCMODE  = 0003;
const int O_RDONLY   = 00;
const int O_WRONLY   = 01;
const int O_RDWR     = 02;
const int O_CREAT    = 0100; /* not fcntl */  
const int O_EXCL     = 0200; /* not fcntl */  
const int O_NOCTTY   = 0400; /* not fcntl */  
const int O_TRUNC    = 01000; /* not fcntl */  
const int O_APPEND   = 02000;
const int O_NONBLOCK = 04000;
const int O_NDELAY   = 04000;
const int O_SYNC     = 010000;
const int O_FSYNC    = 010000;
const int O_ASYNC    = 020000;

int file_open(const char *filename, int flags)
{
    int fd;
  
    fd = open(filename, flags, 0666);
    if (fd == -1)
        return -1;

    return fd;
}

 int file_read(int fd, unsigned char *buf, int size)
{
    
    return read(fd, buf, size);
}

 int file_write(int fd, const unsigned char *buf, int size)
{
    
    return write(fd, buf, size);
}


 int64_t file_seek(int fd, int64_t pos, int whence)
{
    
    if (whence == 0x10000) {
        struct stat st;
        int ret = fstat(fd, &st);
        return ret < 0 ? -1 : st.st_size;
    }
    return lseek(fd, pos, whence);
}

 int file_close(int fd)
{
   
    return close(fd);
}

//==================2========================//
FILE* FileHandle;
unsigned short File_Open(FILE** FileHandle, char* name,
        unsigned short flag, unsigned short mode) {

    if (FileHandle == NULL) {
        return 100;
    }
    if (name == NULL) {
        return 100;
    }
    char *type = "w+";
    /*
    if (flag == O_OREAD) {
        if (mode != O_CREATE) {
            strcpy(type, "r"); //只读，不建文件
        } else {
            strcpy(type, "r+");
        }
    } else if (flag == O_OWRITE) {
        if (mode != O_CREATE) {
            strcpy(type, "w"); //只写，不建文件
        } else {
            strcpy(type, "w+");
        }
    } else if (flag == O_RW) {
        if (mode != O_CREATE) {
            strcpy(type, "a");
        } else {
            strcpy(type, "a+");
        }
    } else if (flag == O_APPEND) {
        if (mode != O_CREATE) {
            strcpy(type, "a");
        } else {
            strcpy(type, "a+");
        }
    }*/

    *FileHandle = fopen(name, type);
    return 0;
}

/**
 * File_Close--关闭文件
 * 返回值
 * 0--关闭成功;否则失败
 */
unsigned short File_Close(FILE* FileHandle) {
    if (FileHandle == NULL) {
        return 100;
    }
    return fclose(FileHandle);
}

/**
 * File_Read--读取文件到buf
 * count--读取的长度
 * ReadCount--返回已读取的长度
 */
unsigned short File_Read(FILE* FileHandle, char* buf,
        unsigned long count, unsigned long* ReadCount) {
    if (FileHandle == NULL) {
        return 100;
    }
    *ReadCount = fread(buf, 1, count, FileHandle);
    __android_log_print(ANDROID_LOG_INFO, "JNIMsg",
            "File_Read           ReadCount=%d", *ReadCount);
    return 0;
}

/**
 * File_Write--从buf中写入文件
 * count--写入的长度
 * WriteCount--返回已写入的长度
 */
unsigned short File_Write(FILE* FileHandle, char* buf,
        unsigned long count, unsigned long* WriteCount) {
    if (FileHandle == NULL) {
        return 100;
    }
    unsigned short write_result = fwrite(buf, count, 1, FileHandle); // 返回值是成功写入的项目数
    if(write_result == 1) {
        *WriteCount = write_result * count;
    }
    return write_result;
}

unsigned short File_Write_inCache(FILE* FileHandle, char* buf,
        unsigned long count, unsigned long* WriteCount) {
    if (FileHandle == NULL) {
        return 100;
    }
    unsigned short write_result = fput(buf, count, 1, FileHandle); // 返回值是成功写入的项目数
    if(write_result == 1) {
        *WriteCount = write_result * count;
    }
    return write_result;
}


//==================2========================//

int get_line_length(char *p){
    int i = 0;
    // LOGI("get_line_length");
    while(p[i] && p[i] != "\n"){
        i++;
    }
    // LOGI("got len: %d", i);
    return i;
};

void addtoFile(const char *addpath){
    // LOGI("sizeof string %d.", len);
    int len = get_line_length(addpath);
    char *tmp = (char *)malloc(sizeof(addpath)*len);
    // LOGI("tmp %s", tmp);
    strcpy(tmp, addpath);
    // LOGI("tmp2 %s, size: %d, %d", tmp, sizeof(addpath), sizeof(tmp));
    //file_write(fd, addpath, sizeof(addpath));
    file_write(fd, tmp, len);
    free(tmp);
}

char *out;
int length;
void addToMen(const char *path){
    if(length > 0){
        char *tmp = (char *)malloc(sizeof(out)*length);
        strcpy(tmp, out);
        free(out);
        int len = strlen(path);
        out = (char *)malloc(sizeof(path)*(len+length));
        strcpy(out, tmp);
        strcat(out, path);
        length = length + len;
        free(tmp);
    }
    else if(length == 0){
        length = strlen(path);
        out = (char *)malloc(sizeof(path)*length);
        strcpy(out, path);
        LOGI("h3--%d:%s",length,out);
    }
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
            char *p=tmp + strlen(tmp)-4;
            // LOGI("str3 %s",p);
            if(strcmp(p,".wmv")==0 || strcmp(p,".mp4")==0 || strcmp(p,".avi")==0){
                addToList(tmp);
            }
            j = -1;
            memset(tmp, '\0', PATHMAX);
        }
        i++;
        j++;
    }
}

#define CHEKC_BREAK(ARG) if(ARG){break;}

JNIEnv *env_;
jclass SDCardList;
jobject self;
jmethodID addToListMth;
void addToList(const char * p) {
    while(1){
        CHEKC_BREAK(env_ == NULL);
        
        jstring str = (*env_)->NewStringUTF(env_, p);
        
        CHEKC_BREAK(str == NULL && SDCardList == NULL && addToListMth == NULL);

        (*env_)->CallVoidMethod(env_, self, addToListMth, str);
        (*env_)->DeleteLocalRef(env_, str);

        break;
    }
}


void searchdir(char *path)
{
    DIR *dp;
    struct dirent *dmsg;
    int i=0;
    unsigned char addpath[PATHMAX] = {'\0'}, *tmpstr;
    if ((dp = opendir(path)) != NULL)
    {
      
      while ((dmsg = readdir(dp)) != NULL)
      {
  
        if (!strcmp(dmsg->d_name, ".") || !strcmp(dmsg->d_name, ".."))
            continue;
        // strcpy(addpath, path);
        // strcat(addpath, "/");
        // strcat(addpath, dmsg->d_name);
        sprintf(addpath, "%s/%s", path, dmsg->d_name);
        if (dmsg->d_type == DT_DIR ) 
        {
            char *temp;
            temp=dmsg->d_name;
            if(strchr(dmsg->d_name, '.'))
            {
               if((strcmp(strchr(dmsg->d_name, '.'), dmsg->d_name)==0))
               {
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

#ifndef WRITE_TO_JAVA
        strcat(addpath, "\n");
        addtoFile(addpath);
#else
        strcat(addpath, ";");
        c_content.append(addpath);
        // addToList(addpath);
        // strcat(addpath, "\t");
        // addToMen(addpath);
        /*if (dmsg->d_type==8)//普通文件
        {
            char *p=dmsg->d_name + strlen(dmsg->d_name)-4;
            if(strcmp(p,".wmv")==0 || strcmp(p,".mp4")==0 || strcmp(p,".avi")==0){
                addToList(p);
            }
            //LOGI("%s",addpath);
        }*/
#endif
        
      }
    }
    closedir(dp);
}


JNIEXPORT jfloat JNICALL Java_com_mzh_SDCardList_getSecond (JNIEnv *env, jobject thiz, jstring inString)
{
    clock_t   tick_start,tick_end;
    double t;
    float dtime = .0f;
    char *dirpath;//="/mnt/sdcard/";
    dirpath = (*env)->GetStringUTFChars(env,inString,NULL);
    strcpy(savepath,dirpath);
    strcat(savepath, "test.txt");
    LOGI("begin...................................... savepath: %s", savepath);

#ifndef WRITE_TO_JAVA
    fd = file_open(savepath, O_CREAT | O_WRONLY);
    LOGI("fd. %d", fd);
    if(fd != -1)
    {
#else
    env_ = env;
    self = thiz;
    SDCardList = (*env)->FindClass(env, __my_class__);
    if (SDCardList == NULL)
    {
        LOGI("no found class %s", __my_class__);
        return dtime;
    }
    addToListMth = (*env)->GetMethodID(env, SDCardList, __call_method__, "(Ljava/lang/String;)V");
    if (addToListMth == NULL)
    {
        LOGI("no found method %s", __call_method__);
        (*env)->DeleteLocalRef(env, SDCardList);
        return dtime;
    }
    LOGI("h1");
    c_content.clear();
#endif
        tick_start=clock();
        searchdir(dirpath);
        tick_end=clock();
        dtime = (float)(tick_end-tick_start)/1000/1000;

#ifndef WRITE_TO_JAVA
        file_close(fd);
    }
    LOGI("file write finished!");
#else
    getVideo(c_content.c_str());
    LOGI("h4--%d",length);
    env_ = NULL;
    self = NULL;
#endif
    (*env)->ReleaseStringUTFChars(env,inString,dirpath);
    return dtime;
}