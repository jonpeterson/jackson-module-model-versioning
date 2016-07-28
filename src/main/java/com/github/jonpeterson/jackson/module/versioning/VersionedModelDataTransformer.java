package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class VersionedModelDataTransformer<V> {
    public final V targetVersion;

    public VersionedModelDataTransformer(V targetVersion) {
        this.targetVersion = targetVersion;
    }

    abstract void transform(V modelVersion, ObjectNode modelData);
}
