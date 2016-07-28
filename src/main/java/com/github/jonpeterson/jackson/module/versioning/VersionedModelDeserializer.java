package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public class VersionedModelDeserializer extends StdDeserializer<VersionedModel> implements ResolvableDeserializer {
    private final JsonDeserializer<VersionedModel> delegateDeserializer;
    private final VersionedModelDataTransformer transformer;

    protected VersionedModelDeserializer(JsonDeserializer<VersionedModel> delegateDeserializer, VersionedModelDataTransformer transformer) {
        super(VersionedModel.class);

        this.delegateDeserializer = delegateDeserializer;
        this.transformer = transformer;
    }

    @Override
    public void resolve(DeserializationContext context) throws JsonMappingException {
        ((ResolvableDeserializer)delegateDeserializer).resolve(context);
    }

    @Override
    public VersionedModel deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        TreeNode treeNode = parser.readValueAsTree();

        if(!(treeNode instanceof ObjectNode))
            throw context.mappingException("value must be a JSON object");

        ObjectNode objectNode = (ObjectNode)treeNode;

        JsonNode modelVersionNode = objectNode.remove(VersionedModel.MODEL_VERSION_PROPERTY);
        if(modelVersionNode == null)
            throw context.mappingException("'" + VersionedModel.MODEL_VERSION_PROPERTY + "' property was not present");

        // DEBUG HERE
        //Class<?> abc = delegateDeserializer.handledType();
        //((ParameterizedType)abc.getGenericInterfaces()[0]).getActualTypeArguments()[0]

        String modelVersion = modelVersionNode.asText();
        if(modelVersion == null)
            throw context.mappingException("'" + VersionedModel.MODEL_VERSION_PROPERTY + "' property was null");

        if(transformer != null && !modelVersion.equals(transformer.targetVersion))
            transformer.transform(modelVersion, objectNode);

        JsonParser objectNodeParser = new TreeTraversingParser(objectNode);
        objectNodeParser.nextToken();
        return delegateDeserializer.deserialize(objectNodeParser, context);
    }
}
