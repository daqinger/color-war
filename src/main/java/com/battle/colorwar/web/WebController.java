package com.battle.colorwar.web;

import com.alibaba.fastjson.JSONArray;
import com.battle.colorwar.constant.ColorConstant;
import com.battle.colorwar.entity.ColorEntity;
import com.battle.colorwar.service.ColorService;
import com.battle.colorwar.service.RabbitService;
import com.battle.colorwar.utils.R;
import com.battle.colorwar.vo.CheatVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("battle/color")
public class WebController {

    @Autowired
    ColorService colorService;

    @Autowired
    RabbitService rabbitService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 保存数据
     */
    @PostMapping("/savedata")
    public R saveData(){


        try{
            String mainColorCache = stringRedisTemplate.opsForValue().get(ColorConstant.MAIN_COLOR_CACHE);
//            String data = " This content will append to the end of the file";

            File file =new File("d://data//mydata.json");

            //if file doesnt exists, then create it
            if(!file.exists()){
                file.createNewFile();
            }

            //true = append file
            FileWriter fileWritter = new FileWriter(file.getName(),true);
            fileWritter.write(mainColorCache);
            fileWritter.close();

            System.out.println("Done");

        }catch(IOException e){
            e.printStackTrace();
        }


        return R.ok();
    }


    /**
     * 创建交换机
     */
    @PostMapping("/cheat")
    public R cheat(@RequestBody CheatVo cheatVo){
        boolean flag = false;
        try {
            flag = colorService.cheat(cheatVo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag ? R.ok() : R.error();
    }


    /**
     * 加载首页获取的数据
     */
    @GetMapping("/getcolorforhome")
    public R getColorForHome(){
        return R.ok().setData(colorService.getMainColorCache());
    }

    /**
     * 加载分区获取的数据
     */
    @GetMapping("/getcolorforbattlefield/{baseX}/{baseY}")
    public R getColorForBattleField(@PathVariable("baseX") Integer baseX, @PathVariable("baseY") Integer baseY){
        return R.ok().setData(colorService.getSubColorCache(baseX,baseY));
    }

    /**
     * 创建交换机
     */
    @PostMapping("/createexchaneg")
    public R createExchange(){
        boolean flag = rabbitService.createExchange();
        return flag ? R.ok() : R.error();
    }


    /**
     * 将一个请求的颜色数据 存入 总库中 和 临时库中
     */
    @PostMapping("/save")
    public R saveColor(@RequestBody ColorEntity colorEntity){
        try {
            colorService.saveColor(colorEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok();
    }


    /**
     * 获取目前临时库内所有的数据  5s 秒一次
     */
    @GetMapping("/gettempcolor")
    public R getTempColor(){
        return R.ok().setData(colorService.getTempColor());
    }

    /**
     *  获取总库中所有的数据  页面首次加载获取
     */
    @GetMapping("/getallcolor")
    public R getAllColor(){
        return R.ok().setData(colorService.getAllColor());
    }


}
