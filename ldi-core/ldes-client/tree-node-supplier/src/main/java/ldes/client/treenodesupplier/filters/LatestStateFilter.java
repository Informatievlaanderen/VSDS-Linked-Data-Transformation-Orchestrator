package ldes.client.treenodesupplier.filters;

import be.vlaanderen.informatievlaanderen.ldes.ldi.extractor.PropertyPathExtractor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.timestampextractor.TimestampExtractor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.timestampextractor.TimestampFromPathExtractor;
import ldes.client.treenodesupplier.domain.entities.MemberVersionRecord;
import ldes.client.treenodesupplier.domain.valueobject.SuppliedMember;
import ldes.client.treenodesupplier.repository.MemberVersionRepository;

import java.time.LocalDateTime;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

public class LatestStateFilter implements MemberFilter {

	private final MemberVersionRepository memberVersionRepository;
	private final boolean keepState;
	private final TimestampExtractor timestampExtractor;
	private final PropertyPathExtractor versionOfExtractor;


	public LatestStateFilter(MemberVersionRepository memberVersionRepository, boolean keepState, String timestampPath, String versionOfPath) {
		this.memberVersionRepository = memberVersionRepository;
		this.keepState = keepState;
		this.timestampExtractor = new TimestampFromPathExtractor(createProperty(timestampPath));
		this.versionOfExtractor = PropertyPathExtractor.from(versionOfPath);
	}

	@Override
	public boolean isAllowed(SuppliedMember member) {
		final String versionOf = versionOfExtractor.getProperties(member.getModel()).stream()
				.findFirst()
				.map(node -> node.asResource().getURI())
				.orElseThrow(() -> new IllegalStateException("Could not find versionOf in supplied member"));
		final LocalDateTime timestamp = timestampExtractor.extractTimestamp(member.getModel());
		return memberVersionRepository.isVersionAfterTimestamp(new MemberVersionRecord(versionOf, timestamp));
	}

	@Override
	public void saveAllowedMember(SuppliedMember member) {
		final String versionOf = versionOfExtractor.getProperties(member.getModel()).stream()
				.findFirst()
				.map(node -> node.asResource().getURI())
				.orElseThrow(() -> new IllegalStateException("Could not find versionOf in supplied member"));
		final LocalDateTime timestamp = timestampExtractor.extractTimestamp(member.getModel());
		memberVersionRepository.addMemberVersion(new MemberVersionRecord(versionOf, timestamp));
	}

	@Override
	public void destroyState() {
		if(!keepState) {
			memberVersionRepository.destroyState();
		}
	}
}