package com.example.providera.config;

import io.micrometer.context.ContextSnapshot;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-trace-");
        // Set context propagating task decorator to avoid trace loss in async threads
        executor.setTaskDecorator(new ContextCopyingDecorator());
        executor.initialize();
        return executor;
    }

    public static class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture current context (including Brave/Micrometer tracing and MDC)
            ContextSnapshot snapshot = ContextSnapshot.captureAll();
            return () -> {
                // Write captured thread locals into the async thread environment
                try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                    runnable.run();
                }
            };
        }
    }
}
