package com.battle.colorwar.vo;

import lombok.Data;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;

@Data
public class CheatVo {
    private String picUrl;  //图片地址      "D://mine/color-war/test3.jpg"
    private Integer size;   //图片的最大边大小占多少像素
    private Integer beginX; // 初始x
    private Integer beginY; // 初始y
}
