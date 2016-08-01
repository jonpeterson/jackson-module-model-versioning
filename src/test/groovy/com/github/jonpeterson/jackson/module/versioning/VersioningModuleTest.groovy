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

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import spock.lang.Specification

class VersioningModuleTest extends Specification {

    static class CarsByType {
        String type
        List<Car> cars
        List<CustomVersionedCar> customVersionedCars
    }

    @JsonVersionedModel(currentVersion = '3', converterClass = CarVersionedModelConverter)
    static class Car {
        String make
        String model
        int year
        boolean used
        String originalModelVersion
    }

    @JsonVersionedModel(currentVersion = '3', converterClass = CarVersionedModelConverter, alwaysConvert = true, propertyName = '_version')
    static class CustomVersionedCar extends Car {
    }

    static class CarVersionedModelConverter implements VersionedModelConverter {

        @Override
        def void convert(String modelVersion, ObjectNode modelData) {
            // model version is an int
            def modelVersionNum = modelVersion as int

            // version 1 had a single 'model' field that combined 'make' and 'model' with a colon delimiter; split
            if(modelVersionNum < 2) {
                def makeAndModel = modelData.get('model').asText().split(':')
                modelData.put('make', makeAndModel[0])
                modelData.put('model', makeAndModel[1])
            }

            // version 1-2 had a 'new' text field instead of a boolean 'used' field; convert and invert
            if(modelVersionNum < 3)
                modelData.put('used', !(modelData.remove('new').asText() as boolean))

            // setting a debug field
            modelData.put('originalModelVersion', modelVersion)
        }
    }


    def 'deserialize and reserialize'() {
        given:
        def mapper = new ObjectMapper()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new VersioningModule())

        expect:
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

        mapper.writeValueAsString(deserialized).replaceAll('\r\n', '\n') ==
            '''{
              |  "cars" : [ {
              |    "make" : "honda",
              |    "model" : "civic",
              |    "originalModelVersion" : "1",
              |    "used" : false,
              |    "year" : 2016,
              |    "modelVersion" : "3"
              |  }, {
              |    "make" : "toyota",
              |    "model" : "camry",
              |    "originalModelVersion" : "2",
              |    "used" : false,
              |    "year" : 2012,
              |    "modelVersion" : "3"
              |  }, {
              |    "make" : "mazda",
              |    "model" : "6",
              |    "originalModelVersion" : null,
              |    "used" : false,
              |    "year" : 2017,
              |    "modelVersion" : "3"
              |  }, {
              |    "make" : "ford",
              |    "model" : "fusion",
              |    "originalModelVersion" : "4",
              |    "used" : true,
              |    "year" : 2013,
              |    "modelVersion" : "3"
              |  } ],
              |  "customVersionedCars" : [ {
              |    "make" : "honda",
              |    "model" : "civic",
              |    "originalModelVersion" : "1",
              |    "used" : false,
              |    "year" : 2016,
              |    "_version" : "3"
              |  }, {
              |    "make" : "toyota",
              |    "model" : "camry",
              |    "originalModelVersion" : "2",
              |    "used" : false,
              |    "year" : 2012,
              |    "_version" : "3"
              |  }, {
              |    "make" : "mazda",
              |    "model" : "6",
              |    "originalModelVersion" : "3",
              |    "used" : false,
              |    "year" : 2017,
              |    "_version" : "3"
              |  }, {
              |    "make" : "ford",
              |    "model" : "fusion",
              |    "originalModelVersion" : "4",
              |    "used" : true,
              |    "year" : 2013,
              |    "_version" : "3"
              |  } ],
              |  "type" : "sedan"
              |}'''.stripMargin()
    }
}