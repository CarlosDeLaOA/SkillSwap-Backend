package com.project.skillswap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkillSwapApp {

    public static void main(String[] args) {
        SpringApplication.run(SkillSwapApp.class, args);
    }

}
