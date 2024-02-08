package be.vlaanderen.informatievlaanderen.ldes.ldio.config;

import be.vlaanderen.informatievlaanderen.ldes.ldi.RmlAdapter;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiAdapter;
import be.vlaanderen.informatievlaanderen.ldes.ldio.configurator.LdioAdapterConfigurator;
import be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.ComponentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LdioRmlAdapterAutoConfig {

	@SuppressWarnings("java:S6830")
	@Bean("Ldio:RmlAdapter")
	public LdioAdapterConfigurator ldiHttpOutConfigurator() {
		return new LdioRmlAdapterProcessorConfigurator();
	}

	public static class LdioRmlAdapterProcessorConfigurator implements LdioAdapterConfigurator {
		public static final String MAPPING = "mapping";

		@Override
		public LdiAdapter configure(ComponentProperties config) {
			String rmlMapping = config.getOptionalPropertyFromFile(MAPPING).orElse(config.getProperty(MAPPING));

			return new RmlAdapter(rmlMapping);
		}
	}
}
