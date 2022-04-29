package com.battle.colorwar.service.impl;

import com.battle.colorwar.constant.ColorConstant;
import com.battle.colorwar.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RabbitServiceImpl implements RabbitService {

    @Autowired
    AmqpAdmin amqpAdmin;

    /**
     * 通过传过来的base的值，增加对应数量的交换机
     *
     */
    @Override
    public boolean createExchange() {

        // 规定从 0 开始计数
        for (int i = 0; i < ColorConstant.BASE_NUMBER; i++) {
            for (int j = 0; j < ColorConstant.BASE_NUMBER; j++) {
                // 循环添加
                //创建一个fanout类型的交换机  名字 ，是否持久化，是否自动删除
                FanoutExchange fanoutExchange = new FanoutExchange(ColorConstant.COLOR_EXCHANGE_PRE + i + "." + j, true, false);
                //声明一个交换机
                amqpAdmin.declareExchange(fanoutExchange);
                log.info("交换机创建成功");
            }
        }
        return true;
    }
}
