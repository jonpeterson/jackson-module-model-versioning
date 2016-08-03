/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jon Peterson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.jonpeterson.jackson.module.versioning

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import spock.lang.Specification

class VersioningModuleTest extends Specification {

    static class CarsByType {
        String type
        List<Car> cars
        List<DefaultSerializeToCar> customVersionedCars
    }

    @JsonVersionedModel(currentVersion = '3',
                        toCurrentConverterClass = ToCurrentCarConverter)
    static class Car {
        String make
        String model
        int year
        boolean used
        String _debugPreDeserializationVersion
    }

    @JsonVersionedModel(currentVersion = '3',
                        defaultSerializeToVersion = '2',
                        toCurrentConverterClass = ToCurrentCarConverter,
                        toPastConverterClass = ToPastCarConverter,
                        alwaysConvert = true,
                        propertyName = '_version')
    static class DefaultSerializeToCar extends Car {
    }

    static class MethodSerializeToCar extends DefaultSerializeToCar {

        @JsonSerializeToVersion
        String getSerializeToVersion() {
            return "1"
        }
    }

    static class ToCurrentCarConverter implements VersionedModelConverter {

        @Override
        def ObjectNode convert(ObjectNode modelData, String modelVersion, String targetModelVersion, JsonNodeFactory nodeFactory) {
            // model version is an int
            def version = modelVersion as int

            // version 1 had a single 'model' field that combined 'make' and 'model' with a colon delimiter; split
            if(version <= 1) {
                def makeAndModel = modelData.get('model').asText().split(':')
                modelData.put('make', makeAndModel[0])
                modelData.put('model', makeAndModel[1])
            }

            // version 1-2 had a 'new' text field instead of a boolean 'used' field; convert and invert
            if(version <= 2)
                modelData.put('used', !Boolean.parseBoolean(modelData.remove('new').asText()))

            // setting a debug field
            modelData.put('_debugPreDeserializationVersion', modelVersion)
        }
    }

    static class ToPastCarConverter implements VersionedModelConverter {

        @Override
        def ObjectNode convert(ObjectNode modelData, String modelVersion, String targetModelVersion, JsonNodeFactory nodeFactory) {
            // model version is an int
            def version = modelVersion as int
            def targetVersion = targetModelVersion as int

            // version 1 had a single 'model' field that combined 'make' and 'model' with a colon delimiter; combine
            if(targetVersion <= 1 && version > 1)
                modelData.put('model', "${modelData.remove('make').asText()}:${modelData.get('model').asText()}")

            // version 1-2 had a 'new' text field instead of a boolean 'used' field; convert and invert
            if(targetVersion <= 2 && version > 2)
                modelData.put('new', !modelData.remove('used').asBoolean() as String)

            // setting a debug field
            modelData.put('_debugPreSerializationVersion', modelVersion)
        }
    }


    def mapper = new ObjectMapper().registerModule(new VersioningModule())

    def 'deserialize and reserialize'() {
        when:
        def deserialized = mapper.readValue(
            '''{
              |  "type": "sedan",
              |  "cars": [
              |    {
              |      "model": "honda:civic",
              |      "year": 2016,
              |      "new": "true",
              |      "modelVersion": "1"
              |    }, {
              |      "make": "toyota",
              |      "model": "camry",
              |      "year": 2012,
              |      "new": "false",
              |      "modelVersion": "2"
              |    }, {
              |      "make": "mazda",
              |      "model": "6",
              |      "year": 2017,
              |      "used": false,
              |      "modelVersion": "3"
              |    }, {
              |      "make": "ford",
              |      "model": "fusion",
              |      "year": 2013,
              |      "used": true,
              |      "modelVersion": "4"
              |    }
              |  ],
              |  "customVersionedCars": [
              |    {
              |      "model": "honda:civic",
              |      "year": 2016,
              |      "new": "true",
              |      "_version": "1"
              |    }, {
              |      "make": "toyota",
              |      "model": "camry",
              |      "year": 2012,
              |      "new": "false",
              |      "_version": "2"
              |    }, {
              |      "make": "mazda",
              |      "model": "6",
              |      "year": 2017,
              |      "used": false,
              |      "_version": "3"
              |    }, {
              |      "make": "ford",
              |      "model": "fusion",
              |      "year": 2013,
              |      "used": true,
              |      "_version": "4"
              |    }
              |  ]
              |}'''.stripMargin(),
            CarsByType
        )

        then:
        mapper.convertValue(deserialized, Map) == [
            type: 'sedan',
            cars: [
                [
                    modelVersion: '3',
                    make: 'honda',
                    model: 'civic',
                    used: false,
                    year: 2016,
                    _debugPreDeserializationVersion: '1'
                ], [
                    modelVersion: '3',
                    make: 'toyota',
                    model: 'camry',
                    used: true,
                    year: 2012,
                    _debugPreDeserializationVersion: '2'
                ], [
                    modelVersion: '3',
                    make: 'mazda',
                    model: '6',
                    used: false,
                    year: 2017,
                    _debugPreDeserializationVersion: null
                ], [
                    modelVersion: '3',
                    make: 'ford',
                    model: 'fusion',
                    used: true,
                    year: 2013,
                    _debugPreDeserializationVersion: '4'
                ]
            ],
            customVersionedCars: [
                [
                    _version: '2',
                    make: 'honda',
                    model: 'civic',
                    new: 'true',
                    year: 2016,
                    _debugPreDeserializationVersion: '1',
                    _debugPreSerializationVersion: '3'
                ], [
                    _version: '2',
                    make: 'toyota',
                    model: 'camry',
                    new: 'false',
                    year: 2012,
                    _debugPreDeserializationVersion: '2',
                    _debugPreSerializationVersion: '3'
                ], [
                    _version: '2',
                    make: 'mazda',
                    model: '6',
                    new: 'true',
                    year: 2017,
                    _debugPreDeserializationVersion: '3',
                    _debugPreSerializationVersion: '3'
                ], [
                    _version: '2',
                    make: 'ford',
                    model: 'fusion',
                    new: 'false',
                    year: 2013,
                    _debugPreDeserializationVersion: '4',
                    _debugPreSerializationVersion: '3'
                ]
            ]
        ]
    }

    def 'abc'() {
        expect:
        def car = new MethodSerializeToCar(
            make: 'honda',
            model: 'civic',
            used: false,
            year: 2016
        )
        mapper.readValue(mapper.writeValueAsString(car), Map) == [
            _version: '1',
            model: 'honda:civic',
            new: 'true',
            year: 2016,
            _debugPreDeserializationVersion: null,
            _debugPreSerializationVersion: '3'
        ]
    }
}