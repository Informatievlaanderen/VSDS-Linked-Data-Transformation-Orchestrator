package be.vlaanderen.informatievlaanderen.ldes.ldto.transformer;

import be.vlaanderen.informatievlaanderen.ldes.ldto.types.LdtoTransformer;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static be.vlaanderen.informatievlaanderen.ldes.ldto.LdtoConstants.*;

@Component
public class SparqlConstructTransformer implements LdtoTransformer {
	private final Query query;
	private final boolean inferMode;

	public SparqlConstructTransformer(Map<String, String> config) {
		this.query = QueryFactory.create(Objects.requireNonNull(config.get(QUERY), QUERY_VALIDATION_MSG));
		this.inferMode = Boolean.parseBoolean(config.getOrDefault(INFER, String.valueOf(false)));
	}

	@Override
	public Model execute(Model linkedDataModel) {
		try (QueryExecution qexec = QueryExecutionFactory.create(query, linkedDataModel)) {
			Model resultModel = qexec.execConstruct();
			if (inferMode) {
				resultModel.add(linkedDataModel);
			}
			return resultModel;
		}
	}
}
