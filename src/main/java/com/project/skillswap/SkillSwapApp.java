package com.project.skillswap; // <-- OJO: paquete raíz del proyecto

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.project.skillswap")
public class SkillSwapApp {
    public static void main(String[] args) {
        SpringApplication.run(SkillSwapApp.class, args);
    }
}
