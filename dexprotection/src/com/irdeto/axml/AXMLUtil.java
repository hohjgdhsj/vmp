package com.irdeto.axml;

import java.io.FileInputStream;

public class AXMLUtil {
    
    public static String getXMLString(String axmlPath){
        String ret = null;
        try {
            ret = parseXMLString(axmlPath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }
    
    private static String parseXMLString(String xmlPath) throws Exception{
        StringBuffer sb = new StringBuffer();
        AXmlResourceParser parser=new AXmlResourceParser();
        parser.open(new FileInputStream(xmlPath));
        StringBuilder indent=new StringBuilder(10);
        final String indentStep="   ";
        while (true) {
            int type=parser.next();
            if (type==XmlPullParser.END_DOCUMENT) {
                break;
            }
            switch (type) {
                case XmlPullParser.START_DOCUMENT:
                {
                    log(sb, "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                    break;
                }
                case XmlPullParser.START_TAG:
                {
                    log(sb, "%s<%s%s",indent,
                        getNamespacePrefix(parser.getPrefix()),parser.getName());
                    indent.append(indentStep);
                    
                    int namespaceCountBefore=parser.getNamespaceCount(parser.getDepth()-1);
                    int namespaceCount=parser.getNamespaceCount(parser.getDepth());
                    for (int i=namespaceCountBefore;i!=namespaceCount;++i) {
                        log(sb, "%sxmlns:%s=\"%s\"",
                            indent,
                            parser.getNamespacePrefix(i),
                            parser.getNamespaceUri(i));
                    }
                    
                    for (int i=0;i!=parser.getAttributeCount();++i) {
                        log(sb, "%s%s%s=\"%s\"",indent,
                            getNamespacePrefix(parser.getAttributePrefix(i)),
                            parser.getAttributeName(i),
                            getAttributeValue(parser,i));
                    }
                    log(sb, "%s>",indent);
                    break;
                }
                case XmlPullParser.END_TAG:
                {
                    indent.setLength(indent.length()-indentStep.length());
                    log(sb, "%s</%s%s>",indent,
                        getNamespacePrefix(parser.getPrefix()),
                        parser.getName());
                    break;
                }
                case XmlPullParser.TEXT:
                {
                    log(sb, "%s%s",indent,parser.getText());
                    break;
                }
            }
        }
        return sb.toString();
    }
    
    private static String getNamespacePrefix(String prefix) {
        if (prefix==null || prefix.length()==0) {
            return "";
        }
        return prefix+":";
    }
    
    private static String getAttributeValue(AXmlResourceParser parser,int index) {
        int type=parser.getAttributeValueType(index);
        int data=parser.getAttributeValueData(index);
        if (type==TypedValue.TYPE_STRING) {
            return parser.getAttributeValue(index);
        }
        if (type==TypedValue.TYPE_ATTRIBUTE) {
            return String.format("?%s%08X",getPackage(data),data);
        }
        if (type==TypedValue.TYPE_REFERENCE) {
            return String.format("@%s%08X",getPackage(data),data);
        }
        if (type==TypedValue.TYPE_FLOAT) {
            return String.valueOf(Float.intBitsToFloat(data));
        }
        if (type==TypedValue.TYPE_INT_HEX) {
            return String.format("0x%08X",data);
        }
        if (type==TypedValue.TYPE_INT_BOOLEAN) {
            return data!=0?"true":"false";
        }
        if (type==TypedValue.TYPE_DIMENSION) {
            return Float.toString(complexToFloat(data))+
                DIMENSION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
        }
        if (type==TypedValue.TYPE_FRACTION) {
            return Float.toString(complexToFloat(data))+
                FRACTION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
        }
        if (type>=TypedValue.TYPE_FIRST_COLOR_INT && type<=TypedValue.TYPE_LAST_COLOR_INT) {
            return String.format("#%08X",data);
        }
        if (type>=TypedValue.TYPE_FIRST_INT && type<=TypedValue.TYPE_LAST_INT) {
            return String.valueOf(data);
        }
        return String.format("<0x%X, type 0x%02X>",data,type);
    }
    
    private static String getPackage(int id) {
        if (id>>>24==1) {
            return "android:";
        }
        return "";
    }

    private static void log(StringBuffer sb, String format,Object...arguments) {
        sb.append(String.format(format, arguments));
        sb.append("\r\n");
    }
    
    public static float complexToFloat(int complex) {
        return (float)(complex & 0xFFFFFF00)*RADIX_MULTS[(complex>>4) & 3];
    }
    
    private static final float RADIX_MULTS[]={
        0.00390625F,3.051758E-005F,1.192093E-007F,4.656613E-010F
    };
    private static final String DIMENSION_UNITS[]={
        "px","dip","sp","pt","in","mm","",""
    };
    private static final String FRACTION_UNITS[]={
        "%","%p","","","","","",""
    };

}
