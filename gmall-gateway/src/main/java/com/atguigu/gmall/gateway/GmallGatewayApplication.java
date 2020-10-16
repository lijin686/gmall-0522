package com.atguigu.gmall.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
//@RefreshScope 注解从nacos配置列表中读取配置文件
//@RefreshScope
public class GmallGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallGatewayApplication.class, args);
    }

}
