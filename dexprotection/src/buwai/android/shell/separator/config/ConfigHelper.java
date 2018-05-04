package buwai.android.shell.separator.config;

import java.util.Iterator;
import java.util.List;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.instruction.Instruction;

import buwai.android.shell.base.TypeDescription;
import buwai.android.shell.base.helper.TypeDescriptionHelper;
import buwai.android.shell.controlcentre.MyLog;

/**
 * Created by buwai on 2015/4/1.
 */
public class ConfigHelper {

    private Config mConfig;

    public ConfigHelper (Config config) {
        mConfig = config;
    }
    
    public void printMethod(Method method){
        if(null == method)
        {
            return;
        }
        DexBackedMethod myMethod = (DexBackedMethod) method;
        MyLog myLog = new MyLog("D:\\vmp\\printMethodlog.txt");
        myLog.init(true);
        String tmp = method.getName();
        myLog.writeLine("");
        myLog.writeLine("printMethod ==>"+myMethod.methodIndex);
        
        myLog.writeLine("getName:"+tmp);
        
        int accessFlags = method.getAccessFlags();
        myLog.writeLine("getAccessFlags:" + String.format("0x%x", accessFlags));
        
        String defcls = method.getDefiningClass();
        myLog.writeLine("getDefiningClass:" + defcls);
        
        MethodImplementation mip = method.getImplementation();
        myLog.writeLine("getImplementation:" + mip);
        if(null != mip){
            Iterator<? extends Instruction>  it = mip.getInstructions().iterator();
            while(it.hasNext()){
                Instruction instruction = it.next();
                Opcode code = instruction.getOpcode();
                myLog.writeLine(""+ code + ":"+code.ordinal());
            }
        }
        
        
        List<? extends MethodParameter> params = method.getParameters();
        myLog.writeLine("getParameters:" + params);
        for(int i=0; i<params.size();i++){
            MethodParameter mp = params.get(i);
            myLog.writeLine("\t\t"+mp.getType()+":"+mp.getName());
        }
        
        List<? extends CharSequence> paramTypes =  method.getParameterTypes();
        myLog.writeLine("getParameterTypes:" + paramTypes);
        
        String returnType = method.getReturnType();
        myLog.writeLine("getReturnType:" + returnType);
        
        myLog.writeLine("printMethod <==");
        myLog.close();
        
    }

    /**
     * 是否有效。
     * @param method
     * @return true：有效。false：无效。
     */
    public boolean isValid(Method method) {
        
        int zzz = method.getAccessFlags();
        if((zzz&0x1000) != 0x1000){
            printMethod(method);
        }
        TypeDescription methodTypeDesc = TypeDescriptionHelper.convertByMethod(method);

        // 不支持方法中有try...catch的。
        if (false == mConfig.isSupportTryCatch) {
            
            if (null != method.getImplementation() 
                    && 0 != method.getImplementation().getTryBlocks().size()) {
                return false;
            }
        }

        // 判断方法是否实现。
        if (null == method.getImplementation()) {
            return false;
        }

        // 判断是否是native方法。
        if (0 != (method.getAccessFlags() & AccessFlags.NATIVE.getValue())) {
            return false;
        }
        
        //accessFlags为0x1000时，表示该函数不由用户代码生成
        int flag = method.getAccessFlags();
        if((flag&0x1000) == 0x1000){
            return false;
        }

        // 不支持<clinit>
        if ("<clinit>".equals(methodTypeDesc.methodName)) {
            return false;
        }

        // 不支持<init>
        if ("<init>".equals(methodTypeDesc.methodName)) {
            return false;
        }

        // TODO 这里现在是写死的！
//        if (!method.getName().startsWith("my_separatorTest_")) {
//            return false;
//        }
        
        

        // 先判断黑名单。
        if (TypeDescriptionHelper.isMatchMethodInBlackList(mConfig.blackList, methodTypeDesc)) {
            return false;
        }

        // 再判断白名单。
        if (!TypeDescriptionHelper.isMatchMethodInWhiteList(mConfig.whiteList, methodTypeDesc)) {
            return false;
        }

        return true;
    }

}
