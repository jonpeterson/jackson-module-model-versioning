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
package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class VersionedModelDeserializer<T> extends StdDeserializer<T> implements ResolvableDeserializer {
    private final StdDeserializer<T> delegate;
    private final JsonVersionedModel jsonVersionedModel;
    private final VersionedModelConverter converter;
    private final BeanPropertyDefinition serializeToVersionProperty;
    private final JsonSerializeToVersion serializeToVersionAnnotation;

    public VersionedModelDeserializer(StdDeserializer<T> delegate, JsonVersionedModel jsonVersionedModel, BeanPropertyDefinition serializeToVersionProperty) {
        super(delegate.getValueType());

        this.delegate = delegate;
        this.jsonVersionedModel = jsonVersionedModel;
        this.serializeToVersionProperty = serializeToVersionProperty;
        this.serializeToVersionAnnotation = serializeToVersionProperty != null ? serializeToVersionProperty.getAccessor().getAnnotation(JsonSerializeToVersion.class) : null;

        Class<? extends VersionedModelConverter> converterClass = jsonVersionedModel.toCurrentConverterClass();
        if(converterClass != VersionedModelConverter.class)
            try {
                this.converter = converterClass.newInstance();
            } catch(Exception e) {
                throw new RuntimeException("unable to create instance of converter '" + converterClass.getName() + "'", e);
            }
        else
            converter = null;
    }

    @Override
    public void resolve(DeserializationContext context) throws JsonMappingException {
        if(delegate instanceof ResolvableDeserializer)
            ((ResolvableDeserializer)delegate).resolve(context);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode jsonNode = parser.readValueAsTree();

        if (jsonNode.isNull()) {
            jsonNode = (JsonNode) parser.getCodec().createObjectNode();
        }

        if(!(jsonNode instanceof ObjectNode))
            throw context.mappingException("value must be a JSON object");

        ObjectNode modelData = (ObjectNode)jsonNode;

        JsonNode modelVersionNode = modelData.remove(jsonVersionedModel.propertyName());

        String modelVersion = null;
        if(modelVersionNode != null)
            modelVersion = modelVersionNode.asText();

        if(modelVersion == null)
            modelVersion = jsonVersionedModel.defaultDeserializeToVersion();

        if(modelVersion.isEmpty())
            throw context.mappingException("'" + jsonVersionedModel.propertyName() + "' property was null and defaultDeserializeToVersion was not set");

        // convert the model if converter specified and model needs converting
        if(converter != null && (jsonVersionedModel.alwaysConvert() || !modelVersion.equals(jsonVersionedModel.currentVersion())))
            modelData = converter.convert(modelData, modelVersion, jsonVersionedModel.currentVersion(), context.getNodeFactory());

        // set the serializeToVersionProperty value to the source model version if the defaultToSource property is true
        if(serializeToVersionAnnotation != null && serializeToVersionAnnotation.defaultToSource())
            modelData.put(serializeToVersionProperty.getName(), modelVersion);

        JsonParser postInterceptionParser = new TreeTraversingParser(modelData, parser.getCodec());
        postInterceptionParser.nextToken();
        return delegate.deserialize(postInterceptionParser, context);
    }
}