package com.project.skillswap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 *  Configuración para procesamiento asíncrono
 * Permite que las transcripciones se procesen en segundo plano
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "transcriptionExecutor")
    public Executor transcriptionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("transcription-");
        executor.initialize();

        System.out.println("️ Executor de transcripción configurado");
        System.out.println("   Core pool size: 2");
        System.out.println("   Max pool size: 4");
        System.out.println("   Queue capacity: 10");

        return executor;
    }
}