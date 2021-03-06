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
        log.info("???:{},???{}",image.getWidth(),image.getHeight());
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
     * ??????????????????---????????????????????????????????????????????????
     */
    @Override
    public List<List<String>>  getMainColorCache() {
        String mainColorCache = stringRedisTemplate.opsForValue().get(ColorConstant.MAIN_COLOR_CACHE);
        List<List<String>> result = JSON.parseObject(mainColorCache, new TypeReference<List<List<String>>>() {});
        return result;
    }

    /**
     * ?????? (baseX,baseY) ??????????????????????????????????????????
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
     * ???????????????map????????????k-v
     */
    @Override
    public Map<String, String> getTotalColorHolder() {
        return redisTemplate.boundHashOps(ColorConstant.TOTAL_COLOR_HOLDER).entries();
    }

    /**
     * ?????? (baseX,baseY,positionX,positionY,colorValue) ?????????????????????????????????????????????
     *
     * @param colorEntity
     */
    @Override
    public boolean saveColor(ColorEntity colorEntity) throws ExecutionException, InterruptedException {

        //????????????redis??? https://blog.csdn.net/lydms/article/details/105224210
        log.info("?????????color??????{}",colorEntity.toString());

        // ???????????? ????????????
        CompletableFuture<Void> saveColorToMainDB = CompletableFuture.runAsync(() -> {
            // ?????? total:color:holder
//            BoundHashOperations colorHolder = redisTemplate.boundHashOps(ColorConstant.TOTAL_COLOR_HOLDER);
//
//            //?????????????????????
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


            log.info("???????????????key???({}),??????{}",colorEntity.getTrueKey(),colorEntity.getColorValue());
        },executor);

        //??????????????????key???????????????,?????????????????????
        CompletableFuture<Void> saveColorToTempDB = CompletableFuture.runAsync(() -> {
            // ??????key?????????????????????????????????key?????????value ,??????????????????????????????
            redisTemplate.opsForSet().add(ColorConstant.TEMP_COLOR_HOLDER, colorEntity.getTempKey());
            log.info("???????????????????????????key???{}",colorEntity.getTempKey());
        },executor);

        // ??????????????????
        CompletableFuture<Void> saveTempColor = CompletableFuture.runAsync(() -> {
            // ??????  tempColor
            redisTemplate.boundValueOps(colorEntity.getTempKey()).set(colorEntity.getColorValue(),40, TimeUnit.SECONDS);
            log.info("??????????????????????????????key???{},??????{}",colorEntity.getTempKey(),colorEntity.getColorValue());
        },executor);
        // ??????????????????
        CompletableFuture.allOf(saveColorToMainDB, saveColorToTempDB,saveTempColor).get();
        log.info("??????????????????..");
        return true;
    }

    /**
     * ????????????color
     * ??????????????????????????????  1 ?????????????????? main:color:cache ?????????
     */
    @Override
    public List<List<String>> getAllColor() {
        // ?????????????????????????????????
        BoundHashOperations colorHolder = redisTemplate.boundHashOps(ColorConstant.TOTAL_COLOR_HOLDER);
        Map<String,String> colorMap = colorHolder.entries();
        List<List<String>> res = new LinkedList<>(); // ???????????????????????????
        for (int i = 0; i < ColorConstant.BASE_NUMBER*ColorConstant.POSITION_NUMBER; i++) {
            List<String> tempList = new LinkedList<>();
            for (int j = 0; j < ColorConstant.BASE_NUMBER*ColorConstant.POSITION_NUMBER; j++) {
                String color =  colorMap.get(i+":"+j);
                tempList.add(StringUtils.hasLength(color)?color:"#ffffff");     // ??????????????????
            }
            res.add(tempList);
        }
        return res;
    }

    /**
     * ?????????????????? color
     */
    @Override
    public List<ColorEntity> getTempColor() {

        List<ColorEntity> res = new ArrayList<>();
        List<String> readyDel = new ArrayList<>();

        //  ??????????????? ?????????????????????key
        Set<String> colorTempHolder = redisTemplate.boundSetOps(ColorConstant.TEMP_COLOR_HOLDER).members();
        Iterator<String> it = colorTempHolder.iterator();
        while (it.hasNext()) {
            String key = it.next();
            log.info("???????????????key???{}????????????",key);
            // ??????????????????
            if (redisTemplate.hasKey(key)){
                log.info("key???{}???????????????",key);
                // ????????????
                String color = (String) redisTemplate.boundValueOps(key).get();
                log.info("color:{}",color);
                if (StringUtils.hasLength(color)){
                    ColorEntity colorEntity = new ColorEntity(key, color);
                    log.info("colorEntity:{}",colorEntity.toString());
                    res.add(colorEntity);
                }else {
                    // ??????????????????????????????
                    readyDel.add(key);
                }
            }else {
                // ???????????????
                log.info("key???{}???????????????????????????????????????",key);
                readyDel.add(key);
            }
        }

        CompletableFuture.runAsync(() -> {
          readyDel.stream().forEach(item->{
              // ??????????????????
              redisTemplate.boundSetOps(ColorConstant.TEMP_COLOR_HOLDER).remove(item);
          });
        },executor);

//        log.info("res:{}",res.get(0).toString());
        return res;
    }
}
