package com.project.skillswap.config;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Bean(name = "transcriptionExecutor")
    public Executor transcriptionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("transcription-");
        executor.initialize();

        logger.info("️ Executor de transcripción configurado");
        logger.info("   Core pool size: 2");
        logger.info("   Max pool size: 4");
        logger.info("   Queue capacity: 10");

        return executor;
    }
}