#include "stdafx.h"
#include "Common.h"
#include "InterpC.h"
#include "Globals.h"
#include "Utils.h"
#include <dlfcn.h>
#include <pthread.h>

#ifdef _AVMP_DEBUG_

void nativeLog(JNIEnv* env, jobject thiz) {
    MY_LOG_INFO("nativeLog, thiz=%p", thiz);
}

jint separatorTest(JNIEnv* env, jobject thiz, jint value) {
    MY_LOG_INFO("separatorTest - value=%d", value);
    jvalue result = BWdvmInterpretPortable(0, env, thiz, value);
    return result.i;
}

/**
 * register native functions
 */
bool registerNatives(JNIEnv* env) {
    const char* classDesc = "buwai/android/shell/advmptest/MainActivity";
    const JNINativeMethod methods[] = {
        { "separatorTest", "(I)I", (void*) separatorTest },
        { "nativeLog", "()V", (void*) nativeLog }
    };

    jclass clazz = env->FindClass(classDesc);
    if (!clazz) {
        MY_LOG_ERROR("not find class:%s !", classDesc);
        return false;
    }

    bool bRet = false;
    if ( JNI_OK == env->RegisterNatives(clazz, methods, array_size(methods)) ) {
        bRet = true;
    } else {
        MY_LOG_ERROR("register class:%s.register native method fail.", classDesc);
    }
    env->DeleteLocalRef(clazz);
    return bRet;
}

void registerFunctions(JNIEnv* env) {
    if (!registerNatives(env)) {
        MY_LOG_ERROR("registerFunctions fail.");
        return;
    }
}

#else

//+${replaceAll}

#endif

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;

    if (vm->GetEnv((void **)&env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }




    registerFunctions(env);

    void* so = dlopen("/system/lib/libdvm.so", 0);
	void* gDvm_addr = dlsym(so, "gDvm");
	DvmGlobals *gDvm = (DvmGlobals*)gDvm_addr;

	Thread* self = (Thread*)pthread_getspecific(gDvm->pthreadKeySelf);


    gAdvmp.apkPath = GetAppPath(env);
    MY_LOG_INFO("apk path:%s", gAdvmp.apkPath);


    gAdvmp.ycSize = ReleaseYcFile(gAdvmp.apkPath, &gAdvmp.ycData);
    if (0 == gAdvmp.ycSize) {
        MY_LOG_WARNING("release Yc file fail!");
        goto _ret;
    }

    // parse yc file
    gAdvmp.ycFile = new YcFile;
    if (!gAdvmp.ycFile->parse(env, gAdvmp.ycData, gAdvmp.ycSize)) {
        MY_LOG_WARNING("parse Yc file fail.");
        goto _ret;
    }



_ret:
    return JNI_VERSION_1_4;
}
