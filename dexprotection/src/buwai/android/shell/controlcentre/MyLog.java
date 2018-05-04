package buwai.android.shell.controlcentre;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class MyLog {
    
    private BufferedWriter bw;
    private String logPath;
    
    public MyLog(String logPath)
    {
        this.logPath = logPath;
    }

    public void init(boolean isAppend)
    {
        try {
            bw = new BufferedWriter(new FileWriter(logPath, isAppend));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void writeLine(String log)
    {
        if(bw == null)
        {
            System.out.println("Log 工具尚未初始化!");
        }
        try {
            System.out.println(log);
            bw.write(log+"\r\n");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void close()
    {
        if(null != bw)
        {
            try {
                bw.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            bw = null;
        }
    }
}
