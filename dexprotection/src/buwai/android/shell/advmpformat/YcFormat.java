package buwai.android.shell.advmpformat;

import java.util.List;

/**
 * Created by buwai on 2015/4/2.
 */
public class YcFormat {

    public static final String MAGIC = "YC0000";

    public static final int SIZE_HEADER = MAGIC.length() + 4 + 4 + 4 + 4 + 4;

    /**
     * 文件头。
     */
    public YcFormat.Header header;

    /**
     *
     */
    public List<YcFormat.AdvmpMethod> methods;

    /**
     * 抽离器数据。
     */
    public List<YcFormat.SeparatorData> separatorDatas;

    public static class Header {

        /**
         * 魔术字。
         */
        public String magic;

        /**
         * 文件头大小。
         */
        public int size;

        /**
         * Method结构的个数。
         */
        public int methodSize;

        /**
         * Method数据距离文件起始的偏移。
         */
        public int methodOffset;

        /**
         * SeparatorData结构的个数。
         */
        public int separatorDataSize;

        /**
         * SeparatorData数据距离文件起始的偏移。
         */
        public int separatorDataOffset;

    }

    /**
     * TODO 这个结构要被抛弃。
     * 这个类中的字段对应dex文件中相应方法的信息。
     * 这些信息保证了在advmp中invoke方法时，可以
     * 通过jni函数找到该方法。
     */
    public static class AdvmpMethod {

        /**
         * 方法在dex中的索引。
         */
        public int methodIndex;

        /**
         * 当前结构的大小。
         */
        public int size;

        /**
         * 方法的访问标志。
         */
        public int accessFlag;

        /**
         * 方法所属类。
         * 以"java/lang/System"这样的格式保存。
         */
        public StringItem definingClass;

        /**
         * 方法名。
         */
        public StringItem name;

        /**
         * 方法签名。
         */
        public StringItem sig;
    }

    /**
     * TODO 这个结构要被缩小。
     * 抽离器数据。
     */
    public static class SeparatorData {

        /**
         * 这个索引表示当前SeparatorData结构在
         * SeparatorData结构数组中的索引。
         */
        public int methodIndex;
        
        /**
         * 这个索引表示当前SeparatorData对应的函数在dex文件中的索引
         * */
        public int methodIndexForDexFile;

        /**
         * 当前结构的大小。
         */
        public int size;

        /**
         * 方法的访问标志。
         */
        public int accessFlag;

        /**
         * 参数个数。
         */
        public int paramSize;

        /**
         * 寄存器个数。
         */
        public int registerSize;

        /**
         * 参数的短类型描述。
         */
        public StringItem paramShortDesc;

        /**
         * 指令数组元素个数。
         */
        public int instSize;

        /**
         * 抽取出来的方法指令。
         */
        public short[] insts;

        /**
         * 方法的try...catch块。
         */
        //public List<TryBlock> tryBlocks;

        /**
         * 方法的debug信息。
         */
        //public List<DebugItem> debugItems;

    }

}
