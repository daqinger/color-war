package com.battle.colorwar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *  做一些规定
 *  基准为  40* 40
 *
 *  必须要放入缓存中的数据
 *      1.1 首页总的1000*1000颜色数据的缓存
 *      1.2 625个 40*40 分区的颜色缓存
 *
 *   必须要有的接口
 *      2.1 获取首页数据---直接从缓存中取出，不用组装的那种
 *      2.2 通过 (baseX,baseY) 获取分区颜色，不用组装的那种
 *      2.3 传入 (baseX,baseY,positionX,positionY,colorValue) ，将数据存储在总库，临时库等等
 *      2.4 获取所有的临时库中的数据，数据解析成 (baseX,baseY,positionX,positionY，colorValue)形式
 *      2.5 获取所有的数据
 *
 *   定时任务
 *      3.1 暂定 1 分钟更新 1.1 中总库的缓存
 *      3.2 暂定 1 分钟更新 1.2 中分区的缓存
 *      3.3 暂定 30s 使用rabbitmq 向对应交换机推送最近更新消息
 *
 *   其他
 *      4.1 临时库数据存活时间 40s
 *      4.2 交换机设置625个，命名方式 color.exchange.baseX.baseY
 *      4.3 存入首页以及分区的缓存，只需要排序好的颜色的值即可
 *      4.4 为了获取分区以及首页颜色，存入大map时，选用真实地址作为 key
 *      4.5 40s内的临时数据 key为了统一，也使用真实地址作为key
 *      4.6 3.3定时任务中，利用steam流分组，有选择的推送数据
 *
 *
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ColorWarApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColorWarApplication.class, args);
    }

}
