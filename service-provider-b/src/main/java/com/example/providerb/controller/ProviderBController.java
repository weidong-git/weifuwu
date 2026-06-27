package com.example.providerb.controller;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ProviderBController {

    private static final Logger log = LoggerFactory.getLogger(ProviderBController.class);

    private final Tracer tracer;

    public ProviderBController(Tracer tracer) {
        this.tracer = tracer;
    }

    @GetMapping("/test-b")
    public Map<String, Object> testB() {
        log.info("Received request in service-provider-b.");

        String traceId = "no-trace";
        String spanId = "no-span";

        if (tracer != null && tracer.currentSpan() != null) {
            traceId = tracer.currentSpan().context().traceId();
            spanId = tracer.currentSpan().context().spanId();
        }

        log.info("Current TraceId: {}, SpanId: {}", traceId, spanId);

        Map<String, Object> response = new HashMap<>();
        response.put("service-b", "success");
        response.put("traceId", traceId);
        response.put("spanId", spanId);
        return response;
    }
}
