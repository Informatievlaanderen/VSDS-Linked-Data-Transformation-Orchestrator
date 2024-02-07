package be.vlaanderen.informatievlaanderen.ldes.ldi.discoverer.config;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "discoverer")
public class LdesDiscovererConfig {
	public static final String DEFAULT_SOURCE_FORMAT = "application/n-quads";
	public static final String DEFAULT_OUTPUT_FORMAT = "text/turtle";
	private String url;
	private String sourceFormat;
	private String outputFormat;

	public String getUrl() {
		if (url == null) {
			throw new IllegalArgumentException("Missing value for 'discoverer.url'");
		}
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSourceFormat() {
		return sourceFormat == null ? DEFAULT_SOURCE_FORMAT : sourceFormat;
	}

	public Lang getSourceFormatAsLang() {
		return RDFLanguages.nameToLang(getSourceFormat());
	}

	public void setSourceFormat(String sourceFormat) {
		this.sourceFormat = sourceFormat;
	}

	public String getOutputFormat() {
		return outputFormat == null ? DEFAULT_OUTPUT_FORMAT : outputFormat;
	}

	public Lang getOutputFormatAsLang() {
		return RDFLanguages.nameToLang(getOutputFormat());
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}
}
