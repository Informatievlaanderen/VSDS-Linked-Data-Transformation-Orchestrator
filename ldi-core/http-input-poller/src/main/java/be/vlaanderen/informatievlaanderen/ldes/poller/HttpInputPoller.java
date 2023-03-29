package be.vlaanderen.informatievlaanderen.ldes.poller;

import be.vlaanderen.informatievlaanderen.ldes.ldi.services.ComponentExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiAdapter;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiInput;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class HttpInputPoller extends LdiInput {
	private final WebClient client;
	public HttpInputPoller(ComponentExecutor executor, LdiAdapter adapter, String endpoint) {
		super(executor, adapter);
		client = WebClient.create(endpoint);
	}

	public Mono<String> poll() {
		return client.get().retrieve().bodyToMono(String.class);
	}

}
