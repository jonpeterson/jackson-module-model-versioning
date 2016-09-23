## 1.2.0 (2016-09-22)

- brentryan:   Made @JsonVersionedModel.toCurrentConverter optional for when used on version 1 of a model.
- brentryan:   Added ability to have serializeToVersion match the source model version when deserializing.
- jonpeterson: Added tests for @brentryan's changes, fixed a couple bugs, and polished.
- jonpeterson: Added @JsonVersionedModel.defaultDeserializeToVersion

## 1.1.1 (2016-08-10)

- jonpeterson: Fixed bug that caused error while serializing models using @JsonSubTypes.
- jonpeterson: Fixed detection of multiple @JsonSerializeToVersion properties; getters and setters now allowed.

## 1.1.0 (2016-08-04)

- jonpeterson: Added @JsonSerializeToVersion annotation.

## 1.0.0 (2016-08-01)

- jonpeterson: Initial release.