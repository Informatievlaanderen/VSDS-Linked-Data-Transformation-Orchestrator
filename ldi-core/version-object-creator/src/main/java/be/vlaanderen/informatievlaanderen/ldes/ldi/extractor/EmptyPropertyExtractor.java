package be.vlaanderen.informatievlaanderen.ldes.ldi.extractor;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.List;

public class EmptyPropertyExtractor implements PropertyExtractor {

	// TODO TVB: 08/09/23 add test
	@Override
	public List<RDFNode> getProperties(Model model) {
		return new ArrayList<>();
	}

}
