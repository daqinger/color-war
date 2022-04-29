package com.battle.colorwar.entity;

import com.battle.colorwar.constant.ColorConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.omg.PortableInterceptor.INACTIVE;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColorEntity {
    private Integer baseX;  //基座x坐标
    private Integer baseY;  //基座y坐标
    private Integer positionX;   // 相对位置x
    private Integer positionY;   // 相对位置y
    private Integer trueX;  //真实x
    private Integer trueY;  //真实y

    private String colorValue;  // 颜色的值


    /**
     * 前端传过来肯定是  baseX,baseY,positionX,positionY
     *
     * 需要获取真实坐标组成的key
     * @return
     */
    public String getTrueKey(){
        return getTrueX()+":"+getTrueY();
    }

    public Integer getTrueX() {
        return this.baseX*ColorConstant.POSITION_NUMBER +this.positionX;
    }

    public Integer getTrueY() {
        return this.baseY*ColorConstant.POSITION_NUMBER +this.positionY;
    }

    /**
     * 存入临时库的key，为了推送和反编方便，使用baseX,baseY,positionX,positionY方式进行存储
     * @return
     */
    public String getTempKey(){
        return this.baseX+":"+this.baseY+":"+this.positionX+":"+this.positionY;
    }

    public ColorEntity(Integer positionX, Integer positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public ColorEntity(String key, String colorValue){

        if (StringUtils.hasLength(key)){
            String [] parts = key.split(":");
            this.baseX = Integer.parseInt(parts[0]);
            this.baseY = Integer.parseInt(parts[1]);
            this.positionX = Integer.parseInt(parts[2]);
            this.positionY = Integer.parseInt(parts[3]);
        }
        this.colorValue = colorValue;


    }
}
