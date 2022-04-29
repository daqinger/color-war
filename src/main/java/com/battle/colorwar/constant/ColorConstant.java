package com.battle.colorwar.constant;

public class ColorConstant {
    // 1000*1000 临时数据 key  总库
    public static final String MAIN_COLOR_CACHE = "main:color:cache";

    // 625 个子临时数据key前缀   分库
    public static final String SUB_COLOR_CACHE_PRE = "sub:color:cache:";

    // 最大的缓存 ，缓存所有颜色的map
    public static final String TOTAL_COLOR_HOLDER = "total:color:holder";

    // mongodb 持久化 集合名称
    public static final String MONGODB_COLOR_HOLDER = "total_color_holder";


    // 所有的临时数据缓存地点
    public static final String TEMP_COLOR_HOLDER = "temp:color:holder";

    // 625 个交换机前缀
    public static final String COLOR_EXCHANGE_PRE = "color.exchange.";

    // 目前有多少个基准
    public static final Integer BASE_NUMBER = 10;

    // 目前每个基座有多少position  暂定40
    public static final Integer POSITION_NUMBER = 40;


}
