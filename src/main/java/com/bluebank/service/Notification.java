package com.bluebank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class Notification {

    private final Logger logger = LoggerFactory.getLogger(Notification.class);

    @KafkaListener(
            topics = "transfer-notification",
            groupId = "group-id")
    public void consume(String message){
        logger.info(String.format("Message receiver \n %s", message));
    }
}
