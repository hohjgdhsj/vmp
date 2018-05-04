package buwai.android.dexlib2.helper;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.dexbacked.DexBackedMethodImplementation;
import org.jf.dexlib2.dexbacked.raw.CodeItem;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;

import buwai.android.shell.controlcentre.CommonUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 增强对Method类的操作。
 * Created by buwai on 2015/4/1.
 */
public class MethodHelper {

    public static final String[] paramNames = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
            "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" };

    public static HashMap<String, String> nativeTypeMap = new HashMap<String, String>();
    
    static{
        nativeTypeMap.put("V", "void");
        nativeTypeMap.put("Z", "jboolean");
        nativeTypeMap.put("B", "jbyte");
        nativeTypeMap.put("S", "jshort");
        nativeTypeMap.put("C", "jchar");
        nativeTypeMap.put("I", "jint");
        nativeTypeMap.put("J", "jlong");
        nativeTypeMap.put("F", "jfloat");
        nativeTypeMap.put("D", "jdouble");
        nativeTypeMap.put("L", "jobject");
        nativeTypeMap.put("[", "jobject");
    }
    /**
     * 在起始位置插入指令。
     * @param method 方法对象。
     * @param insts 要插入的指令。
     * @param regIncrement 寄存器增量。因为插入的指令可能需要增加寄存器。
     * @return 返回新的方法对象。
     */
    public static Method insertInstructionInStart(Method method, List<Instruction> insts, int regIncrement) {
        MethodImplementation oldmi = method.getImplementation();
        List<Instruction> newInsts = new ArrayList<>();

        // 在起始位置插入指令。
        for (Instruction inst : insts) {
            newInsts.add(inst);
        }

        // 再将原有指令插入。
        for (Instruction inst : oldmi.getInstructions()) {
            newInsts.add(inst);
        }

        int regCount = oldmi.getRegisterCount() + regIncrement;
        ImmutableMethodImplementation newmi = new ImmutableMethodImplementation(regCount, newInsts, oldmi.getTryBlocks(), oldmi.getDebugItems());
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(), method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), newmi);
    }

    /**
     * 获得方法中的指令。
     * @param dexBackedMethod
     * @return 返回方法中的指令。
     */
    public static short[] getInstructions(DexBackedMethod dexBackedMethod) {
        int codeOffset = getCodeOffset(dexBackedMethod);
        DexBackedDexFile dexFile = dexBackedMethod.getImplementation().dexFile;

        // 这个size指是u2数组的元素个数。
        int instructionsSize = dexFile.readSmallUint(codeOffset + CodeItem.INSTRUCTION_COUNT_OFFSET);

        int instructionsStartOffset = codeOffset + CodeItem.INSTRUCTION_START_OFFSET;
        short[] insts = new short[instructionsSize];
        for (int i = 0; i < instructionsSize; i++) {
            insts[i] = (short) dexFile.readUshort(instructionsStartOffset);
            instructionsStartOffset += 2;
        }

        return insts;
    }

    /**
     * 获得方法的code_item结构的偏移。
     * @param dexBackedMethod
     * @return 返回方法code_item结构的偏移。
     */
    public static int getCodeOffset(DexBackedMethod dexBackedMethod) {
        int offset = -1;
        DexBackedMethodImplementation dbmi = dexBackedMethod.getImplementation();
        try {
            Field field = dbmi.getClass().getDeclaredField("codeOffset");
            field.setAccessible(true);
            offset = (Integer) field.get(dbmi);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return offset;
    }

    /**
     * 生成方法签名。
     *
     * @param mr
     * @return
     */
    public static String genMethodSig(MethodReference mr) {
        List<? extends CharSequence> params = mr.getParameterTypes();
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        int length = params.size();
        for (int i = 0; i < length; i++) {
            sb.append(params.get(i));
        }
        sb.append(")");
        sb.append(mr.getReturnType());
        return sb.toString();
    }

    /**
     * 生成方法参数的短类型。
     * @param mr
     * @return 返回方法参数的短类型。
     */
    public static String genParamsShortDesc(MethodReference mr) {
        List<? extends CharSequence> params = mr.getParameterTypes();
        StringBuilder sb = new StringBuilder();
        int length = params.size();
        for (int i = 0; i < length; i++) {
            char chType = params.get(i).charAt(0);
            if ('[' == chType) {
                sb.append('L');
            } else {
                sb.append(chType);
            }
        }
        return sb.toString();
    }

    /**
     * 生成在native中的类型。
     * @param mr
     * @return
     */
    public static String genTypeInNative (MethodReference mr) {
        String type = mr.getReturnType();
        return getTypeInNativeByTypeName(type);
    }

    /**
     * 生成在native中的类型。
     * @param type
     * @return
     */
    public static String getTypeInNativeByTypeName(String type) {
        String ret = null;
        char cType = type.charAt(0);
        if(type.equals("Ljava/lang/String;")){
            ret = "jstring";
        }else if('[' != cType){
            ret = nativeTypeMap.get(""+cType);
        }else{
            ret = nativeTypeMap.get("" + type.charAt(1)) + "Array";
        }
        return ret;
    }

    /**
     * 生成JNI方法参数列表。
     *
     * @param mr
     * @return 返回方法参数列表。
     */
    public static String genParamTypeListInNative(Method mr) {
        StringBuilder paramsList = new StringBuilder();
        paramsList.append("JNIEnv *env, jobject thiz");
        List<? extends MethodParameter> parameters = mr.getParameters();
        for(int i = 0; i < parameters.size(); i++){
            paramsList.append(", ");
            paramsList.append(getTypeInNativeByTypeName(parameters.get(i).getType()));
            paramsList.append(" ");
            //paramsList.append(parameter.getName());
            paramsList.append(generateParamNameByIndex(i));
            
        }

        return paramsList.toString();
    }
    
    public static String generateParamNameByIndex(int index){
        String ret = null;
        if(index<0){
            return ret; 
        }
        int mod = index%26;
        int times = index/26 + 1;
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < times; i++ ){
            sb.append((char)('a' + mod));
        }
        sb.append(index);
        return sb.toString();
    }

    public static String getNativeMethodName(Method method){
        String ret = null;
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("Java_%s_%s__", CommonUtil.convertClassName(method.getDefiningClass()), method.getName()));
        List<? extends CharSequence> params = method.getParameterTypes();
        for(int i=0; i<params.size();i++){
            String param = (String)params.get(i);
            if(param.startsWith("[")){
                param = param.replaceAll("\\[", "3");
            }
            
            if(param.endsWith(";")){
                param = param.replaceAll(";", "_2");
            }
            param = param.replaceAll("/", "_");
            sb.append(param);
            if(i != params.size()-1){
                sb.append("_");
            }
        }
        ret = sb.toString();
        return ret;
    }
    /**
     * 生成JNINativeMethod结构的数据。
     * @param method
     * @return
     */
    public static String genJNINativeMethod(Method method) {
        return String.format("{ \"%s\", \"%s\", (void*)%s }, ", method.getName(), genMethodSig(method), getNativeMethodName(method));
    }

}
