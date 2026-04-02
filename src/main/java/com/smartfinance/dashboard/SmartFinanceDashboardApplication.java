package com.smartfinance.dashboard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartfinance.dashboard.module")
public class SmartFinanceDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFinanceDashboardApplication.class, args);
    }
}
