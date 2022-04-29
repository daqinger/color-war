package com.battle.colorwar;


import com.battle.colorwar.entity.MongoEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;

@Slf4j
@SpringBootTest
public class ColorWarApplicationTests {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void contextLoads() {


    }


//    @Test
//    public void test () throws IOException {
//
//        int rgb = image.getRGB(1, 1);
//        System.out.println(rgb);
//
//
//        String s=Integer.toHexString(rgb);
//        System.out.println(s.substring(2));
////        log.info("{}",s);
//    }
}
