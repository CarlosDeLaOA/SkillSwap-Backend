
package com.project.skillswap;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración general de la aplicación.
 * Define beans comunes utilizados en toda la aplicación.
 */
@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /**
     * Crea un bean de RestTemplate para realizar llamadas HTTP externas.
     *
     * @return Instancia de RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}