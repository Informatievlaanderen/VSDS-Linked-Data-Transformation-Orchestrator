package be.vlaanderen.informatievlaanderen.ldes.ldio.config;

import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.RequestExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.edc.services.MemoryTokenService;
import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.edc.services.MemoryTokenServiceLifecycle;
import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.edc.services.MemoryTransferService;
import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.edc.valueobjects.EdcUrlProxy;
import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.services.RequestExecutorFactory;
import be.vlaanderen.informatievlaanderen.ldes.ldi.services.ComponentExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiAdapter;
import be.vlaanderen.informatievlaanderen.ldes.ldio.LdioLdesClientConnectorApi;
import be.vlaanderen.informatievlaanderen.ldes.ldio.configurator.LdioInputConfigurator;
import be.vlaanderen.informatievlaanderen.ldes.ldio.event.LdesClientConnectorApiCreatedEvent;
import be.vlaanderen.informatievlaanderen.ldes.ldio.statusmanagement.pipelinestatus.InitPipelineStatus;
import be.vlaanderen.informatievlaanderen.ldes.ldio.statusmanagement.pipelinestatus.PipelineStatus;
import be.vlaanderen.informatievlaanderen.ldes.ldio.types.LdioInput;
import be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.ComponentProperties;
import io.micrometer.observation.ObservationRegistry;
import ldes.client.treenodesupplier.membersuppliers.MemberSupplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("java:S6830")
@Configuration
public class LdioLdesClientConnectorAutoConfig {

	public static final String NAME = "Ldio:LdesClientConnector";

	@Bean(NAME)
	public LdioInputConfigurator ldioConfigurator(ApplicationEventPublisher eventPublisher,
	                                              ObservationRegistry observationRegistry) {
		return new LdioClientConnectorConfigurator(eventPublisher, observationRegistry);
	}

	public static class LdioClientConnectorConfigurator implements LdioInputConfigurator {

		public static final String CONNECTOR_TRANSFER_URL = "connector-transfer-url";
		public static final String PROXY_URL_TO_REPLACE = "proxy-url-to-replace";
		public static final String PROXY_URL_REPLACEMENT = "proxy-url-replacement";
		private final ApplicationEventPublisher eventPublisher;
		private final ObservationRegistry observationRegistry;
		private final RequestExecutorFactory requestExecutorFactory = new RequestExecutorFactory(false);
		private final RequestExecutor baseRequestExecutor = requestExecutorFactory.createNoAuthExecutor();

		public LdioClientConnectorConfigurator(ApplicationEventPublisher eventPublisher, ObservationRegistry observationRegistry) {
			this.eventPublisher = eventPublisher;
			this.observationRegistry = observationRegistry;
		}

		@Override
		public LdioInput configure(LdiAdapter adapter, ComponentExecutor executor, ComponentProperties properties) {
			final String pipelineName = properties.getPipelineName();
			final var connectorTransferUrl = properties.getProperty(CONNECTOR_TRANSFER_URL);
			final var transferService = new MemoryTransferService(baseRequestExecutor, connectorTransferUrl);
			final var memoryTokenServiceLifecycle = new MemoryTokenServiceLifecycle();
			final var tokenService = new MemoryTokenService(transferService, memoryTokenServiceLifecycle);

			final var urlProxy = getEdcUrlProxy(properties);
			final var edcRequestExecutor = requestExecutorFactory.createEdcExecutor(baseRequestExecutor, tokenService,
					urlProxy);
			final MemberSupplier memberSupplier = new MemberSupplierFactory(properties, edcRequestExecutor).getMemberSupplier();
			final var ldesClientConnector = new LdioLdesClientConnectorApi(executor, pipelineName, observationRegistry, memberSupplier, eventPublisher, transferService, tokenService);
			eventPublisher.publishEvent(new LdesClientConnectorApiCreatedEvent(pipelineName, ldesClientConnector));
			return ldesClientConnector;
		}

		@Override
		public boolean isAdapterRequired() {
			return false;
		}

		@Override
		public PipelineStatus getInitialPipelineStatus() {
			return new InitPipelineStatus();
		}

		private static EdcUrlProxy getEdcUrlProxy(ComponentProperties properties) {
			final var proxyUrlToReplace = properties.getOptionalProperty(PROXY_URL_TO_REPLACE).orElse("");
			final var proxyUrlReplacement = properties.getOptionalProperty(PROXY_URL_REPLACEMENT).orElse("");
			return new EdcUrlProxy(proxyUrlToReplace, proxyUrlReplacement);
		}
	}
}
