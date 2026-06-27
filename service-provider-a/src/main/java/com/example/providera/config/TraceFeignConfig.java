package com.example.providera.config;

import io.micrometer.tracing.Tracer;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceFeignConfig {

    private final Tracer tracer;

    public TraceFeignConfig(Tracer tracer) {
        this.tracer = tracer;
    }

    @Bean
    public RequestInterceptor feignTraceInterceptor() {
        return template -> {
            if (tracer != null && tracer.currentSpan() != null) {
                var context = tracer.currentSpan().context();
                // Propagate standard B3 headers for Brave/Zipkin compatibility
                template.header("X-B3-TraceId", context.traceId());
                template.header("X-B3-SpanId", context.spanId());
                if (context.parentId() != null) {
                    template.header("X-B3-ParentSpanId", context.parentId());
                }
                if (context.sampled() != null) {
                    template.header("X-B3-Sampled", context.sampled() ? "1" : "0");
                }
                // Also propagate standard W3C headers (traceparent)
                template.header("traceparent", String.format("00-%s-%s-%s",
                        context.traceId(),
                        context.spanId(),
                        (context.sampled() != null && context.sampled()) ? "01" : "00"));
            }
        };
    }
}
