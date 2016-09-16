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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

class VersioningBeanSerializationModifier extends BeanSerializerModifier {

    // here just to make generics work without warnings
    private static <T> VersionedModelSerializer<T> createVersioningSerializer(StdSerializer<T> serializer, JsonVersionedModel jsonVersionedModel, BeanPropertyDefinition serializeToVersionProperty) {
        return new VersionedModelSerializer<T>(serializer, jsonVersionedModel, serializeToVersionProperty);
    }

    static BeanPropertyDefinition getSerializeToVersionProperty(BeanDescription beanDescription) throws RuntimeException {
        BeanPropertyDefinition serializeToVersionProperty = null;
        for(BeanPropertyDefinition definition: beanDescription.findProperties()) {
            AnnotatedMember accessor = definition.getAccessor();
            if(accessor.hasAnnotation(JsonSerializeToVersion.class)) {
                if(serializeToVersionProperty != null)
                    throw new RuntimeException("@" + JsonSerializeToVersion.class.getSimpleName() + " must be present on at most one field or method");
                if(accessor.getRawType() != String.class || (definition.getField() == null && !definition.hasGetter()))
                    throw new RuntimeException("@" + JsonSerializeToVersion.class.getSimpleName() + " must be on a field or a getter method that returns a String");
                serializeToVersionProperty = definition;
            }
        }

        return serializeToVersionProperty;
    }


    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDescription, JsonSerializer<?> serializer) {
        if(serializer instanceof StdSerializer) {
            JsonVersionedModel jsonVersionedModel = beanDescription.getClassAnnotations().get(JsonVersionedModel.class);
            if(jsonVersionedModel != null)
                return createVersioningSerializer((StdSerializer)serializer, jsonVersionedModel, getSerializeToVersionProperty(beanDescription));
        }

        return serializer;
    }
}
