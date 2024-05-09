package com.vpn33.websocket;

import com.vpn33.entity.DataMessage;

import javax.websocket.Session;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * 定时任务
 *
 * @Author vpn_33
 * @Date 2024/5/6 15:56
 */
public class DgLabTimerTask extends TimerTask {
    Session client;
    Session target;
    Integer totalSendsA;
    Integer totalSendsB;
    DataMessage sendDataA;
    DataMessage sendDataB;
    Consumer<Object> timerConsumer;

    public DgLabTimerTask(Session client, Session target, DataMessage sendDataA, DataMessage sendDataB, Integer totalSendsA, Integer totalSendsB, Consumer<Object> timerConsumer) {
        this.client = client;
        this.target = target;
        this.sendDataA = sendDataA;
        this.sendDataB = sendDataB;
        this.totalSendsA = totalSendsA;
        this.totalSendsB = totalSendsB;
        this.timerConsumer = timerConsumer;
    }

    private void send(Session session, DataMessage da) {
        session.getAsyncRemote().sendText(WebSocketServer.gson.toJson(da));
    }

    private void send(Session session, String message) {
        session.getAsyncRemote().sendText(message);
    }

    @Override
    public void run() {
        if (totalSendsA > 0) {
            send(target, sendDataA);
            totalSendsA--;
        }
        if (totalSendsB > 0) {
            send(target, sendDataB);
            totalSendsB--;
        }
        // 如果达到发送次数上限，则停止定时器
        if (totalSendsA <= 0 && totalSendsB <= 0) {
            send(client, "发送完毕");
            // 删除对应的定时器
            timerConsumer.accept(null);
        }
    }
}
