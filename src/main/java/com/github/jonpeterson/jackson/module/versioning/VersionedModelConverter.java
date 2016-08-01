package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Converter used by {@link JsonVersionedModel} for resolving model versioning.
 */
public interface VersionedModelConverter {

    /**
     * Updates JSON data before deserialization to model.
     *
     * @param modelVersion version of data
     * @param modelData    JSON data to update
     */
    void convert(String modelVersion, ObjectNode modelData);
}
