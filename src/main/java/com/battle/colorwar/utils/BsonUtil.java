package com.battle.colorwar.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bson.BSONDecoder;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;

public class BsonUtil {


    private static String readBsonFile(File file) {
//        System.out.println(System.getProperty("user.dir"));//user.dir指定了当前的路径
//        File file = new File("E:\\WorkSpace\\Report\\src\\test\\Bjson\\binary.bson");
//        File file = new File("E:\\WorkSpace\\Report\\src\\test\\Bjson\\complex1.bson");
//        File file = new File("E:\\WorkSpace\\Report\\src\\test\\Bjson\\large.bson");
//        File file = new File("E:\\WorkSpace\\Report\\src\\test\\Bjson\\number3.bson");
        int count = 0;
        InputStream inputStream = null;
        BSONObject obj = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            inputStream = new BufferedInputStream(fileInputStream);
            BSONDecoder decoder = new BasicBSONDecoder();
            while (inputStream.available() > 0) {
                obj = decoder.readObject(inputStream);
                if(obj == null){
                    break;
                }
                System.out.println(obj);
                count++;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        System.err.println(String.format("%s objects read", count));
        return obj.toString();
    }
    public static void main(String[] args) {
        System.out.println(readBsonFile(new File("D:\\data\\mongo\\storage.bson")));
    }

}