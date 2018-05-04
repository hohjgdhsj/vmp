package buwai.android.shell.controlcentre;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Logger;

import buwai.android.shell.base.Utils;
import buwai.android.shell.base.helper.CommandHelper;
import buwai.android.shell.separator.Separator;
import buwai.android.shell.separator.SeparatorOption;

/**
 * Created by Neptunian on 2015/4/1.
 */
public class ControlCentre {

    private final static Logger log = Logger.getLogger(ControlCentre.class);

    private ControlCentreOption mOpt;

    public ControlCentre(ControlCentreOption opt) throws IOException {
        mOpt = opt;
        prepare();
    }

    /**
     * 做一些准备工作。
     */
    private void prepare() throws IOException {
        // 创建工作目录。
        mOpt.workspace = Files.createTempDirectory(mOpt.outDir.toPath(), "advmp").toFile();
        log.info("workspack:" + mOpt.workspace);
    }

    /**
     * 加壳。
     *
     * @return
     */
    public boolean shell() {
        boolean bRet = false;
        try {
            // 运行抽离器。
            runSeparator();

            // 从template目录中拷贝jni文件。
            copyJniFiles();

            // 更新jni文件的内容。
            updateJniFiles();

            // 编译native代码。
            buildNative();

            System.out.println(" 编译成功 !");
            bRet = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bRet;
    }

    /**
     * 运行抽取器。
     *
     * @return
     * @throws IOException
     */
    private boolean runSeparator() throws IOException {
        SeparatorOption opt = new SeparatorOption();
        File outDir = new File(mOpt.workspace, "separator");
        opt.outDexFile = new File(outDir, "classes.dex");
        opt.outYcFile = mOpt.outYcFile = new File(outDir, "classes.yc");
        opt.outCPFile = mOpt.outYcCPFile = new File(outDir, "advmp_separator.cpp");
        opt.dexFile = mOpt.dexFile;

        Separator separator = new Separator(opt);
        return separator.run();
    }

    /**
     * 将template中的jni目录拷贝到工作目录。
     *
     * @throws IOException
     */
    private void copyJniFiles() throws IOException {
        File jniTemplateDir = new File(System.getProperty("user.dir") + File.separator + "template" + File.separator + "jni");
        mOpt.jniDir = new File(mOpt.workspace, "jni");
        Utils.copyFolder(jniTemplateDir.getAbsolutePath(), mOpt.jniDir.getAbsolutePath());
    }

    /**
     * 更新jni目录中的文件。
     */
    private void updateJniFiles() throws IOException {
        File file;
        File tmpFile;
        StringBuffer sb = new StringBuffer();

        // 更新avmp.cpp文件中的内容。
        try (BufferedReader reader = new BufferedReader(new FileReader(mOpt.outYcCPFile))) {
            String line = null;
            while (null != (line = reader.readLine())) {
                sb.append(new String(line.getBytes(), "UTF-8"));
                sb.append(System.getProperty("line.separator"));
            }
        }

        file = new File(mOpt.jniDir.getAbsolutePath() + File.separator + "avmp.cpp");
        tmpFile = new File(mOpt.jniDir.getAbsolutePath() + File.separator + "avmp.cpp" + ".tmp");
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile))) {
            String line = null;
            while (null != (line = reader.readLine())) {
                if ("#ifdef _AVMP_DEBUG_".equals(line)) {
                    writer.write("#if 0");
                    writer.newLine();
                } else if ("//+${replaceAll}".equals(line)) {
                    writer.write(sb.toString());
                } else {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
        file.delete();
        tmpFile.renameTo(file);
        sb.delete(0, sb.length());
    }

    /**
     * 编译native代码。
     * @throws FileNotFoundException
     */
    private void buildNative() throws FileNotFoundException {
        File ndkDir = new File(System.getenv("NDK_ROOT"));
        if (ndkDir.exists()) {
            String ndkPath = ndkDir.getAbsolutePath() + File.separator + "ndk-build";
            if (Utils.isWindowsOS()) {
                ndkPath += ".cmd";
            }
            log.info("------ 开始编译native代码 ------");
            // 编译native代码。
            CommandHelper.exec(new String[]{ndkPath, "NDK_PROJECT_PATH=" + mOpt.jniDir.getParent()});
            
            log.info("------ 编译结束 ------");
        } else {
            throw new FileNotFoundException("未能通过环境变量\"ANDROID_NDK_HOME\"找到ndk目录！这个环境变量可能未设置。");
        }
    }

}
