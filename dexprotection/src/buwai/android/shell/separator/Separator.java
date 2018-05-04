package buwai.android.shell.separator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;
import org.jf.dexlib2.rewriter.ClassDefRewriter;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.MethodRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import buwai.android.dexlib2.helper.MethodHelper;
import buwai.android.shell.advmpformat.StringItem;
import buwai.android.shell.advmpformat.YcFile;
import buwai.android.shell.advmpformat.YcFormat;
import buwai.android.shell.base.Common;
import buwai.android.shell.separator.config.ConfigHelper;
import buwai.android.shell.separator.config.ConfigParse;

/**
 * 方法指令抽取器。
 * Created by buwai on 2015/4/1.
 */
public class Separator {
    private DexFile mDexFile;
    private ConfigHelper mConfigHelper;
    private DexRewriter mDexRewriter;
    private SeparatorOption mOpt;

    private Map<String, List<IndexedMethod>> mSeparatedMethod = new HashMap<String, List<IndexedMethod>>();
    private List<YcFormat.SeparatorData> mSeparatorData = new ArrayList<>();

    /**
     * @param opt
     */
    public Separator(SeparatorOption opt) throws IOException {
        mOpt = opt;
        mDexFile = DexFileFactory.loadDexFile(opt.dexFile, Opcodes.forApi(Common.API)); // 加载dex。

        mDexRewriter = new SeparatorDexRewriter(new SeparatorRewriterModule());

        // 解析配置文件。
        mConfigHelper = new ConfigHelper(new ConfigParse(opt.configFile).parse());

        if (!mOpt.outDexFile.exists()) {
            mOpt.outDexFile.getParentFile().mkdirs();
        }
    }

    /**
     * 抽取方法指令。
     *
     * @return true：成功。false：失败。
     */
    public boolean run() {
        boolean bRet = false;
        // 重新dex。
        DexFile newDexFile = mDexRewriter.rewriteDexFile(mDexFile);
        try {
            // 将新dex输出到文件。
            DexFileFactory.writeDexFile(mOpt.outDexFile.getAbsolutePath(), newDexFile);

            // 写Yc文件。
            writeYcFile();

            // 写C文件。
            writeCFile();

            bRet = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bRet;
    }

    /**
     * 写Yc文件。
     *
     * @throws IOException
     */
    private void writeYcFile() throws IOException {
        YcFormat ycFormat = new YcFormat();
        // TODO 没有对methods字段赋值。
        ycFormat.separatorDatas = mSeparatorData;
        int size = mSeparatorData.size();
        YcFile ycFile = new YcFile(mOpt.outYcFile, ycFormat);
        ycFile.write();
    }

    /**
     * 写C文件。
     */
    private void writeCFile() throws IOException {
        SeparatorCWriter separatorCWriter = new SeparatorCWriter(mOpt.outCPFile, mSeparatedMethod);
        separatorCWriter.write();
    }

    class SeparatorDexRewriter extends DexRewriter {

        public SeparatorDexRewriter(RewriterModule module) {
            super(module);
        }

        @Nonnull
        @Override
        public DexFile rewriteDexFile(@Nonnull DexFile dexFile) {
            return super.rewriteDexFile(dexFile);
        }
    }

    class SeparatorRewriterModule extends RewriterModule {
        @Nonnull
        @Override
        public Rewriter<ClassDef> getClassDefRewriter(@Nonnull Rewriters rewriters) {
            return new ClassDefRewriter(rewriters) {
                @Nonnull
                @Override
                public ClassDef rewrite(@Nonnull ClassDef classDef) {
                    return super.rewrite(classDef);
                }
            };
        }

        private void addMethod(Map<String, List<IndexedMethod>> methodList, Method method,int index){
            List<IndexedMethod> temp = methodList.get(method.getDefiningClass());
            if(null == temp){
                temp = new ArrayList<IndexedMethod>();
                methodList.put(method.getDefiningClass(), temp);
            }
            temp.add(new IndexedMethod(index, method));
        }
        
        @Nonnull
        @Override
        public Rewriter<Method> getMethodRewriter(Rewriters rewriters) {
            return new MethodRewriter(rewriters) {
                @Nonnull
                @Override
                public Method rewrite(@Nonnull Method value) {
                    if (mConfigHelper.isValid(value)) {
                        int index = mSeparatorData.size();
                        addMethod(mSeparatedMethod, value, index);
                        // 抽取代码。
                        YcFormat.SeparatorData separatorData = new YcFormat.SeparatorData();
                        separatorData.methodIndex = mSeparatorData.size();
                        separatorData.methodIndexForDexFile = ((DexBackedMethod) value).methodIndex;
                        separatorData.accessFlag = value.getAccessFlags();
                        separatorData.paramSize = value.getParameters().size();
                        separatorData.registerSize = value.getImplementation().getRegisterCount();

                        separatorData.paramShortDesc = new StringItem();
                        separatorData.paramShortDesc.str = MethodHelper.genParamsShortDesc(value).getBytes();
                        separatorData.paramShortDesc.size = separatorData.paramShortDesc.str.length;

                        separatorData.insts = MethodHelper.getInstructions((DexBackedMethod) value);
                        separatorData.instSize = separatorData.insts.length;
                        separatorData.size = 4 + 4 + 4 + 4 + 4 + 4 + separatorData.paramShortDesc.size + 4 + (separatorData.instSize * 2) + 4;
                        mSeparatorData.add(separatorData);

                        // 下面这么做的目的是要把方法的name删除，否则生成的dex安装的时候会有这个错误：INSTALL_FAILED_DEXOPT。
                        List<? extends MethodParameter> oldParams = value.getParameters();
                        List<ImmutableMethodParameter> newParams = new ArrayList<>();
                        for (MethodParameter mp : oldParams) {
                            newParams.add(new ImmutableMethodParameter(mp.getType(), mp.getAnnotations(), null));
                        }

                        // 生成一个新的方法。
                        return new ImmutableMethod(value.getDefiningClass(), value.getName(), newParams, value.getReturnType(), value.getAccessFlags() | AccessFlags.NATIVE.getValue(), value.getAnnotations(), null);
                    }

                    return super.rewrite(value);
                }
            };
        }
    }

}
