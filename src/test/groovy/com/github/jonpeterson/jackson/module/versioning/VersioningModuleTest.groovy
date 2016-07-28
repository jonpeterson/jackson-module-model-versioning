package com.github.jonpeterson.jackson.module.versioning

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import spock.lang.Specification

class VersioningModuleTest extends Specification {

    static class SomeModel implements VersionedModel<String> {
        static final CURRENT_MODEL_VERSION = '1.2'

        String fieldA
        int fieldB

        @Override
        String getCurrentModelVersion() {
            return CURRENT_MODEL_VERSION
        }
    }

    def 'abc'() {
        given:
        def mapper = new ObjectMapper()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .registerModule(new VersioningModule()
                .withTransformer(SomeModel, new VersionedModelDataTransformer<String>(SomeModel.CURRENT_MODEL_VERSION) {
                    @Override
                    def void transform(String modelVersion, ObjectNode modelData) {
                        switch(modelVersion) {
                            case '1.0':
                                modelData.put('fieldB', modelData.get('fieldB').asInt() / 2)
                                break

                            case '1.1':
                                modelData.put('fieldA', modelData.remove('fieldZ').asText().reverse())
                                break
                        }
                    }
                })
            )

        expect:
        mapper.writeValueAsString(mapper.readValue(inputJson, SomeModel)) == expectedOutputJson

        where:
        inputJson | expectedOutputJson
        '{"fieldA":"abc","fieldB":246,"modelVersion":"1.0"}' | '{"fieldA":"abc","fieldB":123,"modelVersion":"1.2"}'
        '{"fieldZ":"cba","fieldB":123,"modelVersion":"1.1"}' | '{"fieldA":"abc","fieldB":123,"modelVersion":"1.2"}'
        '{"fieldA":"abc","fieldB":123,"modelVersion":"1.2"}' | '{"fieldA":"abc","fieldB":123,"modelVersion":"1.2"}'
        '{"fieldA":"abc","fieldB":123,"modelVersion":1.3}'   | '{"fieldA":"abc","fieldB":123,"modelVersion":"1.2"}'
    }
}
