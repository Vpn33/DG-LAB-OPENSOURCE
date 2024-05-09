package com.vpn33.websocket;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vpn33.entity.DataMessage;
import com.vpn33.entity.HeartBeatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.awt.image.ConvolveOp;
import java.io.IOException;
import java.util.*;

/**
 * WebSocket服务器
 *
 * @author: vpn_33
 */
@Component
@ServerEndpoint(value = "/**") // web端用/连接  app端用/{connectId}连接  所以需要监听/** 否则app连接不上
public class WebSocketServer {
    protected static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
    // 储存已连接的用户及其标识
    protected static Map<String, Session> connections = Maps.newConcurrentMap();
    // 存储消息关系
    protected static Map<String, String> relations = Maps.newConcurrentMap();
    // 存储定时器
    protected static Map<String, Timer> clientTimers = Maps.newConcurrentMap();
    public static final Gson gson = new GsonBuilder().create();

    //默认发送时间1秒
    public static final Integer punishmentDuration = 5;

    // 默认一秒发送1次
    public static final Integer punishmentTime = 1;

    // 心跳定时器
    Timer heartTimer = null;

    @OnOpen
    public void onOpen(Session session) {
        String clientId = session.getId();
        connections.put(clientId, session);
        logger.debug("新的 WebSocket 连接已建立，标识符为: {}", session.getId());
        // 发送标识符给客户端（格式固定，双方都必须获取才可以进行后续通信：比如浏览器和APP）
        DataMessage da = new DataMessage();
        da.setType("bind");
        da.setClientId(clientId);
        da.setMessage("targetId");
        da.setTargetId("");
        send(da);

        // 启动心跳定时器（如果尚未启动）
        if (null == heartTimer) {
            heartTimer = new Timer();
            heartTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // 遍历 clients Map（大于0个链接），向每个客户端发送心跳消息
                    if (connections.size() > 0) {
                        logger.debug("关系池大小:" + relations.size() + " 连接池大小:" + connections.size() + " 发送心跳消息：" + new Date().toLocaleString());
                        for (String clientId : connections.keySet()) {
                            Session client = connections.get(clientId);
                            String targetId = relations.get(clientId);
                            if (ObjectUtils.isEmpty(targetId)) {
                                targetId = "";
                            }
                            HeartBeatMessage ht = new HeartBeatMessage(clientId, targetId);
                            send(client, ht);
                        }
                    }
                }
            }, 0, 60 * 1000);// 每分钟发送一次心跳消息
        }
    }

    private void send(DataMessage da) {
        Session session = connections.get(da.getClientId());
        send(session, da);
    }

    private void send(Session session, DataMessage da) {
        session.getAsyncRemote().sendText(gson.toJson(da));
    }

    private void send(Session session, String message) {
        session.getAsyncRemote().sendText(message);
    }

    @OnMessage
    public void onMessage(Session session, String text) {
        logger.debug("收到消息：{}", text);
        DataMessage data = null;
        try {
            data = gson.fromJson(text, DataMessage.class);
        } catch (Exception e) {
            // 非JSON数据处理
            DataMessage da = new DataMessage();
            da.setType("msg");
            da.setClientId("");
            da.setTargetId("");
            da.setMessage("403");
            send(session, da);
            return;
        }
        // 非法消息来源拒绝
        if (!connections.containsKey(data.getClientId()) && !connections.containsKey(data.getTargetId())) {
            DataMessage da = new DataMessage();
            da.setType("msg");
            da.setClientId("");
            da.setTargetId("");
            da.setMessage("404");
            send(session, da);
            return;
        }
        if (!ObjectUtils.isEmpty(data.getType()) && !ObjectUtils.isEmpty(data.getClientId()) && !ObjectUtils.isEmpty(data.getMessage()) && !ObjectUtils.isEmpty(data.getTargetId())) {
            // 优先处理绑定关系
            String type = data.getType();
            String clientId = data.getClientId();
            String targetId = data.getTargetId();
            String message = data.getMessage();
            switch (type) {
                case "bind":
                    // 服务器下发绑定关系
                    if (connections.containsKey(clientId) && connections.containsKey((targetId))) {
                        // relations的双方都不存在这俩id
                        if (!(relations.containsKey(clientId) || relations.containsKey(targetId)) || relations.containsValue(clientId) || relations.containsValue(targetId)) {
                            relations.put(clientId, targetId);
                            Session client = connections.get(clientId);
                            DataMessage sendData = new DataMessage(clientId, targetId);
                            sendData.setType("bind");
                            sendData.setMessage("200");
                            send(session, sendData);
                            send(client, sendData);
                        } else {
                            DataMessage da = new DataMessage(clientId, targetId);
                            da.setType("bind");
                            da.setMessage("400");
                            send(session, da);
                            return;
                        }
                    } else {
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("bind");
                        da.setMessage("401");
                        send(session, da);
                        return;
                    }
                    break;
                case "1":
                case "2":
                case "3":
                    // 服务器下发APP强度调节
                    if (relations.containsKey(clientId) && !relations.get(clientId).equals(targetId)) {
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("bind");
                        da.setMessage("402");
                        send(session, da);
                        return;
                    }
                    if (connections.containsKey(targetId)) {
                        Session client = connections.get(targetId);
                        Integer dataType = Integer.parseInt(data.getType());
                        Integer sendType = dataType - 1;
                        Integer sendChannel = null != data.getChannel() ? data.getChannel() : 1;
                        Integer sendStrength = dataType >= 3 ? data.getStrength() : 1; //增加模式强度改成1
                        String msg = "strength-" + sendChannel + "+" + sendType + "+" + sendStrength;
                        DataMessage sendData = new DataMessage(clientId, targetId);
                        sendData.setType("msg");
                        sendData.setMessage(msg);
                        send(client, sendData);
                    }
                    break;
                case "4":
                    // 服务器下发指定APP强度
                    if (relations.containsKey(clientId) && !relations.get(clientId).equals(targetId)) {
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("bind");
                        da.setMessage("402");
                        send(session, da);
                        return;
                    }
                    if (connections.containsKey(targetId)) {
                        Session client = connections.get(targetId);
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("msg");
                        da.setMessage(message);
                        send(client, da);
                    }
                    break;
                case "clientMsg":
                    // 服务端下发给客户端的消息
                    if (relations.containsKey(clientId) && !relations.get(clientId).equals(targetId)) {
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("bind");
                        da.setMessage("402");
                        send(session, da);
                        return;
                    }
                    if (ObjectUtils.isEmpty(data.getMessage2())) {
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("error");
                        da.setMessage("501");
                        send(session, da);
                        return;
                    }
                    if (connections.containsKey(targetId)) {
                        Integer sendtimeA = null != data.getTime1() ? data.getTime1() : punishmentDuration; // AB通道的执行时间可以独立
                        Integer sendtimeB = null != data.getTime2() ? data.getTime2() : punishmentDuration;
                        Session target = connections.get(targetId); //发送目标
                        DataMessage sendDataA = new DataMessage(clientId, targetId);
                        sendDataA.setType("msg");
                        sendDataA.setMessage("pulse-" + data.getMessage());
                        DataMessage sendDataB = new DataMessage(clientId, targetId);
                        sendDataB.setType("msg");
                        sendDataB.setMessage("pulse-" + data.getMessage2());
                        Integer totalSendsA = punishmentTime * sendtimeA;
                        Integer totalSendsB = punishmentTime * sendtimeB;
                        Integer timeSpace = 1000 / punishmentTime;

                        logger.debug("消息发送中，总消息数A：" + totalSendsA + "总消息数B：" + totalSendsB + "持续时间A：" + sendtimeA + "持续时间B：" + sendtimeB);
                        if (clientTimers.containsKey(clientId)) {
                            // 计时器尚未工作完毕, 清除计时器且发送清除APP队列消息，延迟150ms重新发送新数据
                            // 新消息覆盖旧消息逻辑
                            session.getAsyncRemote().sendText("当前有正在发送的消息，覆盖之前的消息");

                            Timer timerId = clientTimers.get(clientId);
                            clearInterval(clientId, timerId); // 清除定时器
                            clientTimers.remove(clientId); // 清除 Map 中的对应项

                            // 发送APP波形队列清除指令
                            DataMessage clearDataA = new DataMessage(clientId, targetId);
                            clearDataA.setType("msg");
                            clearDataA.setMessage("clear-1");
                            DataMessage clearDataB = new DataMessage(clientId, targetId);
                            clearDataB.setType("msg");
                            clearDataB.setMessage("clear-2");
                            send(target, clearDataA);
                            send(target, clearDataB);
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            delaySendMsg(clientId, session, target, sendDataA, sendDataB, totalSendsA, totalSendsB, timeSpace);
                        } else {
                            // 不存在未发完的消息 直接发送
                            delaySendMsg(clientId, session, target, sendDataA, sendDataB, totalSendsA, totalSendsB, timeSpace);
                        }

                    } else {
                        logger.debug("未找到匹配的客户端，clientId: {}", clientId);
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("msg");
                        da.setMessage("404");
                        send(session, da);
                    }
                    break;
                default:
                    // 未定义的普通消息
                    if (relations.containsKey(clientId) && !relations.get(clientId).equals(targetId)) {
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("bind");
                        da.setMessage("402");
                        send(session, da);
                        return;
                    }
                    if (connections.containsKey(clientId)) {
                        Session client = connections.get(clientId);
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType(data.getType());
                        da.setMessage(data.getMessage());
                        send(client, da);
                    } else {
                        // 未找到匹配的客户端
                        DataMessage da = new DataMessage(clientId, targetId);
                        da.setType("msg");
                        da.setMessage("404");
                        send(session, da);
                    }
                    break;
            }
        }
    }

    public void clearInterval(String clientId, Timer timer) {
        timer.cancel();
        clientTimers.remove(clientId); // 删除对应的定时器
        timer = null;
    }

    public void delaySendMsg(String clientId, Session client, Session target, DataMessage sendDataA, DataMessage sendDataB, Integer totalSendsA, Integer totalSendsB, Integer timeSpace) {
        // 发信计时器 AB通道会分别发送不同的消息和不同的数量 必须等全部发送完才会取消这个消息 新消息可以覆盖
        send(target, sendDataA);//立即发送一次AB通道的消息
        send(target, sendDataB);
        totalSendsA--;
        totalSendsB--;
        if (totalSendsA > 0 || totalSendsB > 0) {
            Timer timer = new Timer();
            DgLabTimerTask task = new DgLabTimerTask(client, target, sendDataA, sendDataB, totalSendsA, totalSendsB, (k) -> {
                clearInterval(clientId, timer);
            });
            timer.scheduleAtFixedRate(task, 0, timeSpace.longValue());// 每隔频率倒数触发一次定时器
            // 存储clientId与其对应的timerId
            clientTimers.put(clientId, timer);
        }
    }


    /**
     * 关闭连接
     */
    @OnClose
    public void onClosing(Session session) throws IOException {
        // 连接关闭时，清除对应的 clientId 和 WebSocket 实例
        logger.debug("WebSocket 连接已关闭");
        // 遍历 clients Map，找到并删除对应的 clientId 条目
        String clientId = session.getId();
        logger.debug("断开的client id:" + clientId);
        for (String key : relations.keySet()) {
            String value = relations.get(key);
            if (key.equals(clientId)) {
                //网页断开 通知app
                String appid = value;
                Session appClient = connections.get(appid);
                DataMessage da = new DataMessage(clientId, appid);
                da.setType("break");
                da.setMessage("209");
                send(appClient, da);
                appClient.close(); // 关闭当前 WebSocket 连接
                relations.remove(key); // 清除关系
                logger.debug("对方掉线，关闭" + appid);
            } else if (value.equals(clientId)) {
                // app断开 通知网页
                Session webClient = connections.get(key);
                DataMessage da = new DataMessage(key, clientId);
                da.setType("break");
                da.setMessage("209");
                send(webClient, da);
                webClient.close(); // 关闭当前 WebSocket 连接
                relations.remove(key); // 清除关系
                logger.debug("对方掉线，关闭" + clientId);
            }
        }
        connections.remove(clientId); //清除ws客户端
        logger.debug("已清除" + clientId + " ,当前size: " + connections.size());
    }

    /**
     * 异常处理
     *
     * @param throwable
     */
    @OnError
    public void onError(Throwable throwable, Session session) {
        // java.io.IOException: 远程主机强迫关闭了一个现有的连接。 这个错误是由于连接直接被关闭 无需处理 依然会自动回调到onClose关闭
        if (throwable instanceof java.io.IOException) {
            return;
        }
        logger.error("WebSocket 异常: {}", throwable);
        // 在此通知用户异常，通过 WebSocket 发送消息给双方
        String clientId = session.getId();
        if (ObjectUtils.isEmpty(clientId)) {
            logger.debug("无法找到对应的 clientId");
            return;
        }
        // 构造错误消息
        String errorMessage = "WebSocket 异常: " + throwable.getMessage();
        for (String key : relations.keySet()) {
            // 遍历关系 Map，找到并通知没掉线的那一方
            String value = relations.get(key);
            if (key.equals(clientId)) {
                // 通知app
                String appid = relations.get(key);
                Session appClient = connections.get(appid);
                DataMessage da = new DataMessage(clientId, appid);
                da.setType("error");
                da.setMessage("500");
                send(appClient, da);
            } else if (value.equals(clientId)) {
                // 通知网页
                Session webClient = connections.get(key);
                DataMessage da = new DataMessage(key, clientId);
                da.setType("error");
                da.setMessage(errorMessage);
                send(webClient, da);
            }
        }
    }

}