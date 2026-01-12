package com.huixing.fontal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author qimu
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.huixing.fontal.mapper")
public class fontalBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(fontalBackendApplication.class, args);
    }
}
