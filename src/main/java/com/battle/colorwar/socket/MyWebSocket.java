package com.battle.colorwar.socket;


import com.alibaba.fastjson.JSON;
import com.battle.colorwar.entity.ColorEntity;
import com.battle.colorwar.service.ColorService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@ServerEndpoint("/battle/colorsocket")
@Component
public class MyWebSocket {

    @Autowired
    ColorService colorService;

    /**
     * 存放所有在线的客户端
     */
    private static Map<String, Session> clients = new ConcurrentHashMap<>();
//    private String userId;

    @OnOpen
    public void onOpen(Session session) { // , @PathParam("userId") String userId
        log.info("打开了一个连接.....");
//        System.out.println("userId:"+userId);
//        System.out.println("session.getId():"+session.getId());
//        this.userId = userId;
//        User user = new User();
//        user.setUserMsg(new Msg(false,"有新人加入聊天",true));
//        sendAll(user.toString());

        //将新用户存入在线的组
        clients.put(session.getId(), session);
    }

    /**
     * 客户端关闭
     * @param session session
     */
    @OnClose
    public void onClose(Session session) {  // , @PathParam("userId") String userId
//        System.out.println("有用户断开了");
//        System.out.println("userId:"+userId);
//        System.out.println("session.getId():"+session.getId());

//        User user = new User();
//        user.setUserMsg(new Msg(false,"有用户断开聊天",true));
//        sendAll(user.toString());

        //将掉线的用户移除在线的组里
        clients.remove(session.getId());
    }

    /**
     * 发生错误
     * @param throwable e
     */
    @OnError
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    /**
     * 收到客户端发来消息
     * @param message  消息对象
     */
    @OnMessage
    public void onMessage(String message) {

//        log.info("websocket收到的消息:{}",message);
    }

    /**
     * 将最近的数据以10秒一次的速度推送给所有客户端
     */
    @XxlJob("sendTempColor")
    public void sendTempColorToAll() {
        log.info("sendAll方法被执行....");

        List<ColorEntity> tempColor = colorService.getTempColor();
        if (!CollectionUtils.isEmpty(tempColor)){
            for (Map.Entry<String, Session> sessionEntry : clients.entrySet()) {
                sessionEntry.getValue().getAsyncRemote().sendText(JSON.toJSONString(tempColor));
            }
        }
    }
}
