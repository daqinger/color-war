# color-war
> 介绍

在2017年4月1日愚人节,社交网站Reddit发布了一个全球性社会实验:每个用户每隔5分钟可以在一个1000x1000像素的画布上绘制一个色块... [相关视频连接](https://www.bilibili.com/video/av71160863?from=search&seid=14889639180811676751&spm_id_from=333.337.0.0)

基于上面的创意，便有了此项目。

前端采用 VUE+ [Element UI](https://element.eleme.cn/#/zh-CN)

> 简陋的操作说明

1. 玩法：每人每次只能填一个格子（因为是想复现原来网站的创意，而不是一个画画网站）

2. 目前的设定：主页和分区更新时间是1分钟 ，一个分区数据推送时间是30s，所以您画的格子刷新后在主页看不到，不要着急，那也许是还没更新

3. 注意事项：只有一点，，提交数据的契机只有打开选色板，点击确认后才会提交！！！！点击确认后才会提交！！！！点击确认后才会提交！！！！

> 构建方法

1.准备好 redis，mysql，rabbitmq，mongodb，nacos

2.我用了一个其他项目的网关，自己写也是没有问题，记得配置跨域

```
@Configuration
public class MyCorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //1.配置跨域
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.setAllowCredentials(true);

        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }
}
```
网关的配置如下：

```
spring:
  cloud:
    gateway:
      routes:
        - id: colorwar_route
          uri: lb://color-war  #lb,指的是负载均衡
          predicates:
            - Path=/api/battle/**
          filters:
            - RewritePath=/api/(?<segment>.*),/$\{segment}
```

3.初始化 xxl-job ，添加 sendTempColorToRabbit，updateColorCache 两个任务。

4.初始化 rabbitmq ，[开通Stomp通道](https://blog.csdn.net/weixin_40461281/article/details/81806921)

5.通过请求地址，创建交换机（具体见controller层）