package ldes.client.treenodesupplier.filters;

import be.vlaanderen.informatievlaanderen.ldes.ldi.timestampextractor.TimestampExtractor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.timestampextractor.TimestampFromPathExtractor;
import ldes.client.treenodesupplier.domain.entities.MemberVersionRecord;
import ldes.client.treenodesupplier.domain.valueobject.SuppliedMember;
import ldes.client.treenodesupplier.repository.MemberVersionRepository;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;

import java.time.LocalDateTime;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

public class LatestStateFilter implements MemberFilter {

	private final MemberVersionRepository memberVersionRepository;
	private final boolean keepState;
	private final TimestampExtractor timestampExtractor;
	private final String versionOfPath;


	public LatestStateFilter(MemberVersionRepository memberVersionRepository, boolean keepState, String timestampPath, String versionOfPath) {
		this.memberVersionRepository = memberVersionRepository;
		this.keepState = keepState;
		this.timestampExtractor = new TimestampFromPathExtractor(createProperty(timestampPath));
		this.versionOfPath = versionOfPath;
	}

	@Override
	public boolean isAllowed(SuppliedMember member) {
		final String versionOf = extractVersionOf(member);
		final LocalDateTime timestamp = extractTimestampWithSubject(member);
		return memberVersionRepository.isVersionAfterTimestamp(new MemberVersionRecord(versionOf, timestamp));
	}

	@Override
	public void saveAllowedMember(SuppliedMember member) {
		final String versionOf = extractVersionOf(member);
		final LocalDateTime timestamp = extractTimestampWithSubject(member);
		memberVersionRepository.addMemberVersion(new MemberVersionRecord(versionOf, timestamp));
	}

	@Override
	public void destroyState() {
		if(!keepState) {
			memberVersionRepository.destroyState();
		}
	}

	private LocalDateTime extractTimestampWithSubject(SuppliedMember member) {
		return timestampExtractor.extractTimestampWithSubject(
				ResourceFactory.createProperty(member.getId()),
				member.getModel());
	}

	private String extractVersionOf(SuppliedMember member) {
		return member.getModel()
				.listObjectsOfProperty(ResourceFactory.createProperty(member.getId()), ResourceFactory.createProperty(versionOfPath))
				.filterKeep(RDFNode::isResource)
				.mapWith(rdfNode -> rdfNode.asResource().getURI())
				.nextOptional()
				.orElseThrow(() -> new IllegalStateException("Could not find versionOf in supplied member"));
	}
}
