package com.battle.colorwar.service;

public interface RabbitService {
    /**
     * 通过传过来的base的值，增加对应数量的交换机
     */
    boolean createExchange();
}
