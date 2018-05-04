#include "stdafx.h"
#include "Utils.h"

char* GetAppPath(JNIEnv* env) {
    jclass cActivityThread = env->FindClass("android/app/ActivityThread");
    jmethodID mActivityThread = env->GetStaticMethodID(cActivityThread, "currentActivityThread", "()Landroid/app/ActivityThread;");
    jobject oActivityThread = env->CallStaticObjectMethod(cActivityThread, mActivityThread);

    jfieldID fmBoundApplication = env->GetFieldID(cActivityThread, "mBoundApplication", "Landroid/app/ActivityThread$AppBindData;");
    jobject omBoundApplication = env->GetObjectField(oActivityThread, fmBoundApplication);

    jclass cAppBindData = env->FindClass("android/app/ActivityThread$AppBindData");
    jfieldID fappInfo = env->GetFieldID(cAppBindData, "appInfo", "Landroid/content/pm/ApplicationInfo;");
    jobject oApplicationInfo = env->GetObjectField(omBoundApplication, fappInfo);

    jclass cApplicationInfo = env->FindClass("android/content/pm/ApplicationInfo");
    jfieldID fsourceDir = env->GetFieldID(cApplicationInfo, "sourceDir", "Ljava/lang/String;");
    jstring sourceDir = (jstring) env->GetObjectField(oApplicationInfo, fsourceDir);

    const char* sourceDir_s = env->GetStringUTFChars(sourceDir, NULL);

    char* cRet = strdup(sourceDir_s);

_ret:
    if (NULL != sourceDir_s) {
        env->ReleaseStringUTFChars(sourceDir, sourceDir_s);
    }
    if (NULL != sourceDir) {
        env->DeleteLocalRef(sourceDir);
    }
    if (NULL != cApplicationInfo) {
        env->DeleteLocalRef(cApplicationInfo);
    }
    if (NULL != cAppBindData) {
        env->DeleteLocalRef(cAppBindData);
    }
    if (NULL != omBoundApplication) {
        env->DeleteLocalRef(omBoundApplication);
    }
    if (NULL != oActivityThread) {
        env->DeleteLocalRef(oActivityThread);
    }
    if (NULL != cActivityThread) {
        env->DeleteLocalRef(cActivityThread);
    }

    return cRet;
}

jobject getInteger(JNIEnv * env, int value)
{
    jobject ret = NULL;
    jclass cls_integer = env->FindClass("java/lang/Integer");
    jmethodID mid_valueOf = env->GetStaticMethodID(cls_integer, "valueOf", "(I)Ljava/lang/Integer;");
    ret = env->CallStaticObjectMethod(cls_integer, mid_valueOf, value);

    env->DeleteLocalRef(cls_integer);
    return ret;
}

int getIntegerValue(JNIEnv * env, jobject value)
{
    int ret = 0;
    jclass cls_integer = env->FindClass("java/lang/Integer");
    jmethodID mid_intValue = env->GetMethodID(cls_integer, "intValue", "()I");
    ret = (int)env->CallIntMethod(value, mid_intValue);

    env->DeleteLocalRef(cls_integer);
    return ret;
}

void setHashMapValue(JNIEnv * env, jobject hashMap, int key, int value)
{
    jclass cls_hashMap = env->FindClass("java/util/HashMap");
    jmethodID mid_put = env->GetMethodID(cls_hashMap, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    env->CallObjectMethod(hashMap, mid_put, getInteger(env, key), getInteger(env, value));

    env->DeleteLocalRef(cls_hashMap);
}

int getHashMapValue(JNIEnv * env, jobject hashMap, int key)
{
    int ret = -1;
    jclass cls_hashMap = env->FindClass("java/util/HashMap");
    jmethodID mid_get = env->GetMethodID(cls_hashMap, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    jobject objValue = env->CallObjectMethod(hashMap, mid_get, getInteger(env, key));
    if(NULL != objValue)
    {
        ret = getIntegerValue(env, objValue);
        env->DeleteLocalRef(objValue);
    }
    env->DeleteLocalRef(cls_hashMap);
    return ret;
}
