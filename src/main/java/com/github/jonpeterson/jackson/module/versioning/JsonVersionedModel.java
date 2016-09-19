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

import com.fasterxml.jackson.annotation.JacksonAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies model versioning details.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonVersionedModel {

    /**
     * @return the current version of the model.
     */
    String currentVersion();

    /**
     * @return the version to convert the model to during serialization; this can be overridden by using
     *         {@link JsonSerializeToVersion}
     */
    String defaultSerializeToVersion() default "";

    /**
     * @return class of the converter to use when resolving versioning to the current version; not specifying will cause
     *         models to not be converted at all
     */
    Class<? extends VersionedModelConverter> toCurrentConverterClass() default VersionedModelConverter.class;

    /**
     * @return class of the converter to use when resolving versioning to a past version; not specifying will cause
     *         models to be serialized as the current version
     */
    Class<? extends VersionedModelConverter> toPastConverterClass() default VersionedModelConverter.class;

    /**
     * @return whether to always send model data to converters, even when the data is the same version as the version to
     *         convert to
     */
    boolean alwaysConvert() default false;

    /**
     * @return name of property in which the model's version is stored in JSON
     */
    String propertyName() default "modelVersion";

    /**
     * @return the default version to use if the propertyName() attribute is not available.
     */
    String defaultDeserializeToVersion() default "";
}
