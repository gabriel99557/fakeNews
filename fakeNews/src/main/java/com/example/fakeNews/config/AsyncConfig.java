package com.example.fakeNews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Habilita o processamento assíncrono do backend (RNF02): a persistência do
 * histórico roda em uma thread separada, sem travar a resposta síncrona
 * devolvida ao front-end (ver Fig. 2, "Sincronismo Paralelo Fork/Join").
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("fakenews-async-");
        executor.initialize();
        return executor;
    }
}
