package be.vlaanderen.informatievlaanderen.ldes.ldio.services;

import be.vlaanderen.informatievlaanderen.ldes.ldi.services.ComponentExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldio.types.LdioProcessor;
import org.apache.jena.rdf.model.Model;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComponentExecutorImpl implements ComponentExecutor {

	private final ExecutorService executorService;
	private final LdioProcessor ldiTransformerPipeline;

	public ComponentExecutorImpl(LdioProcessor ldiTransformerPipeline) {
		this.executorService = Executors.newSingleThreadExecutor();
		this.ldiTransformerPipeline = ldiTransformerPipeline;
	}

	@Override
	public void transformLinkedData(final Model linkedDataModel) {
		executorService.execute(() -> ldiTransformerPipeline.apply(linkedDataModel));
	}
}
