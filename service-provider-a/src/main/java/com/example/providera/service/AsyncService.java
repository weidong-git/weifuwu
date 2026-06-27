package com.example.providera.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

    @Async
    public void executeAsyncTask() {
        log.info("Asynchronous task running in custom thread pool. TraceId should be propagated here!");
    }
}
