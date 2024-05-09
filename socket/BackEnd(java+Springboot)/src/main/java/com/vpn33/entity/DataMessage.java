package com.vpn33.entity;

import lombok.Data;

/**
 * 消息体
 *
 * @Author vpn_33
 * @Date 2024/4/25 18:00
 */
@Data
public class DataMessage {
    private String type;
    private String clientId;
    private String targetId;
    private String message;
    private String message2;
    private Integer time1;
    private Integer time2;
    private Integer channel;
    private Integer strength;

    public DataMessage() {

    }

    public DataMessage(String clientId, String targetId) {
        this.clientId = clientId;
        this.targetId = targetId;
    }
}
