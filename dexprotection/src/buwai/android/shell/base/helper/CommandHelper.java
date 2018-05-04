package buwai.android.shell.base.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import buwai.android.shell.controlcentre.MyLog;

/**
 * Created by buwai on 2015/4/1.
 */
public class CommandHelper {

    private static final Logger log = Logger.getLogger(CommandHelper.class);

    public static int exec(String[] args) {
        int ret = 0;
        try {
            String argslog = "";
            for (String str : args) {
                argslog += " " + str;
            }
            log.info("Start running command: " + argslog);
            Process process = Runtime.getRuntime().exec(args);

            // any error message?
            StreamRedirector err = new StreamRedirector(process.getErrorStream(), StreamRedirector.TYPE_ERROR);
            StreamRedirector info = new StreamRedirector(process.getInputStream(), StreamRedirector.TYPE_INFO);

            err.start();
            info.start();
            ret = process.waitFor();

            log.info("end...");

        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return ret;
    }

    public static class StreamRedirector extends Thread {
        public static final int TYPE_ERROR = 0;
        public static final int TYPE_INFO = 1;
        InputStream mIs;
        int mType;

        public StreamRedirector(InputStream is, int type) {
            this.mIs = is;
            this.mType = type;
        }

        public void run() {
            try (InputStreamReader isr = new InputStreamReader(mIs);
                BufferedReader br = new BufferedReader(isr);) {
                String line = null;
                //Log mylog = new Log("c://log.txt");
                //mylog.init(false);
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    //mylog.writeLine(line);
                    if (TYPE_INFO == mType) {
                        log.info(line);
                    } else if (TYPE_ERROR == mType) {
                        log.error(line);
                    }
                }
                //mylog.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}
