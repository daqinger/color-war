package com.battle.colorwar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Title: ThreadPoolConfigProperties</p>
 */
@ConfigurationProperties(prefix = "colorwar.thread")
@Data
public class ThreadPoolConfigProperties {

    private Integer coreSize;

    private Integer maxSize;

    private Integer keepAliveTime;
}
