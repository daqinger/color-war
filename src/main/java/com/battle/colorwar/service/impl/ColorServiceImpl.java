package com.battle.colorwar.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.battle.colorwar.constant.ColorConstant;
import com.battle.colorwar.entity.ColorEntity;
import com.battle.colorwar.entity.MongoEntity;
import com.battle.colorwar.service.ColorService;
import com.battle.colorwar.vo.CheatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ColorServiceImpl implements ColorService {

    @Autowired
    RedisTemplate redisTemplate;


    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @Resource
    private ThreadPoolExecutor executor;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    @Transactional
    public boolean cheat(CheatVo cheatVo) throws IOException {
        String imgPath = cheatVo.getPicUrl();
        BufferedImage image = ImageIO.read(new FileInputStream(imgPath));
        log.info("宽:{},高{}",image.getWidth(),image.getHeight());
        int width = image.getWidth();
        int height = image.getHeight();

        int jump = width/cheatVo.getSize() >height/cheatVo.getSize()?width/cheatVo.getSize():height/cheatVo.getSize();

        for (int i = 0; i < width/jump; i++) {
            for (int j = 0; j < height/jump; j++) {
                int rgb = image.getRGB(i*jump, j*jump);
                String colorValue = "#"+Integer.toHexString(rgb).substring(2);
                String key  = (cheatVo.getBeginX()+j)+":"+(cheatVo.getBeginY()+i);
                MongoEntity mongoEntity = new MongoEntity(key,colorValue);
                Query query = new Query();
                query.addCriteria(Criteria.where("key").is(mongoEntity.getKey()));
                FindAndReplaceOptions findAndReplaceOptions = new FindAndReplaceOptions().upsert();
                mongoTemplate.findAndReplace(query,mongoEntity,findAndReplaceOptions);
            }
        }
        return true;

    }



    /**
     * 获取首页数据---直接从缓存中取出，不用组装的那种
     */
    @Override
    public List<List<String>>  getMainColorCache() {
        String mainColorCache = stringRedisTemplate.opsForValue().get(ColorConstant.MAIN_COLOR_CACHE);
        List<List<String>> result = JSON.parseObject(mainColorCache, new TypeReference<List<List<String>>>() {});
        return result;
    }

    /**
     * 通过 (baseX,baseY) 获取分区颜色，不用组装的那种
     *
     * @param baseX
     * @param baseY
     */
    @Override
    public List<List<String>> getSubColorCache(Integer baseX, Integer baseY) {
        String mainColorCache = stringRedisTemplate.opsForValue().get(ColorConstant.SUB_COLOR_CACHE_PRE+baseX+":"+baseY);
        List<List<String>> result = JSON.parseObject(mainColorCache, new TypeReference<List<List<String>>>() {});
        return result;
    }

    /**
     * 查询最大的map内的所有k-v
     */
    @Override
    public Map<String, String> getTotalColorHolder() {
        return redisTemplate.boundHashOps(ColorConstant.TOTAL_COLOR_HOLDER).entries();
    }

    /**
     * 传入 (baseX,baseY,positionX,positionY,colorValue) ，将数据存储在总库，临时库等等
     *
     * @param colorEntity
     */
    @Override
    public boolean saveColor(ColorEntity colorEntity) throws ExecutionException, InterruptedException {

        //存入到总redis中 https://blog.csdn.net/lydms/article/details/105224210
        log.info("传入的color值：{}",colorEntity.toString());

        // 异步编排 存入主库
        CompletableFuture<Void> saveColorToMainDB = CompletableFuture.runAsync(() -> {
            // 存入 total:color:holder
//            BoundHashOperations colorHolder = redisTemplate.boundHashOps(ColorConstant.TOTAL_COLOR_HOLDER);
//
//            //规定存入主库的
//            colorHolder.put(colorEntity.getTrueKey(), colorEntity.getColorValue());
//            redisTemplate.expire(ColorConstant.TOTAL_COLOR_HOLDER,30,TimeUnit.DAYS);


            MongoEntity mongoEntity = new MongoEntity(colorEntity.getTrueKey(),colorEntity.getColorValue());
            Query query = new Query();
            query.addCriteria(Criteria.where("key").is(mongoEntity.getKey()));

//            Update update = new Update();
//            update.push("value", mongoEntity.getValue());
            FindAndReplaceOptions findAndReplaceOptions = new FindAndReplaceOptions().upsert();
            mongoTemplate.findAndReplace(query,mongoEntity,findAndReplaceOptions);
//            mongoTemplate.upsert(query, update, MongoEntity.class);


            log.info("存入总库！key为({}),值为{}",colorEntity.getTrueKey(),colorEntity.getColorValue());
        },executor);

        //将最近更新的key存入临时库,公用一个临时库
        CompletableFuture<Void> saveColorToTempDB = CompletableFuture.runAsync(() -> {
            // 存入key值，，要找的话，，先找key，再找value ,最近更新的所有的数据
            redisTemplate.opsForSet().add(ColorConstant.TEMP_COLOR_HOLDER, colorEntity.getTempKey());
            log.info("存入临时库！存入的key为{}",colorEntity.getTempKey());
        },executor);

        // 存放临时数据
        CompletableFuture<Void> saveTempColor = CompletableFuture.runAsync(() -> {
            // 存放  tempColor
            redisTemplate.boundValueOps(colorEntity.getTempKey()).set(colorEntity.getColorValue(),40, TimeUnit.SECONDS);
            log.info("存入临时数据！存入的key为{},值为{}",colorEntity.getTempKey(),colorEntity.getColorValue());
        },executor);
        // 等待执行完成
        CompletableFuture.allOf(saveColorToMainDB, saveColorToTempDB,saveTempColor).get();
        log.info("全部执行结束..");
        return true;
    }

    /**
     * 获取所有color
     * 准备数据，为定时任务  1 分钟更新总库 main:color:cache 的缓存
     */
    @Override
    public List<List<String>> getAllColor() {
        // 先获取主库内所有的数据
        BoundHashOperations colorHolder = redisTemplate.boundHashOps(ColorConstant.TOTAL_COLOR_HOLDER);
        Map<String,String> colorMap = colorHolder.entries();
        List<List<String>> res = new LinkedList<>(); // 链式存储，提升速度
        for (int i = 0; i < ColorConstant.BASE_NUMBER*ColorConstant.POSITION_NUMBER; i++) {
            List<String> tempList = new LinkedList<>();
            for (int j = 0; j < ColorConstant.BASE_NUMBER*ColorConstant.POSITION_NUMBER; j++) {
                String color =  colorMap.get(i+":"+j);
                tempList.add(StringUtils.hasLength(color)?color:"#ffffff");     // 默认白色画布
            }
            res.add(tempList);
        }
        return res;
    }

    /**
     * 获取临时库内 color
     */
    @Override
    public List<ColorEntity> getTempColor() {

        List<ColorEntity> res = new ArrayList<>();
        List<String> readyDel = new ArrayList<>();

        //  先去临时库 找到暂存的所有key
        Set<String> colorTempHolder = redisTemplate.boundSetOps(ColorConstant.TEMP_COLOR_HOLDER).members();
        Iterator<String> it = colorTempHolder.iterator();
        while (it.hasNext()) {
            String key = it.next();
            log.info("临时库中，key为{}有数据，",key);
            // 拿着这些去查
            if (redisTemplate.hasKey(key)){
                log.info("key为{}还没过期，",key);
                // 还没过期
                String color = (String) redisTemplate.boundValueOps(key).get();
                log.info("color:{}",color);
                if (StringUtils.hasLength(color)){
                    ColorEntity colorEntity = new ColorEntity(key, color);
                    log.info("colorEntity:{}",colorEntity.toString());
                    res.add(colorEntity);
                }else {
                    // 正在查询的时候过期了
                    readyDel.add(key);
                }
            }else {
                // 已经过期了
                log.info("key为{}已经过期，下一次不应该出现",key);
                readyDel.add(key);
            }
        }

        CompletableFuture.runAsync(() -> {
          readyDel.stream().forEach(item->{
              // 将过期的删除
              redisTemplate.boundSetOps(ColorConstant.TEMP_COLOR_HOLDER).remove(item);
          });
        },executor);

//        log.info("res:{}",res.get(0).toString());
        return res;
    }
}
