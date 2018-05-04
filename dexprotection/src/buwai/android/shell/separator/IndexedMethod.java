package buwai.android.shell.separator;

import org.jf.dexlib2.iface.Method;

public class IndexedMethod {
    int index;
    Method method;
    
    public IndexedMethod(int index, Method method) {
        this.index = index;
        this.method = method;
    }
    
}
