
package com.project.skillswap;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SkillSwapApp {
    private static final Logger logger = LoggerFactory.getLogger(SkillSwapApp.class);

    public static void main(String[] args) {
        SpringApplication.run(SkillSwapApp.class, args);
    }

}
