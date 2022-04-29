package com.battle.colorwar.service;

import com.battle.colorwar.entity.ColorEntity;
import com.battle.colorwar.vo.CheatVo;
import redis.clients.jedis.BinaryClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface ColorService {
    /**
     * 作弊器
     */
    boolean cheat(CheatVo cheatVo) throws IOException;

    /**
     * 获取首页数据---直接从缓存中取出，不用组装的那种
     */
    List<List<String>> getMainColorCache();

    /**
     * 通过 (baseX,baseY) 获取分区颜色，不用组装的那种
     */
    List<List<String>> getSubColorCache(Integer baseX,Integer baseY);


    /**
     * 查询最大的map内的所有k-v
     */
    Map<String,String> getTotalColorHolder();


    /**
     * 存入  color  值 到redis  和  temp redis中
     */
    boolean saveColor(ColorEntity colorEntity) throws ExecutionException, InterruptedException;

    /**
     * 获取所有color
     */
    List<List<String>> getAllColor();

    /**
     * 获取临时库内 color
     */
    List<ColorEntity> getTempColor();

}
