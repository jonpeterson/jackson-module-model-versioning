package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.HashMap;
import java.util.Map;

public class VersioningModule extends SimpleModule {
    private final Map<Class<? extends VersionedModel>, VersionedModelDataTransformer> transformers = new HashMap<Class<? extends VersionedModel>, VersionedModelDataTransformer>();

    @SuppressWarnings("unchecked")
    VersioningModule() {
        super("VersioningModule");

        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDescription, JsonDeserializer<?> deserializer) {
                if(VersionedModel.class.isAssignableFrom(beanDescription.getBeanClass()))
                    return new VersionedModelDeserializer(
                        (JsonDeserializer<VersionedModel>)deserializer,
                        transformers.get(beanDescription.getBeanClass())
                    );

                return deserializer;
            }
        });
    }

    public <V> VersioningModule withTransformer(Class<? extends VersionedModel<V>> modelClass, VersionedModelDataTransformer<V> transformer) {
        transformers.put(modelClass, transformer);
        return this;
    }
}
