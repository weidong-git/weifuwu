package com.example.providera.feign;

import com.example.providera.config.TraceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "service-provider-b", configuration = TraceFeignConfig.class)
public interface ServiceBClient {

    @GetMapping("/test-b")
    Map<String, Object> testB();
}
