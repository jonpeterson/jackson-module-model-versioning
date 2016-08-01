# Jackson Model Versioning Module
Jackson 2.x module for handling versioning of models.

## The Problem
Let's say we create an API that accepts the following car data JSON:
```json
{
  "model": "honda:civic",
  "year": 2016,
  "new": "true"
}
```

Later, we decide that `model` should be split into two fields (`make` and `model`).
```json
{
  "make": "honda"
  "model": "civic",
  "year": 2016,
  "new": "true"
}
```

Then we decide that `new` should be actually be retyped and renamed (boolean `used`).
```json
{
  "make": "honda"
  "model": "civic",
  "year": 2016,
  "used": false
}
```

By this point, we have three formats of data that clients might be sending to our API. Hopefully we had the foresight to implement versioning on the models or API call's parameters.

There are many ways this could be achieved, such as creating multiple model objects (one per version) and assigning them to each API call's versioned endpoint:
```
POST /api/car/v1/     <-  CarV1
GET  /api/car/v1/     ->  List<CarV1>
GET  /api/car/v1/{id} ->  CarV1

POST /api/car/v2/     <-  CarV2
GET  /api/car/v1/     ->  List<CarV2>
GET  /api/car/v2/{id} ->  CarV2
...
```

Another, less boilerplate-reliant way to do this is to have a single version of the endpoints, but to version the models themselves instead. This is where this module comes in.

## The Solution
By using this Jackson module, we can annotate a single POJO with version-relevant conversion logic to execute on the raw JSON data before deserializing it into the model.
```groovy
@JsonVersionedModel(currentVersion = '3', converterClass = CarVersionedModelConverter)
class Car {
    String make
    String model
    int year
    boolean used
}
```
```groovy
class CarVersionedModelConverter implements VersionedModelConverter {
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
    }
}
```

All that's left is to configure the Jackson ObjectMapper with the module and test it out.
```groovy
def mapper = new ObjectMapper().registerModule(new VersioningModule())
def hondaCivic = mapper.readValue(
    '{"model": "honda:civic", "year": 2016, "new": "true", "modelVersion": "1"}',
    Car
)
println mapper.writeValueAsString(hondaCivic)
// prints '{"make": "honda", "model": "civic", "year": 2016, "used": false, "modelVersion": "3"}'

def toyotaCamry = mapper.readValue(
    '{"make": "toyota", "model": "camry", "year": 2012, "new": "false", "modelVersion": "2"}',
    Car
)
println mapper.writeValueAsString(hondaCivic)
// prints '{"make": "toyota", "model": "camry", "year": 2012, "used": true, "modelVersion": "3"}'

def mazda6 = mapper.readValue(
    '{"make": "mazda", "model": "6", "year": 2017, "used": false, "modelVersion": "3"}',
    Car
)
println mapper.writeValueAsString(mazda6)
// prints '{"make": "mazda", "model": "6", "year": 2017, "used": false, "modelVersion": "3"}'
```

## Compatibility
Compiled for Java 6 and tested with Jackson 2.2 - 2.8.

## Getting Started with Gradle
```groovy
dependencies {
    compile 'com.github.jonpeterson:jackson-module-model-versioning:1.0.0'
}
```

## Getting Started with Maven
```xml
<dependency>
    <groupId>com.github.jonpeterson</groupId>
    <artifactId>jackson-module-model-versioning</artifactId>
    <version>1.0.0</version>
</dependency>
```

## [JavaDoc](https://jonpeterson.github.io/docs/jackson-module-model-versioning/1.0.0/index.html)