package com.hl7.client.domain.model;

/*
 * 目标数据库枚举
 * 用于指定入库的数据源
 */
import lombok.Getter;

@Getter
public enum TargetDatabaseCode {
    AID,
    EMIS,
    ;
    public static TargetDatabaseCode fromString(String status) {
        if (status == null) {
            return AID;
        }

        try {
            return TargetDatabaseCode.valueOf(status);
        } catch (IllegalArgumentException e) {
            return AID;
        }
    }
}
