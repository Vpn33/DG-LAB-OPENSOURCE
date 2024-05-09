package com.vpn33.entity;

/**
 * 心跳消息
 *
 * @Author vpn_33
 * @Date 2024/5/6 13:52
 */
public class HeartBeatMessage extends DataMessage {
    public HeartBeatMessage() {
        setType("heartbeat");
        setMessage("200");
    }

    public HeartBeatMessage(String clientId, String targetId) {
        super(clientId, targetId);
        setType("heartbeat");
        setMessage("200");
    }
}
