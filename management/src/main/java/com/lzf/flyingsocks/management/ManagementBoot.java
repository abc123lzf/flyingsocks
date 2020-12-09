package com.lzf.flyingsocks.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableTransactionManagement
@SpringBootApplication
public class ManagementBoot {

    public static void main(String[] args) {
        SpringApplication.run(ManagementBoot.class, args);
    }

}
