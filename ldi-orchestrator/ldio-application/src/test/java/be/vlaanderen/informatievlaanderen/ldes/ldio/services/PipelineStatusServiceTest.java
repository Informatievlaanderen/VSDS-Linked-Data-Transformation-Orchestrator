package be.vlaanderen.informatievlaanderen.ldes.ldio.services;

import be.vlaanderen.informatievlaanderen.ldes.ldio.pipeline.components.LdioInput;
import be.vlaanderen.informatievlaanderen.ldes.ldio.pipeline.creation.InputCreatedEvent;
import be.vlaanderen.informatievlaanderen.ldes.ldio.pipeline.creation.PipelineDeletedEvent;
import be.vlaanderen.informatievlaanderen.ldes.ldio.status.PipelineStatus;
import be.vlaanderen.informatievlaanderen.ldes.ldio.status.PipelineStatusTrigger;
import be.vlaanderen.informatievlaanderen.ldes.ldio.status.services.PipelineStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PipelineStatusServiceTest {
    private final String pipelineName = "pipeline";
    private final LdioInput input = mock(LdioInput.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private PipelineStatusServiceImpl pipelineStatusService;

    @BeforeEach
    void setup() {
        pipelineStatusService = new PipelineStatusServiceImpl(eventPublisher);
        pipelineStatusService.handlePipelineCreated(new InputCreatedEvent(pipelineName, input));
    }

    @Test
    void when_StoppingPipeline_Then_MethodsAreCalled() {
        when(input.updateStatus(any())).thenReturn(PipelineStatus.STOPPED);
        PipelineStatus result = pipelineStatusService.stopPipeline(pipelineName);

        assertEquals(PipelineStatus.STOPPED, result);
        verify(input).updateStatus(PipelineStatusTrigger.STOP);
        verify(eventPublisher).publishEvent(new PipelineDeletedEvent(pipelineName));
    }

}
