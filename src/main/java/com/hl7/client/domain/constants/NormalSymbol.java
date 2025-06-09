package com.hl7.client.domain.constants;

/**
 * 常用特殊符号
 */
public class NormalSymbol {
    /**
     * 换行符,常用分割hl7数据(13+10)
     */
    public static final String NEW_LINE = "\n";
    /**
     * 回车符  13
     */
    public static final String ENTER = "\r";
    /**
     * 回车符+换行符
     */
    public static final String ENTER_NEW_LINE = "\r\n";
    /**
     * 空格符
     */
    public static final String SPACE = " ";
    /**多个空格
     *
     */
    public static final String SPACEMORE = "\\s+";
    /**
     * &符
     */
    public static final String AND = "&";
    /**
     * 常用不规范数据的segment分割 开头
     */
    public static final String PREFIX="\u0002";

    public static final String START1="\u0001";
    /**
     * 常用不规范数据的segment分割 结尾
     */
    public static final String SUFFIX = "\u0003";
    /**
     * 传输结束
     */
    public static final String TRANSFORM_END="\u0004";
    /**
     * hl7标准分割符
     */
    public static final String HL7_SPLIT = "\\|";
    /**
     * hl7 <EB> 对应 16进制
     */
    public static final String HL7_EB = "\u001C";
    /**
     * &
     */
    public static final String HL7_26 = "\u0026";
    /**
     * hl7 <SB> 对应 16进制
     */
    public static final String HL7_SB = "\u000B";
    /**
     * 匹配生化仪
     */
    public static final String SUFFIX_ENGLISH = "\u0003[A-Z]";
    /**
     * hl7 <E> 对应 16进制
     */
    public static final String HL7_E = "\u0045";
    /**
     * hl7 <1A> 对应 16进制
     */
    public static final String HL7_1A = "\u001A";

    /**
     * 尖尖头
     */
    public static final String DIVISION = "\\^";
    /**
     * 逗号
     */
    public static final String COMMA = ",";
    /**
     * 横杠杠
     */
    public static final String ROD="\\-";
    /**
     * 星星头
     */
    public static final String DIVISIONXING ="\\*";
    /**
     * 斜杠杠
     */
    public static final String SLASH ="\\/";

//    public static final String HL7_D ="\u0014";

    /**
     *0006
     */
    public static final String SIX_END = "\u0006";

    /**
     * \t
     */
    public static final String TAB = "\t";

    /**
     * End分隔
     */
    public static final String H900 = "End";

    /**
     * #分割
     */
    public static final String MICOR_END="#";

    /**
     * ##分割
     */
    public static final String MICOR_DOUBLE_END="##";

    /**
     * NTE分割
     */
    public static final String FUS2000_END="NTE";

    /**
     * 分割<LF>
     */
    public static final String ECL8000_END="<LF>";

    public static final String ETX="\u0014";

    public static final String LF="\u0010";

    public static final String B="\u0008";

    public static final String EQUAL = "=";

    public static final String Dian = ".";

    public static String HB1000End="END";

    public static String FS="\u001C";

    public static String GS="\u001D";

    public static String RS="\u001E";

    public static String ADD="+";
    /**
     * 冒号
     */
    public static final String COLON=":";

    public static final String F="\f";

    /**设备控制1
     */
    public static final String DC1 ="\u0017";
}
