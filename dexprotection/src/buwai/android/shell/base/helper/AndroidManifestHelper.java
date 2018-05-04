package buwai.android.shell.base.helper;

import java.io.File;
import java.io.IOException;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.irdeto.axml.AXMLUtil;

import buwai.android.shell.base.TypeDescription;

public class AndroidManifestHelper {

    public static String getPackageName(String plainXML){
        String packageName = null;
        Document document = null;
        try {
             document = DocumentHelper.parseText(plainXML);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null != document)
        {
            Node para = document.selectSingleNode("/manifest");
            Element e = (Element)para;
            Attribute attribute = e.attribute("package");
            packageName = attribute.getValue();
        }
        return packageName;
    }
    
    public static String getApplicationName(String plainXML){
        String applicationName = null;
        Document document = null;
        try {
            document = DocumentHelper.parseText(plainXML);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null != document)
        {
            Node para = document.selectSingleNode("/manifest/application");
            Element e = (Element)para;
            Attribute attribute = e.attribute("name");
            if(null != attribute){
                applicationName = attribute.getValue();
            }
        }
        return applicationName;
    }
    
    public static TypeDescription findFirstClass(File manifestFile) throws IOException {
        TypeDescription ret = null;
        String className = null;
        String plainXML = AXMLUtil.getXMLString(manifestFile.getPath());
        String packageName = getPackageName(plainXML);
        String applicationName = getApplicationName(plainXML);
        if(null != applicationName){
            if(applicationName.startsWith(".")){
                className =  packageName + applicationName;
            }else{
                 className = applicationName;
            }
            ret = TypeDescriptionHelper.convertByFullClassName(className);
        }
        return ret;
    }
}
