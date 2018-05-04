package buwai.android.shell.controlcentre;

public class CommonUtil {
    
    public static String convertClassName(String className){
        return getClassDesc(className).replaceAll("/", "_");
    }
    
    public static String getClassDesc(String className){
        return className.substring(1, className.lastIndexOf(';'));
    }

}
