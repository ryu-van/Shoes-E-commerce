package com.example.shoozy_shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
public class EventQueue {
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>(2000);
    public boolean enqueue(Object event){
        boolean ok = queue.offer(event);
        if(!ok){
            log.error("Event queue is full, dropping event: {}", event);
        }
        return ok;
    }
    public Object take() throws InterruptedException {
        return queue.take();
    }
}
