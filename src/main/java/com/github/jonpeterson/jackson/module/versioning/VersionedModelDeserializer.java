package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

class VersionedModelDeserializer<T> extends StdDeserializer<T> implements ResolvableDeserializer {
    private final StdDeserializer<T> delegate;
    private final JsonVersionedModel jsonVersionedModel;
    private final VersionedModelConverter converter;

    VersionedModelDeserializer(StdDeserializer<T> delegate, JsonVersionedModel jsonVersionedModel) {
        super(delegate.getValueType());

        this.delegate = delegate;
        this.jsonVersionedModel = jsonVersionedModel;

        try {
            this.converter = jsonVersionedModel.converterClass().newInstance();
        } catch(Exception e) {
            throw new RuntimeException("unable to create instance of converter '" + jsonVersionedModel.converterClass().getName() + "'", e);
        }
    }

    @Override
    public void resolve(DeserializationContext context) throws JsonMappingException {
        if(delegate instanceof ResolvableDeserializer)
            ((ResolvableDeserializer)delegate).resolve(context);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode jsonNode = parser.readValueAsTree();

        if(!(jsonNode instanceof ObjectNode))
            throw context.mappingException("value must be a JSON object");

        ObjectNode modelData = (ObjectNode)jsonNode;

        JsonNode modelVersionNode = modelData.remove(jsonVersionedModel.propertyName());
        if(modelVersionNode == null)
            throw context.mappingException("'" + jsonVersionedModel.propertyName() + "' property was not present");

        String modelVersion = modelVersionNode.asText();
        if(modelVersion == null)
            throw context.mappingException("'" + jsonVersionedModel.propertyName() + "' property was null");

        if(jsonVersionedModel.alwaysConvert() || !modelVersion.equals(jsonVersionedModel.currentVersion()))
            converter.convert(modelVersion, modelData);

        JsonParser postInterceptionParser = new TreeTraversingParser(jsonNode, parser.getCodec());
        postInterceptionParser.nextToken();
        return delegate.deserialize(postInterceptionParser, context);
    }
}