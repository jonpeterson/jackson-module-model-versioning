package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class VersionedModelSerializer<T> extends StdSerializer<T> implements ResolvableSerializer {
    private final StdSerializer<T> delegate;
    private final JsonVersionedModel jsonVersionedModel;

    VersionedModelSerializer(StdSerializer<T> delegate, JsonVersionedModel jsonVersionedModel) {
        super(delegate.handledType());

        this.delegate = delegate;
        this.jsonVersionedModel = jsonVersionedModel;
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        if(delegate instanceof ResolvableSerializer)
            ((ResolvableSerializer)delegate).resolve(provider);
    }

    @Override
    public void serialize(T value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        // serialize the value into a byte array buffer then parse it back out into a JsonNode tree
        // TODO: find a better way to convert the value into a tree
        JsonFactory factory = generator.getCodec().getFactory();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
        JsonGenerator bufferGenerator = factory.createGenerator(buffer);
        try {
            delegate.serialize(value, bufferGenerator, provider);
        } finally {
            bufferGenerator.close();
        }
        ObjectNode node = factory.createParser(buffer.toByteArray()).readValueAsTree();

        // add current version
        node.put(jsonVersionedModel.propertyName(), jsonVersionedModel.currentVersion());

        // write node
        generator.writeTree(node);
    }
}