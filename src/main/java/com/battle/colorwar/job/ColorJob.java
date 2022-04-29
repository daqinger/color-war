package com.battle.colorwar.job;

import com.alibaba.fastjson.JSON;
import com.battle.colorwar.constant.ColorConstant;
import com.battle.colorwar.entity.ColorEntity;
import com.battle.colorwar.entity.MongoEntity;
import com.battle.colorwar.service.ColorService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.websocket.Session;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 定时任务
 */
@Component
@Slf4j
public class ColorJob {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    ColorService colorService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 每10秒向rabbitmq发送一次数据
     */
    @XxlJob("sendTempColorToRabbit")
    public void sendTempColorToRabbit() {
        log.info("定时任务向rabbitmq推送分库数据开始...");
        List<ColorEntity> tempColor = colorService.getTempColor();
        if (!CollectionUtils.isEmpty(tempColor)){
            log.info("tempColor{}",tempColor.get(0).toString());
        }

        // 先通过baseX,baseY分组
        Map<String, List<ColorEntity>> map = tempColor.stream().collect(
                Collectors.groupingBy(
                        item -> item.getBaseX() + "." + item.getBaseY()
                ));

        map.forEach((key,tempColorList)->{
            log.info("key:{}",key);
            if (!CollectionUtils.isEmpty(tempColorList)){
                rabbitTemplate.convertAndSend(ColorConstant.COLOR_EXCHANGE_PRE+key,"",tempColorList);
            }
        });
        log.info("定时任务向rabbitmq推送分库数据结束...");

    }


    /**
     * 定时任务  1分钟一次  修改  分库 和 总库的缓存数据   其中，总库只需要一个，分库需要修改400个
     *      MAIN_COLOR_CACHE  总库
     *
     *      SUB_COLOR_CACHE_PRE  分库
     *
     */
    @XxlJob("updateColorCache")
    public void updateColorCache(){
        log.info("定时任务更新分库总库数据开始...");

        List<MongoEntity> all = mongoTemplate.findAll(MongoEntity.class);

//        all.stream().forEach(item->{
//            log.info(item.toString());
//        });



        Map<String, String> colorMap = all.stream().collect(Collectors.toMap(MongoEntity::getKey, MongoEntity::getValue));


        // 所有的数据 ....这就不对了
//        Map<String, String> colorMap = colorService.getTotalColorHolder();

        // 首先 存储总的...
        List<List<String>> mainColorCache = new LinkedList<>(); // 链式存储，提升速度
        for (int i = 0; i < ColorConstant.BASE_NUMBER*ColorConstant.POSITION_NUMBER; i++) {
            List<String> tempList = new LinkedList<>();
            for (int j = 0; j < ColorConstant.BASE_NUMBER*ColorConstant.POSITION_NUMBER; j++) {
                String color =  colorMap.get(i+":"+j);
                tempList.add(StringUtils.hasLength(color)?color:"#ffffff");     // 默认白色画布
            }
            mainColorCache.add(tempList);
        }
        // 存储
        stringRedisTemplate.opsForValue().set(ColorConstant.MAIN_COLOR_CACHE, JSON.toJSONString(mainColorCache));

        //轮到 存储分区...
        // 先来两个循环
        for (int i = 0; i < ColorConstant.BASE_NUMBER; i++) {
            for (int j = 0; j < ColorConstant.BASE_NUMBER; j++) {
                // 目前baseX  baseY 即为 i,j
                //获取属于自己区域的那些内容
                List<List<String>> subColorCache = new LinkedList<>();
                for (int k = 0; k < ColorConstant.POSITION_NUMBER; k++) {
                    List<String> tempList = new LinkedList<>();
                    for (int l = 0; l < ColorConstant.POSITION_NUMBER; l++) {
                        String color = colorMap.get((i*ColorConstant.POSITION_NUMBER +k)+":"+(j*ColorConstant.POSITION_NUMBER +l));
                        tempList.add(StringUtils.hasLength(color)?color:"#ffffff");
                    }
                    subColorCache.add(tempList);
                }
                // 在这里就存储
                stringRedisTemplate.opsForValue().set(ColorConstant.SUB_COLOR_CACHE_PRE+i+":"+j, JSON.toJSONString(subColorCache));
            }
        }

        log.info("定时任务更新分库总库数据结束...");

    }

}
