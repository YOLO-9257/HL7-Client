package com.hl7.client.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * hl7数据保存入库逻辑入参对象
 */
@Data
public class Hl7StorageModel implements Serializable {
    /**
     * 机构编码
     */
    private String organizationCode;
    /**
     * 仪器编号
     */
    private String instrumentCode;
    /**
     * 外部仪器编码
     */
    private String outCode;
    /**
     * 机构类型
     */
    private String orgType;
    /**
     * $样本号(只需要四位，多余4位的话,会从倒数第四位开始截取)
     */
    private String sampleCode;
    /**
     * 结果时间-用于显示检测的时间
     */
    private Date itemTime;
    /**
     * $条码号
     */
    private String barcode;
    /**
     * $测试日期 yyyyMMdd 用作生成样本号
     */
    private String testDate;
    /**
     * $结果列表
     */
    private List<Item> itemList;
    /**
     * 图片结果
     */
    private List<Image> imageList;
    /**
     * 是否微生物仪器标识 1：是
     */
    private Integer isMir;

    /**
     * 项目结果
     */
    @Data
    public static class Item implements Serializable {
        /**
         * 通道号
         */
        private String channelCode;
        /**
         * 结果
         */
        private String result;
        /**
         * 敏感性
         */
        private String susceptibility;

    }

    /**
     * 图片结果
     */
    @Data
    public static class Image implements Serializable {
        /**
         * base64图片结果,格式必须是  data:image/png;base64,xxxxx
         */
        private String base64str;

        /**
         * 通道号
         */
        private String channelCode;
    }
}
