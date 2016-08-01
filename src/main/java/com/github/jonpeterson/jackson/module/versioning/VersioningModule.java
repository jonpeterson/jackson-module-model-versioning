package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson module to load when using {@link JsonVersionedModel}.
 */
public class VersioningModule extends SimpleModule {

    public VersioningModule() {
        super("VersioningModule");

        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDescription, JsonDeserializer<?> deserializer) {
                if(deserializer instanceof StdDeserializer) {
                    JsonVersionedModel jsonVersionedModel = beanDescription.getClassAnnotations().get(JsonVersionedModel.class);
                    if(jsonVersionedModel != null)
                        return createVersioningDeserializer((StdDeserializer)deserializer, jsonVersionedModel);
                }

                return deserializer;
            }

            // here just to make generics work without warnings
            private <T> VersionedModelDeserializer<T> createVersioningDeserializer(StdDeserializer<T> deserializer, JsonVersionedModel jsonVersionedModel) {
                return new VersionedModelDeserializer<T>(deserializer, jsonVersionedModel);
            }
        });

        setSerializerModifier(new BeanSerializerModifier() {

            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDescription, JsonSerializer<?> serializer) {
                if(serializer instanceof StdSerializer) {
                    JsonVersionedModel jsonVersionedModel = beanDescription.getClassAnnotations().get(JsonVersionedModel.class);
                    if(jsonVersionedModel != null)
                        return createVersioningSerializer((StdSerializer)serializer, jsonVersionedModel);
                }

                return serializer;
            }

            // here just to make generics work without warnings
            private <T> VersionedModelSerializer<T> createVersioningSerializer(StdSerializer<T> serializer, JsonVersionedModel jsonVersionedModel) {
                return new VersionedModelSerializer<T>(serializer, jsonVersionedModel);
            }
        });
    }
}
