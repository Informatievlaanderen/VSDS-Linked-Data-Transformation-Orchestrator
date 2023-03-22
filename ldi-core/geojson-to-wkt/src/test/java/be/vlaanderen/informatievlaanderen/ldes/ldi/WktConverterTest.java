package be.vlaanderen.informatievlaanderen.ldes.ldi;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WktConverterTest {

    private final WktConverter wktConverter = new WktConverter();

    // TODO: 22/03/2023 point test, multi and single json files

    @ParameterizedTest
    @ArgumentsSource(GeoJsonProvider.class)
    void test_getWktFromModel(String source, String expectedResult) {
        final String result = wktConverter.getWktFromModel(
                RDFParser.source(source).lang(Lang.JSONLD).build().toModel()
        );

        assertEquals(expectedResult, result);
    }

    static class GeoJsonProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
//                    Arguments.of("geojson-point.json", "POINT (100 0)"),
//                    Arguments.of("geojson-linestring.json", "LINESTRING (100 0, 101 1, 102 2)"),
//                    Arguments.of("geojson-polygon.json", "POLYGON ((100 0, 101 0, 101 1, 100 1, 100 0), " +
//                            "(100.8 0.8, 100.8 0.2, 100.2 0.2, 100.2 0.8, 100.8 0.8), " +
//                            "(100.95 0.9, 100.95 0.5, 100.9 0.2, 100.9 0.5, 100.95 0.9))"),
//                    Arguments.of("geojson-multipoint.json", "MULTIPOINT ((100 0), (101 1), (102 2))"),
//                    Arguments.of("geojson-multilinestring.json", "MULTILINESTRING ((100 0, 101 1), (102 2, 103 3, 104 4))"),
//                    Arguments.of("geojson-multipolygon.json", "MULTIPOLYGON (((102 2, 103 2, 103 3, 102 3, 102 2)), ((100 0, 101 0, 101 1, 100 1, 100 0), (100.2 0.2, 100.2 0.8, 100.8 0.8, 100.8 0.2, 100.2 0.2)))"),
                    Arguments.of("geojson-geometrycollection.json", "GEOMETRYCOLLECTION (POINT (100 0), LINESTRING (101 0, 102 1))")
            );
        }
    }

}