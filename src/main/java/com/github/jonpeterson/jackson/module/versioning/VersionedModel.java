package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface VersionedModel<V> {
    String MODEL_VERSION_PROPERTY = "modelVersion";

    @JsonProperty(MODEL_VERSION_PROPERTY)
    V getCurrentModelVersion();
}
