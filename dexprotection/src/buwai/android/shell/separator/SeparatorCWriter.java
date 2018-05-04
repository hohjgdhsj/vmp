package buwai.android.shell.separator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jf.dexlib2.iface.Method;

import buwai.android.dexlib2.helper.MethodHelper;
import buwai.android.shell.controlcentre.CommonUtil;

/**
 * Created by buwai on 2015/4/2.
 */
public class SeparatorCWriter {

    private File mOutFile;

    private Map<String, List<IndexedMethod>> mSeparatedMethod;

    private List<String> registerNativesNames = new ArrayList<>();

    //private int registerNativesIndex = 0;

    public SeparatorCWriter (File outFile, Map<String, List<IndexedMethod>> separatedMethod) {
        mOutFile = outFile;
        mSeparatedMethod = separatedMethod;
    }
    
    private void write_methods(BufferedWriter fileWriter, Map<String, List<IndexedMethod>> methods)throws IOException{
        Set<Entry<String, List<IndexedMethod>>> set = methods.entrySet();
        Iterator<Entry<String, List<IndexedMethod>>> it = set.iterator();
        while(it.hasNext()){
            Entry<String, List<IndexedMethod>> e = it.next();
            List<IndexedMethod> methodList = e.getValue();
            for(int i = 0; i < methodList.size(); i++){
                IndexedMethod method = methodList.get(i);
                writeMethod(method.index, method.method, fileWriter);
            }
        }
    }

    public void write() throws IOException {
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(mOutFile));

        write_methods(fileWriter, mSeparatedMethod);

        write_registerNatives(fileWriter);
        StringBuffer sb = new StringBuffer();
        sb.append("\r\n");
        sb.append("static void registerFunctions(JNIEnv* env)\r\n{");
        for (String registerNativesName : registerNativesNames) {
            sb.append("\r\n");
            sb.append(String.format("    if (!%s(env))\r\n    {\r\n", registerNativesName));
            //sb.append(String.format("        MY_LOG_ERROR(\"register %s fail.\");\r\n", registerNativesName));
            sb.append("    }\r\n");
        }
        
        sb.append("}\r\n");
        fileWriter.write(sb.toString());
        fileWriter.close();
    }

    private void writeMethod(int index, Method method, BufferedWriter fileWriter) throws IOException {
        fileWriter.newLine();
        StringBuffer sb = new StringBuffer();
        sb.append(MethodHelper.genTypeInNative(method));
        sb.append(" ");
        sb.append(MethodHelper.getNativeMethodName(method));
        sb.append("(");
        sb.append(MethodHelper.genParamTypeListInNative(method));
        sb.append(")\r\n{");
        fileWriter.write(sb.toString());
        fileWriter.newLine();

        sb.delete(0, sb.length());
        sb.append("    jvalue result = BWdvmInterpretPortable(");
        sb.append(index);
        sb.append(", env, thiz");

        List<? extends CharSequence> params = method.getParameterTypes();
        for (int i = 0; i < params.size(); i++) {
            sb.append(", ");
            sb.append(MethodHelper.generateParamNameByIndex(i));
        }
        sb.append(");");
        fileWriter.write(sb.toString());
        fileWriter.newLine();

        sb.delete(0, sb.length());
        sb.append("    return ");
        
        if(method.getReturnType().equals("Ljava/lang/String;")){
            sb.append("(jstring)result.l");
        }else{
            char cType = method.getReturnType().charAt(0);
            switch (cType) {
            case 'Z':
                sb.append("result.z");
                break;
            case 'B':
                sb.append("result.b");
                break;
            case 'S':
                sb.append("result.s");
                break;
            case 'C':
                sb.append("result.c");
                break;
            case 'I':
                sb.append("result.i");
                break;
            case 'J':
                sb.append("result.j");
                break;
            case 'F':
                sb.append("result.f");
                break;
            case 'D':
                sb.append("result.d");
                break;
            case 'L':
                sb.append("result.l");
                break;
            case '[':
                sb.append(String.format("(%s)result.l", MethodHelper.getTypeInNativeByTypeName(method.getReturnType())));
                break;
            }
        }
        
        sb.append(";\r\n}");
        fileWriter.write(sb.toString());
        fileWriter.newLine();
    }

    private void write_registerNatives(BufferedWriter fileWriter) throws IOException {
        
        Set<Entry<String, List<IndexedMethod>>> set = mSeparatedMethod.entrySet();
        Iterator<Entry<String, List<IndexedMethod>>> it = set.iterator();
        
        while(it.hasNext()){
            StringBuffer sb = new StringBuffer();
            //append empty line
            sb.append("\r\n");
            Entry<String, List<IndexedMethod>> e = it.next();
            String className = e.getKey();
            String functionName = "registerNatives_" + CommonUtil.convertClassName(className);
            registerNativesNames.add(functionName);
            
            //append function name
            sb.append(String.format("static bool %s(JNIEnv* env)\r\n{\r\n", functionName));
            String log = String.format("    MY_LOG_INFO(\"%s ==>\");\r\n", functionName);
            sb.append(log);
            
            
            //define which class to register
            sb.append(String.format("    const char* classDesc = \"%s\";\r\n", CommonUtil.getClassDesc(className)));
            
            //define functions to register
            sb.append("    const JNINativeMethod methods[] = \r\n    {\r\n");
            List<IndexedMethod> list = e.getValue();
            for(int i = 0; i < list.size(); i++){
                sb.append("        ");
                sb.append(MethodHelper.genJNINativeMethod(list.get(i).method));
                sb.append("\r\n");
            }
            sb.append("    };\r\n");
            
            sb.append("    jclass clazz = env->FindClass(classDesc);\r\n");
            sb.append("    if(env->ExceptionCheck())\r\n    {\r\n");
            sb.append("        env->ExceptionClear();\r\n    }\r\n");
            sb.append("    if (!clazz)\r\n    {\r\n");
            
            
            sb.append("        MY_LOG_ERROR(\"can't find class:%s!\", classDesc);\r\n");
            sb.append("        return false;\r\n    }\r\n");
            
            
            sb.append("    bool bRet = false;\r\n");

            sb.append("    if(JNI_OK == env->RegisterNatives(clazz, methods, array_size(methods)))\r\n    {\r\n");
            sb.append("        bRet = true;\r\n    }\r\n");
            
            sb.append("    else\r\n    {\r\n ");
            sb.append("        MY_LOG_ERROR(\"classDesc:%s, register method fail.\", classDesc);\r\n    }\r\n");

            sb.append("    if(env->ExceptionCheck())\r\n    {\r\n");
            sb.append("        env->ExceptionClear();\r\n    }\r\n");
            sb.append("    env->DeleteLocalRef(clazz);\r\n");
            log = String.format("    MY_LOG_INFO(\"%s <==\");\r\n", functionName);
            sb.append(log);
            sb.append("    return bRet; \r\n}\r\n");
            //write to file
            fileWriter.write(sb.toString());
        }
    }
}
