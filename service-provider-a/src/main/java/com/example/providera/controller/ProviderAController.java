package com.example.providera.controller;

import com.example.providera.feign.ServiceBClient;
import com.example.providera.service.AsyncService;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ProviderAController {

    private static final Logger log = LoggerFactory.getLogger(ProviderAController.class);

    private final ServiceBClient serviceBClient;
    private final AsyncService asyncService;
    private final Tracer tracer;

    public ProviderAController(ServiceBClient serviceBClient, AsyncService asyncService, Tracer tracer) {
        this.serviceBClient = serviceBClient;
        this.asyncService = asyncService;
        this.tracer = tracer;
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        log.info("Received request in service-provider-a.");

        // Trigger async execution to verify trace propagation in thread pool
        asyncService.executeAsyncTask();

        log.info("Calling service-provider-b via OpenFeign...");
        Map<String, Object> bResponse = serviceBClient.testB();
        log.info("Response from service-provider-b: {}", bResponse);

        Map<String, Object> response = new HashMap<>();
        response.put("service-a", "success");
        response.put("traceId", tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "no-trace");
        response.put("service-b-data", bResponse);
        return response;
    }
}
