package com.internpilot.service.impl;

import com.internpilot.service.AnalysisProgressPublisher;
import com.internpilot.vo.analysis.AnalysisProgressMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisProgressPublisherImpl implements AnalysisProgressPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(
            String taskNo,
            String status,
            Integer progress,
            String message,
            Long reportId,
            String errorMessage
    ) {
        AnalysisProgressMessage progressMessage = AnalysisProgressMessage.of(
                taskNo,
                status,
                progress,
                message,
                reportId,
                errorMessage
        );

        messagingTemplate.convertAndSend(
                "/topic/analysis/" + taskNo,
                progressMessage
        );
    }
}