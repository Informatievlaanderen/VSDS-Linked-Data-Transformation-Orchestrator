package be.vlaanderen.informatievlaanderen.ldes.ldio;

import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiInput;
import be.vlaanderen.informatievlaanderen.ldes.ldio.events.PipelineStatusEvent;
import be.vlaanderen.informatievlaanderen.ldes.ldio.modules.DummyIn;
import be.vlaanderen.informatievlaanderen.ldes.ldio.modules.MockVault;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.IntStream;

import static be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.PipelineStatus.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RecordApplicationEvents
class PipelineTest {
	@Autowired
	private ApplicationEvents applicationEvents;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	LdiInput ldiInput;
	@Autowired
	MockVault mockVault;

	@Test
	void verifyBasicPipelineFlow() {
		DummyIn dummyIn = (DummyIn) ldiInput;
		dummyIn.sendData();

		Model expected = RDFParserBuilder.create()
				.fromString(
						"""
								_:Beced3445712fba4a1f51b6fae28e0147 <http://schema.org/description> "Transformed" .
								_:Beced3445712fba4a1f51b6fae28e0147 <http://schema.org/integer> "0"^^<http://www.w3.org/2001/XMLSchema#integer> .""")
				.lang(Lang.NQUADS)
				.toModel();

		await().until(() -> mockVault.getReceivedObjects().size() == 1);
		assertTrue(expected.isIsomorphicWith(mockVault.getReceivedObjects().get(0)));
	}

	@Test
	void verifyHaltedPipelineFlow() {
		DummyIn dummyIn = (DummyIn) ldiInput;
		// Initial Run
		IntStream.range(0, 10)
				.forEach(value -> dummyIn.sendData());
		await().until(() -> mockVault.getReceivedObjects().size() == 10);

		// Halt Pipeline
		applicationEventPublisher.publishEvent(new PipelineStatusEvent(HALTED));

		IntStream.range(0, 10)
				.forEach(value -> dummyIn.sendData());
		Assertions.assertEquals(10, mockVault.getReceivedObjects().size());

		// Resume Pipeline
		applicationEventPublisher.publishEvent(new PipelineStatusEvent(RESUMING));

		// Whilst resuming add more members to pipeline
		IntStream.range(0, 10)
				.forEach(value -> dummyIn.sendData());

		await().until(() -> applicationEvents.stream(PipelineStatusEvent.class)
				.filter(event -> event.getStatus() == RUNNING)
				.count() == 1);
		Assertions.assertEquals(30, mockVault.getReceivedObjects().size());
	}

}
