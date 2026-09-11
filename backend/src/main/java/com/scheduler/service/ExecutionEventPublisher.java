package com.scheduler.service;

import com.scheduler.dto.ExecutionResponse;
import com.scheduler.entity.Execution;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Pushes execution state changes to any subscribed dashboard over STOMP/WebSocket. */
@Component
public class ExecutionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public ExecutionEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(Execution execution) {
        messagingTemplate.convertAndSend("/topic/executions", ExecutionResponse.from(execution));
    }
}
